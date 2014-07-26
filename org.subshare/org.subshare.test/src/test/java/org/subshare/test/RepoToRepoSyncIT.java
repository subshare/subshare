package org.subshare.test;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import org.subshare.core.crypto.KeyFactory;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.rest.client.transport.CryptreeRepoTransportFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.client.CloudStoreClient;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;

public class RepoToRepoSyncIT extends AbstractIT {

	private static final Logger logger = LoggerFactory.getLogger(RepoToRepoSyncIT.class);

	private File localRoot;
	private File remoteRoot;

	private String localPathPrefix;
	private String remotePathPrefix;
	private URL remoteRootURLWithPathPrefix;

	@Before
	public void before() {
		localPathPrefix = "";
		remotePathPrefix = "";
	}

	private File getLocalRootWithPathPrefix() {
		if (localPathPrefix.isEmpty())
			return localRoot;

		return new File(localRoot, localPathPrefix);
	}

	private File getRemoteRootWithPathPrefix() {
		if (remotePathPrefix.isEmpty())
			return remoteRoot;

		final File file = new File(remoteRoot, remotePathPrefix);
		return file;
	}

	public static class TestDynamicX509TrustManagerCallback implements DynamicX509TrustManagerCallback {
		@Override
		public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(final CheckServerTrustedCertificateExceptionContext context) {
			final CheckServerTrustedCertificateExceptionResult result = new CheckServerTrustedCertificateExceptionResult();
			result.setTrusted(true);
			return result;
		}
	}

	private static CryptreeRepoTransportFactory cryptreeRepoTransportFactory;

	@BeforeClass
	public static void beforeClass() throws Exception {
		cryptreeRepoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(CryptreeRepoTransportFactory.class);
		cryptreeRepoTransportFactory.setDynamicX509TrustManagerCallbackClass(TestDynamicX509TrustManagerCallback.class);
		cryptreeRepoTransportFactory.setUserRepoKeyRing(createUserRepoKeyRing());
	}

	@AfterClass
	public static void afterClass() {
		cryptreeRepoTransportFactory.setDynamicX509TrustManagerCallbackClass(null);
		cryptreeRepoTransportFactory.setUserRepoKeyRing(null);
	}

	private static UserRepoKeyRing createUserRepoKeyRing() {
		final UserRepoKeyRing userRepoKeyRing = new UserRepoKeyRing();
		userRepoKeyRing.addUserRepoKey(createUserRepoKey());
		userRepoKeyRing.addUserRepoKey(createUserRepoKey());
		return userRepoKeyRing;
	}

	private static UserRepoKey createUserRepoKey() {
		final UserRepoKey userRepoKey = new UserRepoKey(KeyFactory.getInstance().createAsymmetricKeyPair());
		return userRepoKey;
	}

	private URL getRemoteRootURLWithPathPrefix(final UUID remoteRepositoryId) throws MalformedURLException {
		final URL remoteRootURL = new URL(getSecureUrl() + "/" + remoteRepositoryId + remotePathPrefix);
		return remoteRootURL;
	}

	@Test
	public void syncFromLocalToRemote() throws Exception {
		localRoot = newTestRepositoryLocalRoot("local");
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
		remoteRootURLWithPathPrefix = getRemoteRootURLWithPathPrefix(remoteRepositoryId);

		new CloudStoreClient("requestRepoConnection", getLocalRootWithPathPrefix().getPath(), remoteRootURLWithPathPrefix.toExternalForm()).execute();
		new CloudStoreClient("acceptRepoConnection", getRemoteRootWithPathPrefix().getPath()).execute();

		final File child_1 = createDirectory(localRoot, "1 {11 11ä11} 1");

		createFileWithRandomContent(child_1, "a");
		createFileWithRandomContent(child_1, "b");
		createFileWithRandomContent(child_1, "c");

		final File child_2 = createDirectory(localRoot, "2");

		createFileWithRandomContent(child_2, "a");

		final File child_2_1 = createDirectory(child_2, "1 {11 11ä11} 1");
		createFileWithRandomContent(child_2_1, "a");
		createFileWithRandomContent(child_2_1, "b");

		final File child_3 = createDirectory(localRoot, "3");

		createFileWithRandomContent(child_3, "a");
		createFileWithRandomContent(child_3, "b");
		createFileWithRandomContent(child_3, "c");
		createFileWithRandomContent(child_3, "d");

		localRepoManagerLocal.localSync(new LoggerProgressMonitor(logger));

//		assertThatFilesInRepoAreCorrect(localRoot);

		final RepoToRepoSync repoToRepoSync = new RepoToRepoSync(getLocalRootWithPathPrefix(), remoteRootURLWithPathPrefix);
		repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		repoToRepoSync.close();

//		assertThatFilesInRepoAreCorrect(localRoot);

		localRepoManagerLocal.close();
		localRepoManagerRemote.close();

//		assertThatNoCollisionInRepo(localRoot);
//		assertThatNoCollisionInRepo(remoteRoot);
//		assertDirectoriesAreEqualRecursively(getLocalRootWithPathPrefix(), getRemoteRootWithPathPrefix());
	}

}
