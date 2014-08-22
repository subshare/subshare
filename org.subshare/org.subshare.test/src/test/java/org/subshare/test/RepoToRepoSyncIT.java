package org.subshare.test;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.UUID;

import org.subshare.core.AccessDeniedException;
import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.client.CloudStoreClient;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.util.UrlUtil;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class RepoToRepoSyncIT extends AbstractIT {

	private static final Logger logger = LoggerFactory.getLogger(RepoToRepoSyncIT.class);

	private File localSrcRoot;
	private File localDestRoot;
	private File remoteRoot;

	private String localPathPrefix;
	private UUID remoteRepositoryId;
	private String remotePathPrefix1;
	private String remotePathPrefix2Encrypted;
	private String remotePathPrefix2Plain;
	private URL remoteRootURLWithPathPrefixForLocalSrc;
	private URL remoteRootURLWithPathPrefixForLocalDest;

	@Before
	public void before() {
		localPathPrefix = "";
		remotePathPrefix1 = "";
		remotePathPrefix2Plain = "";
		remotePathPrefix2Encrypted = "";
	}

	private File getLocalRootWithPathPrefix() {
		if (localPathPrefix.isEmpty())
			return localSrcRoot;

		return new File(localSrcRoot, localPathPrefix);
	}

	private File getRemoteRootWithPathPrefix1() {
		if (remotePathPrefix1.isEmpty())
			return remoteRoot;

		final File file = new File(remoteRoot, remotePathPrefix1);
		return file;
	}

	private File getRemoteRootWithPathPrefix2() {
		assertNotNull("remotePathPrefix2Encrypted", remotePathPrefix2Encrypted);
		if (remotePathPrefix2Encrypted.isEmpty())
			return remoteRoot;

		final File file = new File(remoteRoot, remotePathPrefix2Encrypted);
		return file;
	}

	private URL getRemoteRootURLWithPathPrefixForLocalSrc(final UUID remoteRepositoryId) throws MalformedURLException {
		final URL remoteRootURL = UrlUtil.appendNonEncodedPath(new URL(getSecureUrl() + "/" + remoteRepositoryId),  remotePathPrefix1);
		return remoteRootURL;
	}

	private URL getRemoteRootURLWithPathPrefixForLocalDest(final UUID remoteRepositoryId) throws MalformedURLException {
		final URL remoteRootURL = UrlUtil.appendNonEncodedPath(new URL(getSecureUrl() + "/" + remoteRepositoryId),  remotePathPrefix2Encrypted);
		return remoteRootURL;
	}

	@Test
	public void syncFromLocalToRemoteToLocal() throws Exception {
		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();
		createLocalDestinationRepo();
		syncFromRemoteToLocalDest();
	}

	protected UserRepoKeyRing createUserRepoKeyRing() {
		assertNotNull("remoteRepositoryId", remoteRepositoryId);
		return createUserRepoKeyRing(remoteRepositoryId);
	}

	@Test
	public void syncFromLocalToRemoteToLocalWithPathPrefix() throws Exception {
		remotePathPrefix2Plain = "/3 + &#ä";
		syncFromLocalToRemoteToLocal();
	}

	@Test
	public void multiSyncFromLocalToRemoteToLocalWithPathPrefixWithSubdirClearanceKey() throws Exception {
		remotePathPrefix2Plain = "/3 + &#ä";

		createLocalSourceAndRemoteRepo();

		// Opening localRepoManagerLocal to make sure it's not discarded while test is running.
		// This caused occasional build errors - especially on slower machines - when creating files
		// 'yyyyyyyy' etc. below.
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);)
		{
			populateLocalSourceRepo();
			syncFromLocalSrcToRemote();
			determineRemotePathPrefix2Encrypted();

			final UserRepoKeyRing otherUserRepoKeyRing = createUserRepoKeyRing();
			grantRemotePathPrefix2EncryptedReadAccessToOtherUser(
					otherUserRepoKeyRing.getRandomUserRepoKey().getPublicKey());

			createFileWithRandomContent(localSrcRoot, "xxxxxxx");

			final File child_2 = new File(localSrcRoot, "2");
			new File(child_2, "a").delete();
			createFileWithRandomContent(child_2, "a"); // overwrite
			createFileWithRandomContent(child_2, "yyyyyyyy"); // new file

			final File child_3 = new File(localSrcRoot, remotePathPrefix2Plain);
			new File(child_3, "b").delete();
			createFileWithRandomContent(child_3, "b"); // overwrite
			createFileWithRandomContent(child_3, "zzzzzzz"); // new file

			syncFromLocalSrcToRemote();

			final UserRepoKeyRing ownerUserRepoKeyRing = cryptreeRepoTransportFactory.getUserRepoKeyRing();
			assertThat(ownerUserRepoKeyRing).isNotNull();
			try {
				cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing);
				createLocalDestinationRepo();
				syncFromRemoteToLocalDest();
			} finally {
				cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
			}

			new File(child_2, "yyyyyyyy").delete();
			createFileWithRandomContent(child_2, "yyyyyyyy"); // overwrite
			createFileWithRandomContent(child_2, "ttttt"); // new file

			new File(child_3, "c").delete();
			createFileWithRandomContent(child_3, "c"); // overwrite
			createFileWithRandomContent(child_3, "kkkkk"); // new file

			syncFromLocalSrcToRemote();

			try {
				cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing);
				syncFromRemoteToLocalDest();
			} finally {
				cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
			}
		}
	}

	@Test
	public void syncFromLocalToRemoteToLocalWithPathPrefixWithSubdirClearanceKey() throws Exception {
		remotePathPrefix2Plain = "/3 + &#ä";

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();

		final UserRepoKeyRing otherUserRepoKeyRing1 = createUserRepoKeyRing();
		final UserRepoKeyRing otherUserRepoKeyRing2 = createUserRepoKeyRing();
		final PublicKey publicKey1 = otherUserRepoKeyRing1.getRandomUserRepoKey().getPublicKey();
		grantRemotePathPrefix2EncryptedReadAccessToOtherUser(
				publicKey1);
		grantRemotePathPrefix2EncryptedReadAccessToOtherUser(
				otherUserRepoKeyRing2.getRandomUserRepoKey().getPublicKey());

		syncFromLocalSrcToRemote();

		final UserRepoKeyRing ownerUserRepoKeyRing = cryptreeRepoTransportFactory.getUserRepoKeyRing();
		assertThat(ownerUserRepoKeyRing).isNotNull();
		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);
			createLocalDestinationRepo();
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}

		revokeRemotePathPrefix2EncryptedReadAccessToOtherUser(publicKey1);
		syncFromLocalSrcToRemote();

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);
			createLocalDestinationRepo();

			// The following sync should still work, because the original files/dirs were not yet modified
			// and revoking the access rights therefore does not yet affect anything. This is called
			// lazy revocation.
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}

		// Now, we modify the directory locally and sync it up again. This should cause the next down-sync
		// with the revoked key to fail.
		getLocalRootWithPathPrefix().setLastModified(getLocalRootWithPathPrefix().lastModified() + 3000);
		syncFromLocalSrcToRemote();

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);
			createLocalDestinationRepo();
			try {
				syncFromRemoteToLocalDest();
				fail("Could still check-out after access rights were revoked!");
			} catch (final AccessDeniedException x) {
				logger.info("Fine! Expected this AccessDeniedException: " + x);
			}

