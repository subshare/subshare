package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.UUID;

import org.subshare.core.AccessDeniedException;
import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.ReadAccessDeniedException;
import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.client.CloudStoreClient;
import co.codewizards.cloudstore.core.oio.File;
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

	private LocalRepoManager localRepoManagerLocal;

	private String localPathPrefix;
	private UUID remoteRepositoryId;
	private String remotePathPrefix1;
	private String remotePathPrefix2Encrypted;
	private String remotePathPrefix2Plain;
	private URL remoteRootURLWithPathPrefixForLocalSrc;
	private URL remoteRootURLWithPathPrefixForLocalDest;

	private UserRepoKeyRing ownerUserRepoKeyRing;

	@Before
	public void before() {
		localPathPrefix = "";
		remotePathPrefix1 = "";
		remotePathPrefix2Plain = "";
		remotePathPrefix2Encrypted = "";
	}

	@After
	public void after() {
		if (localRepoManagerLocal != null) {
			localRepoManagerLocal.close();
			localRepoManagerLocal = null;
		}
	}

	private File getLocalRootWithPathPrefix() {
		if (localPathPrefix.isEmpty())
			return localSrcRoot;

		return createFile(localSrcRoot, localPathPrefix);
	}

	private File getRemoteRootWithPathPrefix1() {
		if (remotePathPrefix1.isEmpty())
			return remoteRoot;

		final File file = createFile(remoteRoot, remotePathPrefix1);
		return file;
	}

	private File getRemoteRootWithPathPrefix2() {
		assertNotNull("remotePathPrefix2Encrypted", remotePathPrefix2Encrypted);
		if (remotePathPrefix2Encrypted.isEmpty())
			return remoteRoot;

		final File file = createFile(remoteRoot, remotePathPrefix2Encrypted);
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
			grantRemotePathPrefix2Encrypted(PermissionType.read,
					otherUserRepoKeyRing.getUserRepoKeys(remoteRepositoryId).get(0).getPublicKey());

			createFileWithRandomContent(localSrcRoot, "xxxxxxx");

			final File child_2 = createFile(localSrcRoot, "2");
			createFile(child_2, "a").delete();
			createFileWithRandomContent(child_2, "a"); // overwrite
			createFileWithRandomContent(child_2, "yyyyyyyy"); // new file

			final File child_3 = createFile(localSrcRoot, remotePathPrefix2Plain);
			createFile(child_3, "b").delete();
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

			createFile(child_2, "yyyyyyyy").delete();
			createFileWithRandomContent(child_2, "yyyyyyyy"); // overwrite
			createFileWithRandomContent(child_2, "ttttt"); // new file

			createFile(child_3, "c").delete();
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
		final PublicKey publicKey1 = otherUserRepoKeyRing1.getUserRepoKeys(remoteRepositoryId).get(0).getPublicKey();
		grantRemotePathPrefix2Encrypted(PermissionType.read,
				publicKey1);
		grantRemotePathPrefix2Encrypted(PermissionType.read,
				otherUserRepoKeyRing2.getUserRepoKeys(remoteRepositoryId).get(0).getPublicKey());

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

		revokeRemotePathPrefix2Encrypted(PermissionType.read, publicKey1);
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
				Assert.fail("Could still check-out after access rights were revoked!");
			} catch (final AccessDeniedException x) {
				logger.info("Fine! Expected this AccessDeniedException: " + x);
			}

			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing2);
			createLocalDestinationRepo();

			logger.info("");
			logger.info("");
			logger.info("");
			logger.info("*** before syncFromRemoteToLocalDest() ***");
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}
	}

	@Test
	public void syncFromLocalToRemoteToLocalWithPathPrefixWithWritePermissionGrantedAndRevoked() throws Exception {
		remotePathPrefix2Plain = "/3 + &#ä";

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();

		final UserRepoKeyRing otherUserRepoKeyRing1 = createUserRepoKeyRing();
		final PublicKey publicKey1 = otherUserRepoKeyRing1.getUserRepoKeys(remoteRepositoryId).get(0).getPublicKey();
		grantRemotePathPrefix2Encrypted(PermissionType.read,
				publicKey1);

		grantRemotePathPrefix2Encrypted(PermissionType.write, publicKey1);

		syncFromLocalSrcToRemote();

		final UserRepoKeyRing ownerUserRepoKeyRing = cryptreeRepoTransportFactory.getUserRepoKeyRing();
		assertThat(ownerUserRepoKeyRing).isNotNull();
		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);
			createLocalDestinationRepo();
			syncFromRemoteToLocalDest();

			final File child_3 = createFile(localSrcRoot, remotePathPrefix2Plain);
			createFile(child_3, "b").delete();
			createFileWithRandomContent(child_3, "b"); // overwrite
			createFileWithRandomContent(child_3, "zzzzzzz"); // new file

			syncFromLocalSrcToRemote();
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}

		revokeRemotePathPrefix2Encrypted(PermissionType.write, publicKey1);

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);

			// The following write should still work, because the revocation becomes active only in the next up-sync.
			final File child_3 = createFile(localSrcRoot, remotePathPrefix2Plain);
			createFile(child_3, "b").delete();
			createFileWithRandomContent(child_3, "b"); // overwrite
			createFileWithRandomContent(child_3, "abczzzz"); // new file

			// Because the user is not allowed to enact the revocation, this does not yet have any
			// effect on setting Permission.validTo.
			syncFromLocalSrcToRemote();
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}

		// We enact the revocation in the other repository - this should work fine ;-)
		syncFromRemoteToLocalDest();

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);

			// And now, the next write(s) should fail, because the revocation should now be active.
			final File child_3 = createFile(localSrcRoot, remotePathPrefix2Plain);
			createFile(child_3, "b").delete();
			createFileWithRandomContent(child_3, "b"); // overwrite

			try {
				syncFromLocalSrcToRemote();
				Assert.fail("Could still write after access rights were revoked!");
			} catch (final WriteAccessDeniedException x) {
				logger.info("Fine! Expected this WriteAccessDeniedException: " + x);
			}

			createFileWithRandomContent(child_3, "abcyyyy"); // new file
			try {
				syncFromLocalSrcToRemote();
				Assert.fail("Could still write after access rights were revoked!");
			} catch (final WriteAccessDeniedException x) {
				logger.info("Fine! Expected this WriteAccessDeniedException: " + x);
			}

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
				Assert.fail("ReadAccessDeniedException was *not* thrown! It should have been!");
			} catch (final ReadAccessDeniedException x) {
				logger.info("syncFromLocalToRemoteToLocalWithPathPrefixWithoutSubdirClearanceKey: Caught ReadAccessDeniedException as expected.");
			}
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}
	}

	private void createLocalSourceAndRemoteRepo() throws Exception {
		localSrcRoot = newTestRepositoryLocalRoot("local-src");
		assertThat(localSrcRoot.exists()).isFalse();
		localSrcRoot.mkdirs();
		assertThat(localSrcRoot.isDirectory()).isTrue();

		remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot.exists()).isFalse();
		remoteRoot.mkdirs();
		assertThat(remoteRoot.isDirectory()).isTrue();

		localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localSrcRoot);
		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);

		remoteRepositoryId = localRepoManagerRemote.getRepositoryId();
		remoteRootURLWithPathPrefixForLocalSrc = getRemoteRootURLWithPathPrefixForLocalSrc(remoteRepositoryId);
		localRepoManagerRemote.close();

		new CloudStoreClient("requestRepoConnection", getLocalRootWithPathPrefix().getPath(), remoteRootURLWithPathPrefixForLocalSrc.toExternalForm()).execute();
		new CloudStoreClient("acceptRepoConnection", getRemoteRootWithPathPrefix1().getPath()).execute();

		ownerUserRepoKeyRing = createUserRepoKeyRing();
		cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
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
				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
				localDestRoot);
	}

	private void createLocalDestinationRepo() throws Exception {
		localDestRoot = newTestRepositoryLocalRoot("local-dest");
		assertThat(localDestRoot.exists()).isFalse();
		localDestRoot.mkdirs();
		assertThat(localDestRoot.isDirectory()).isTrue();

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
				final RepoFile repoFile = repoFileDao.getRepoFile(getLocalRootWithPathPrefix(), createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain));
				final CryptoRepoFile cryptoRepoFile = transaction.getDao(CryptoRepoFileDao.class).getCryptoRepoFileOrFail(repoFile);
				remotePathPrefix2Encrypted = cryptoRepoFile.getServerPath();
				transaction.commit();
			}
		}
	}

	private void grantRemotePathPrefix2Encrypted(final PermissionType permissionType, final UserRepoKey.PublicKey userRepoKeyPublicKey) {
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginWriteTransaction();)
			{
				final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
						transaction, remoteRepositoryId,
						remotePathPrefix2Encrypted,
						cryptreeRepoTransportFactory.getUserRepoKeyRing());
				cryptree.grantPermission(remotePathPrefix2Plain, permissionType, userRepoKeyPublicKey);

				transaction.commit();
			}
		}
	}

	private void revokeRemotePathPrefix2Encrypted(final PermissionType permissionType, final UserRepoKey.PublicKey userRepoKeyPublicKey) {
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginWriteTransaction();)
			{
				final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
						transaction, remoteRepositoryId,
						remotePathPrefix2Encrypted,
						cryptreeRepoTransportFactory.getUserRepoKeyRing());
				cryptree.revokePermission(remotePathPrefix2Plain, permissionType, Collections.singleton(userRepoKeyPublicKey.getUserRepoKeyId()));

				transaction.commit();
			}
		}
	}


}
