package org.subshare.test;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

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

	private File localRoot;
	private File remoteRoot;

	private String localPathPrefix;
	private String remotePathPrefix1;
	private String remotePathPrefix2Encrypted;
	private String remotePathPrefix2Plain;
	private URL remoteRootURLWithPathPrefix1;
	private URL remoteRootURLWithPathPrefix2;

	@Before
	public void before() {
		localPathPrefix = "";
		remotePathPrefix1 = "";
		remotePathPrefix2Plain = "";
		remotePathPrefix2Encrypted = "";
	}

	private File getLocalRootWithPathPrefix() {
		if (localPathPrefix.isEmpty())
			return localRoot;

		return new File(localRoot, localPathPrefix);
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

	private URL getRemoteRootURLWithPathPrefix1(final UUID remoteRepositoryId) throws MalformedURLException {
		final URL remoteRootURL = UrlUtil.appendNonEncodedPath(new URL(getSecureUrl() + "/" + remoteRepositoryId),  remotePathPrefix1);
		return remoteRootURL;
	}

	private URL getRemoteRootURLWithPathPrefix2(final UUID remoteRepositoryId) throws MalformedURLException {
		final URL remoteRootURL = UrlUtil.appendNonEncodedPath(new URL(getSecureUrl() + "/" + remoteRepositoryId),  remotePathPrefix2Encrypted);
		return remoteRootURL;
	}

	@Test
	public void syncFromLocalToRemoteToLocal() throws Exception {
		localRoot = newTestRepositoryLocalRoot("local-src");
		assertThat(localRoot).doesNotExist();
		localRoot.mkdirs();
		assertThat(localRoot).isDirectory();

		remoteRoot = newTestRepositoryLocalRoot("remote");
		assertThat(remoteRoot).doesNotExist();
		remoteRoot.mkdirs();
		assertThat(remoteRoot).isDirectory();

		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localRoot);
		assertThat(localRepoManagerLocal).isNotNull();

		final LocalRepoManager localRepoManagerRemote = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot);
		assertThat(localRepoManagerRemote).isNotNull();

		final UUID remoteRepositoryId = localRepoManagerRemote.getRepositoryId();
		remoteRootURLWithPathPrefix1 = getRemoteRootURLWithPathPrefix1(remoteRepositoryId);
		localRepoManagerRemote.close();

		new CloudStoreClient("requestRepoConnection", getLocalRootWithPathPrefix().getPath(), remoteRootURLWithPathPrefix1.toExternalForm()).execute();
		new CloudStoreClient("acceptRepoConnection", getRemoteRootWithPathPrefix1().getPath()).execute();

		final File child_1 = createDirectory(localRoot, "1 {11 11ä11} 1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		final File child_2 = createDirectory(localRoot, "2");

		createFileWithRandomContent(child_2, "a");

		final File child_2_1 = createDirectory(child_2, "1 {11 11ä11} 1");
		createFileWithRandomContent(child_2_1, "a");
		createFileWithRandomContent(child_2_1, "b");

		final File child_3 = createDirectory(localRoot, "3 + &#ä");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c");
		createFileWithRandomContent(child_3, "d");

		final File child_3_5 = createDirectory(child_3, "5");
		createFileWithRandomContent(child_3_5, "h");
		createFileWithRandomContent(child_3_5, "i");

		localRepoManagerLocal.localSync(new LoggerProgressMonitor(logger));

//		assertThatFilesInRepoAreCorrect(localRoot);

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix1);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

//		assertThatFilesInRepoAreCorrect(localRoot);

		final LocalRepoTransaction transaction = localRepoManagerLocal.beginReadTransaction();
		final RepoFileDao repoFileDao = transaction.getDao(RepoFileDao.class);
		final RepoFile repoFile = repoFileDao.getRepoFile(getLocalRootWithPathPrefix(), new File(getLocalRootWithPathPrefix(), remotePathPrefix2Plain));
		final CryptoRepoFile cryptoRepoFile = transaction.getDao(CryptoRepoFileDao.class).getCryptoRepoFileOrFail(repoFile);
		remotePathPrefix2Encrypted = cryptoRepoFile.getServerPath();
		transaction.commit();

		localRepoManagerLocal.close();

//		assertThatNoCollisionInRepo(localRoot);
//		assertThatNoCollisionInRepo(remoteRoot);

		final File localDestRoot = newTestRepositoryLocalRoot("local-dest");
		assertThat(localDestRoot).doesNotExist();
		localDestRoot.mkdirs();
		assertThat(localDestRoot).isDirectory();

		final LocalRepoManager localDestRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localDestRoot);
		assertThat(localDestRepoManagerLocal).isNotNull();
		localDestRepoManagerLocal.close();

		remoteRootURLWithPathPrefix2 = getRemoteRootURLWithPathPrefix2(remoteRepositoryId);
		new CloudStoreClient("requestRepoConnection", localDestRoot.getPath(), remoteRootURLWithPathPrefix2.toExternalForm()).execute();
		new CloudStoreClient("acceptRepoConnection", getRemoteRootWithPathPrefix2().getPath()).execute();

		final RepoToRepoSync repoToRepoSync2 = new RepoToRepoSync(localDestRoot, remoteRootURLWithPathPrefix2);
		repoToRepoSync2.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync2.close();

		assertDirectoriesAreEqualRecursively(
				(remotePathPrefix2Plain.isEmpty() ? getLocalRootWithPathPrefix() : new File(getLocalRootWithPathPrefix(), remotePathPrefix2Plain)),
				localDestRoot);
	}

	@Test
	public void syncFromLocalToRemoteToLocalWithPathPrefix() throws Exception {
		remotePathPrefix2Plain = "/3 + &#ä";
		syncFromLocalToRemoteToLocal();
	}

}
