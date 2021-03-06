package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static java.util.Objects.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.dto.HistoFrameDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDtoTreeNode;
import org.subshare.core.repo.local.HistoFrameFilter;
import org.subshare.core.repo.local.PlainHistoCryptoRepoFileFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.core.user.UserRepoKeyRingLookup;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.rest.client.transport.CryptreeRestRepoTransportFactoryImpl;

import co.codewizards.cloudstore.client.CloudStoreClient;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.util.UrlUtil;
import co.codewizards.cloudstore.local.LocalRepoTransactionImpl;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public abstract class AbstractRepoToRepoSyncIT extends AbstractIT {

	private static final Logger logger = LoggerFactory.getLogger(AbstractRepoToRepoSyncIT.class);

	protected File localSrcRoot;
	protected File localDestRoot;
	protected File remoteRoot;
	protected LocalRepoManager localSrcRepoManagerLocal;
	protected LocalRepoManager localDestRepoManagerLocal;
	protected String localPathPrefix;
	protected UUID remoteRepositoryId;
	protected String remotePathPrefix1Plain; // for local-SOURCE-repo
	protected String remotePathPrefix1Encrypted; // for local-SOURCE-repo
	protected String remotePathPrefix2Plain; // for local-DEST-repo
	protected String remotePathPrefix2Encrypted; // for local-DEST-repo
	protected URL remoteRootURLWithPathPrefixForLocalSrc;
	protected URL remoteRootURLWithPathPrefixForLocalDest;
	protected UserRepoKeyRing ownerUserRepoKeyRing;

	private static UserRepoKeyRingLookup originalUserRepoKeyRingLookup;

	@BeforeClass
	public static void beforeAbstractRepoToRepoSyncIT() {
		originalUserRepoKeyRingLookup = UserRepoKeyRingLookup.Helper.getUserRepoKeyRingLookup();
	}

	@AfterClass
	public static void afterAbstractRepoToRepoSyncIT() {
		if (originalUserRepoKeyRingLookup != null)
			UserRepoKeyRingLookup.Helper.setUserRepoKeyRingLookup(originalUserRepoKeyRingLookup);
	}

	@Override
	public void before() throws Exception {
		super.before();
		localPathPrefix = "";
		remotePathPrefix1Plain = "";
		remotePathPrefix1Encrypted = "";
		remotePathPrefix2Plain = "";
		remotePathPrefix2Encrypted = "";
	}

	@Override
	public void after() throws Exception {
		if (localSrcRepoManagerLocal != null) {
			localSrcRepoManagerLocal.close();
			localSrcRepoManagerLocal = null;
		}
		if (localDestRepoManagerLocal != null) {
			localDestRepoManagerLocal.close();
			localDestRepoManagerLocal = null;
		}
		if (userRegistryImplMockUp != null) {
//			userRegistryImplMockUp.tearDown(); // should be done automatically, but since we need to manage the reference, anyway, we do this explicitly here, too.
			userRegistryImplMockUp = null;
		}
		super.after();
	}

	protected File getLocalRootWithPathPrefix() {
		if (localPathPrefix.isEmpty())
			return localSrcRoot;

		return createFile(localSrcRoot, localPathPrefix);
	}

//	private File getRemoteRootWithPathPrefix1() {
//		requireNonNull("remotePathPrefix1Encrypted", remotePathPrefix1Encrypted);
//		if (remotePathPrefix1Encrypted.isEmpty())
//			return remoteRoot;
//
//		final File file = createFile(remoteRoot, remotePathPrefix1Encrypted);
//		return file;
//	}
//
//	private File getRemoteRootWithPathPrefix2() {
//		requireNonNull("remotePathPrefix2Encrypted", remotePathPrefix2Encrypted);
//		if (remotePathPrefix2Encrypted.isEmpty())
//			return remoteRoot;
//
//		final File file = createFile(remoteRoot, remotePathPrefix2Encrypted);
//		return file;
//	}

	private URL getRemoteRootURLWithPathPrefixForLocalSrc(final UUID remoteRepositoryId)
			throws MalformedURLException {
				final URL remoteRootURL = UrlUtil.appendNonEncodedPath(new URL(getSecureUrl() + "/" + remoteRepositoryId),  remotePathPrefix1Encrypted);
				return remoteRootURL;
			}

	protected URL getRemoteRootURLWithPathPrefixForLocalDest(final UUID remoteRepositoryId)
			throws MalformedURLException {
				final URL remoteRootURL = UrlUtil.appendNonEncodedPath(new URL(getSecureUrl() + "/" + remoteRepositoryId),  remotePathPrefix2Encrypted);
				return remoteRootURL;
			}

	protected UserRepoKeyRing createUserRepoKeyRing() {
		requireNonNull(remoteRepositoryId, "remoteRepositoryId");
		return createUserRepoKeyRing(remoteRepositoryId);
	}

	protected void createLocalSourceAndRemoteRepo() throws Exception {
		localSrcRoot = newTestRepositoryLocalRoot("local-src");
		assertThat(localSrcRoot.exists()).isFalse();
		localSrcRoot.mkdirs();
		assertThat(localSrcRoot.isDirectory()).isTrue();

		remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot.exists()).isFalse();
		remoteRoot.mkdirs();
		assertThat(remoteRoot.isDirectory()).isTrue();

		localSrcRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localSrcRoot);
		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);

		remoteRepositoryId = localRepoManagerRemote.getRepositoryId();
		remoteRootURLWithPathPrefixForLocalSrc = getRemoteRootURLWithPathPrefixForLocalSrc(remoteRepositoryId);
		localRepoManagerRemote.close();

		ownerUserRepoKeyRing = createUserRepoKeyRing();
		UserRepoKeyRingLookup.Helper.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(ownerUserRepoKeyRing));

		new CloudStoreClient("requestRepoConnection", getLocalRootWithPathPrefix().getPath(), remoteRootURLWithPathPrefixForLocalSrc.toExternalForm()).execute();
		//	acceptRepoConnection is not needed, because already accepted implicitly by *signed* request
	}

	protected void populateLocalSourceRepo() throws Exception {
		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);

		final File child_1 = createDirectory(localSrcRoot, "1 {11 11ä11#+} 1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		final File child_2 = createDirectory(localSrcRoot, "2");

		createFileWithRandomContent(child_2, "a");

		final File child_2_1 = createDirectory(child_2, "1 {11 11ä11#+} 1");
		createFileWithRandomContent(child_2_1, "a");
		createFileWithRandomContent(child_2_1, "b");

		final File child_3 = createDirectory(localSrcRoot, "3 + &#ä");

		createFileWithRandomContent(child_3, "aa");
		createFileWithRandomContent(child_3, "bb");
		createFileWithRandomContent(child_3, "cc");
		createFileWithRandomContent(child_3, "dd");

		final File child_3_5 = createDirectory(child_3, "5");
		createFileWithRandomContent(child_3_5, "h");
		createFileWithRandomContent(child_3_5, "i");

		localRepoManagerLocal.localSync(new LoggerProgressMonitor(logger));
		localRepoManagerLocal.close();
	}

	protected void syncFromLocalSrcToRemote() throws Exception {
		try (final RepoToRepoSync repoToRepoSync = createRepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefixForLocalSrc);)
		{
			repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		}
	}

	protected void syncFromRemoteToLocalDest() throws Exception {
		syncFromRemoteToLocalDest(true);
	}

	protected void syncFromRemoteToLocalDest(final boolean assertLocalSrcAndDestDirectoriesAreEqual) throws Exception {
		try (final RepoToRepoSync repoToRepoSync = createRepoToRepoSync(localDestRoot, remoteRootURLWithPathPrefixForLocalDest);) {
			repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		}

		if (assertLocalSrcAndDestDirectoriesAreEqual) {
			assertDirectoriesAreEqualRecursively(
					(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
					localDestRoot);
		}
	}

	protected RepoToRepoSync createRepoToRepoSync(final File localRoot, final URL remoteRoot) {
		return RepoToRepoSync.create(localRoot, remoteRoot);
	}

	protected void createLocalDestinationRepo() throws Exception {
		localDestRoot = newTestRepositoryLocalRoot("local-dest");
		assertThat(localDestRoot.exists()).isFalse();
		localDestRoot.mkdirs();
		assertThat(localDestRoot.isDirectory()).isTrue();

		localDestRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localDestRoot);
		assertThat(localDestRepoManagerLocal).isNotNull();

		remoteRootURLWithPathPrefixForLocalDest = getRemoteRootURLWithPathPrefixForLocalDest(remoteRepositoryId);

		if (! getUserRepoKeyRing(cryptreeRepoTransportFactory).getUserRepoKeys(remoteRepositoryId).isEmpty()) {
			new CloudStoreClient("requestRepoConnection", localDestRoot.getPath(), remoteRootURLWithPathPrefixForLocalDest.toExternalForm()).execute();
			//	acceptRepoConnection is not needed, because already accepted implicitly by *signed* request
		}
	}

	protected static UserRepoKeyRing getUserRepoKeyRing(CryptreeRestRepoTransportFactoryImpl factory) {
		UserRepoKeyRingLookup lookup = UserRepoKeyRingLookup.Helper.getUserRepoKeyRingLookup();
		if (lookup == null)
			return null;

		return ((StaticUserRepoKeyRingLookup) lookup).getUserRepoKeyRing();
	}

	protected void determineRemotePathPrefix2Encrypted() {
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginReadTransaction();)
			{
				final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
				final RepoFile repoFile = repoFileDao.getRepoFile(getLocalRootWithPathPrefix(), createFile(getLocalRootWithPathPrefix(), remotePathPrefix2Plain));
				final CryptoRepoFile cryptoRepoFile = transaction.getDao(CryptoRepoFileDao.class).getCryptoRepoFileOrFail(repoFile);
				remotePathPrefix2Encrypted = cryptoRepoFile.getServerPath();
				logger.info("determineRemotePathPrefix2Encrypted: remotePathPrefix2Encrypted={}", remotePathPrefix2Encrypted);
				transaction.commit();
			}
		}
	}

	protected void grantPermission(final String localPath, final PermissionType permissionType, final UserRepoKey.PublicKey userRepoKeyPublicKey) {
		grantPermission(localSrcRoot, localPath, permissionType, userRepoKeyPublicKey);
	}

	protected void grantPermission(final File localRoot, final String localPath, final PermissionType permissionType, final UserRepoKey.PublicKey userRepoKeyPublicKey) {
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginWriteTransaction();)
			{
				final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
						transaction, remoteRepositoryId,
						remotePathPrefix2Encrypted,
						getUserRepoKeyRing(cryptreeRepoTransportFactory));
				cryptree.grantPermission(localPath, permissionType, userRepoKeyPublicKey);

				transaction.commit();
			}
		}
	}

	protected void setPermissionsInherited(final String localPath, final boolean inherited) {
		setPermissionsInherited(localSrcRoot, localPath, inherited);
	}

	protected void setPermissionsInherited(final File localRoot, final String localPath, final boolean inherited) {
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginWriteTransaction();)
			{
				final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
						transaction, remoteRepositoryId,
						remotePathPrefix2Encrypted,
						getUserRepoKeyRing(cryptreeRepoTransportFactory));
				cryptree.setPermissionsInherited(localPath, inherited);

				transaction.commit();
			}
		}
	}

	protected void revokePermission(final String localPath, final PermissionType permissionType, final UserRepoKey.PublicKey userRepoKeyPublicKey) {
		revokePermission(localSrcRoot, localPath, permissionType, userRepoKeyPublicKey);
	}

	protected void revokePermission(final File localRoot, final String localPath, final PermissionType permissionType, final UserRepoKey.PublicKey userRepoKeyPublicKey) {
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginWriteTransaction();)
			{
				final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
						transaction, remoteRepositoryId,
						remotePathPrefix2Encrypted,
						getUserRepoKeyRing(cryptreeRepoTransportFactory));
				cryptree.revokePermission(localPath, permissionType, Collections.singleton(userRepoKeyPublicKey.getUserRepoKeyId()));

				transaction.commit();
			}
		}
	}

	protected PersistenceManager getTransactionalPersistenceManager(final File localRoot) {
		final PersistenceManagerFactory pmf;
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginWriteTransaction();)
			{
				pmf = ((LocalRepoTransactionImpl)transaction).getPersistenceManager().getPersistenceManagerFactory();
			}
		}
		final PersistenceManager pm = pmf.getPersistenceManager();
		pm.currentTransaction().begin();
		return pm;
	}

	protected List<PlainHistoCryptoRepoFileDto> getPlainHistoCryptoRepoFileDtos(LocalRepoManager localRepoManager, File file) throws IOException {
		final String path = "/" + localRepoManager.getLocalRoot().relativize(file).replace('\\', '/');
		SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localSrcRepoManagerLocal.getLocalRepoMetaData();
		List<PlainHistoCryptoRepoFileDto> result = new ArrayList<>();

		// TODO need to extend the filter with a path! Do this when extending the UI to show a history in every folder-detail-pane.
		// The current implementation is very inefficient - but we have only small test data, anyway ;-)
		Collection<HistoFrameDto> histoFrameDtos = localRepoMetaData.getHistoFrameDtos(new HistoFrameFilter());
		for (HistoFrameDto histoFrameDto : histoFrameDtos) {
			PlainHistoCryptoRepoFileFilter filter = new PlainHistoCryptoRepoFileFilter();
			filter.setHistoFrameId(histoFrameDto.getHistoFrameId());
			filter.setFillParents(true);
			Collection<PlainHistoCryptoRepoFileDto> plainHistoCryptoRepoFileDtos = localRepoMetaData.getPlainHistoCryptoRepoFileDtos(filter);
			PlainHistoCryptoRepoFileDtoTreeNode rootNode = PlainHistoCryptoRepoFileDtoTreeNode.createTree(plainHistoCryptoRepoFileDtos);
			for (PlainHistoCryptoRepoFileDtoTreeNode node : rootNode) {
				if (path.equals(node.getPath()))
					result.add(node.getPlainHistoCryptoRepoFileDto());
			}
		}

		Collections.sort(result, new Comparator<PlainHistoCryptoRepoFileDto>() {
			@Override
			public int compare(PlainHistoCryptoRepoFileDto o1, PlainHistoCryptoRepoFileDto o2) {
				Date signatureCreated1 = o1.getHistoCryptoRepoFileDto().getSignature().getSignatureCreated();
				Date signatureCreated2 = o2.getHistoCryptoRepoFileDto().getSignature().getSignatureCreated();
				return signatureCreated1.compareTo(signatureCreated2);
			}
		});

		return result;
	}
}