// TODO there's still sth. foul: the following fails :-(
//			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing2);
//			createLocalDestinationRepo();
//			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}
	}

	@Test
	public void syncFromLocalToRemoteToLocalWithPathPrefixWithoutSubdirClearanceKey() throws Exception {
		remotePathPrefix2Plain = "/3 + &#ä";

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();

		final UserRepoKeyRing otherUserRepoKeyRing = createUserRepoKeyRing();
		final UserRepoKeyRing ownerUserRepoKeyRing = cryptreeRepoTransportFactory.getUserRepoKeyRing();
		// Do *not* grant read access to the sub-dir! It must fail.
		assertThat(ownerUserRepoKeyRing).isNotNull();
		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing);
			createLocalDestinationRepo();

			try {
				syncFromRemoteToLocalDest();
				fail("AccessDeniedException was *not* thrown! It should have been!");
			} catch (final AccessDeniedException x) {
				logger.info("syncFromLocalToRemoteToLocalWithPathPrefixWithoutSubdirClearanceKey: Caught AccessDeniedException as expected.");
			}
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}
	}

	private void createLocalSourceAndRemoteRepo() throws Exception {
		localSrcRoot = newTestRepositoryLocalRoot("local-src");
		assertThat(localSrcRoot).doesNotExist();
		localSrcRoot.mkdirs();
		assertThat(localSrcRoot).isDirectory();

		remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot).doesNotExist();
		remoteRoot.mkdirs();
		assertThat(remoteRoot).isDirectory();

		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localSrcRoot);
		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);

		remoteRepositoryId = localRepoManagerRemote.getRepositoryId();
		remoteRootURLWithPathPrefixForLocalSrc = getRemoteRootURLWithPathPrefixForLocalSrc(remoteRepositoryId);
		localRepoManagerRemote.close();

		new CloudStoreClient("requestRepoConnection", getLocalRootWithPathPrefix().getPath(), remoteRootURLWithPathPrefixForLocalSrc.toExternalForm()).execute();
		new CloudStoreClient("acceptRepoConnection", getRemoteRootWithPathPrefix1().getPath()).execute();

		localRepoManagerLocal.close();

		cryptreeRepoTransportFactory.setUserRepoKeyRing(createUserRepoKeyRing());
	}

	private void populateLocalSourceRepo() throws Exception {
		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);

		final File child_1 = createDirectory(localSrcRoot, "1 {11 11ä11} 1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		final File child_2 = createDirectory(localSrcRoot, "2");

		createFileWithRandomContent(child_2, "a");

		final File child_2_1 = createDirectory(child_2, "1 {11 11ä11} 1");
		createFileWithRandomContent(child_2_1, "a");
		createFileWithRandomContent(child_2_1, "b");

		final File child_3 = createDirectory(localSrcRoot, "3 + &#ä");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c");
		createFileWithRandomContent(child_3, "d");

		final File child_3_5 = createDirectory(child_3, "5");
		createFileWithRandomContent(child_3_5, "h");
		createFileWithRandomContent(child_3_5, "i");

		localRepoManagerLocal.localSync(new LoggerProgressMonitor(logger));
		localRepoManagerLocal.close();
	}

	private void syncFromLocalSrcToRemote() throws Exception {
		try (final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefixForLocalSrc);)
		{
			repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		}
	}

	private void syncFromRemoteToLocalDest() throws Exception {
		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(localDestRoot, remoteRootURLWithPathPrefixForLocalDest);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

		assertDirectoriesAreEqualRecursively(
				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : new File(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
				localDestRoot);
	}

	private void createLocalDestinationRepo() throws Exception {
		localDestRoot = newTestRepositoryLocalRoot("local-dest");
		assertThat(localDestRoot).doesNotExist();
		localDestRoot.mkdirs();
		assertThat(localDestRoot).isDirectory();

		final LocalRepoManager localDestRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localDestRoot);
		assertThat(localDestRepoManagerLocal).isNotNull();
		localDestRepoManagerLocal.close();

		remoteRootURLWithPathPrefixForLocalDest = getRemoteRootURLWithPathPrefixForLocalDest(remoteRepositoryId);
		new CloudStoreClient("requestRepoConnection", localDestRoot.getPath(), remoteRootURLWithPathPrefixForLocalDest.toExternalForm()).execute();
		new CloudStoreClient("acceptRepoConnection", getRemoteRootWithPathPrefix2().getPath()).execute();
	}

	private void determineRemotePathPrefix2Encrypted() {
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginReadTransaction();)
			{
				final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
				final RepoFile repoFile = repoFileDao.getRepoFile(getLocalRootWithPathPrefix(), new File(getLocalRootWithPathPrefix(), remotePathPrefix2Plain));
				final CryptoRepoFile cryptoRepoFile = transaction.getDao(CryptoRepoFileDao.class).getCryptoRepoFileOrFail(repoFile);
				remotePathPrefix2Encrypted = cryptoRepoFile.getServerPath();
				transaction.commit();
			}
		}
	}

	private void grantRemotePathPrefix2EncryptedReadAccessToOtherUser(final UserRepoKey.PublicKey userRepoKeyPublicKey) {
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginWriteTransaction();)
			{
				final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().createCryptree(
						transaction, remoteRepositoryId,
						remotePathPrefix2Encrypted,
						cryptreeRepoTransportFactory.getUserRepoKeyRing().getRandomUserRepoKey());
				cryptree.grantReadAccess(remotePathPrefix2Plain, userRepoKeyPublicKey);
				cryptree.close();
				transaction.commit();
			}
		}
	}

	private void revokeRemotePathPrefix2EncryptedReadAccessToOtherUser(final UserRepoKey.PublicKey userRepoKeyPublicKey) {
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginWriteTransaction();)
			{
				final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().createCryptree(
						transaction, remoteRepositoryId,
						remotePathPrefix2Encrypted,
						cryptreeRepoTransportFactory.getUserRepoKeyRing().getRandomUserRepoKey());
				cryptree.revokeReadAccess(remotePathPrefix2Plain, Collections.singleton(userRepoKeyPublicKey.getUserRepoKeyId()));
				cryptree.close();
				transaction.commit();
			}
		}
	}
}
