package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.createFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.rest.client.transport.CryptreeRepoTransportFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.local.FilenameFilterSkipMetaDir;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;

public abstract class AbstractIT {
	static {
		System.setProperty(ConfigDir.SYSTEM_PROPERTY_CONFIG_DIR, "build/.cloudstore");
		System.setProperty(LocalRepoManager.SYSTEM_PROPERTY_KEY_SIZE, "1024");
		System.setProperty("testEnvironment", Boolean.TRUE.toString());
	}

	protected static final SecureRandom random = new SecureRandom();
	private static final SubShareServerTestSupport subShareServerTestSupport = new SubShareServerTestSupport();
	protected static LocalRepoManagerFactory localRepoManagerFactory = LocalRepoManagerFactory.Helper.getInstance();

	public static int getSecurePort() {
		return subShareServerTestSupport.getSecurePort();
	}

	public static String getSecureUrl() {
		return subShareServerTestSupport.getSecureUrl();
	}

	protected static CryptreeRepoTransportFactory cryptreeRepoTransportFactory;

	@BeforeClass
	public static void abstractIT_beforeClass() {
		if (subShareServerTestSupport.beforeClass()) {
			// *IMPORTANT* We run *all* tests in parallel in the same JVM. Therefore, we must - in this entire project - *not*
			// set any other dynamicX509TrustManagerCallbackClass!!! This setting is JVM-wide!
			cryptreeRepoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(CryptreeRepoTransportFactory.class);
			cryptreeRepoTransportFactory.setDynamicX509TrustManagerCallbackClass(TestDynamicX509TrustManagerCallback.class);
		}
	}

	@AfterClass
	public static void abstractIT_afterClass() {
		if (subShareServerTestSupport.afterClass()) {
			cryptreeRepoTransportFactory.setDynamicX509TrustManagerCallbackClass(null);
			cryptreeRepoTransportFactory.setUserRepoKeyRing(null);
			cryptreeRepoTransportFactory = null;
		}
	}

	protected static UserRepoKeyRing createUserRepoKeyRing(final UUID repositoryId) {
		final UserRegistry userRegistry = new TestUserRegistry();
		final User user = userRegistry.getUsers().iterator().next();
		final UserRepoKeyRing userRepoKeyRing = user.getUserRepoKeyRingOrCreate();
		user.createUserRepoKey(repositoryId);
		user.createUserRepoKey(repositoryId);
		return userRepoKeyRing;
	}

	private static class TestUserRegistry extends UserRegistry {
		private final User user;

		public TestUserRegistry() {
			user = new User();
			user.setUserId(new Uid());
			user.getPgpKeyIds().add(0L);
			user.getEmails().add("user@domain.tld");
			user.setFirstName("Hans");
			user.setLastName("Müller");
			user.getUserRepoKeyRingOrCreate();
		}

		@Override
		protected void readUserListFile() {
			// nothing
		}
		@Override
		protected void readPgpUsers() {
			// nothing
		}

		@Override
		public synchronized Collection<User> getUsers() {
			return Collections.singleton(user);
		}
	}

//	protected static UserRepoKeyRing createUserRepoKeyRing(final UUID repositoryId) {
//		final UserRepoKeyRing userRepoKeyRing = new UserRepoKeyRing();
//		createUserRepoKey(userRepoKeyRing, repositoryId);
//		createUserRepoKey(userRepoKeyRing, repositoryId);
//		return userRepoKeyRing;
//	}
//
//	protected static UserRepoKey createUserRepoKey(final UserRepoKeyRing userRepoKeyRing, final UUID repositoryId) {
//		final UserRepoKey userRepoKey = new UserRepoKey(userRepoKeyRing, repositoryId, KeyFactory.getInstance().createAsymmetricKeyPair());
//		userRepoKeyRing.addUserRepoKey(userRepoKey);
//		return userRepoKey;
//	}

	public static class TestDynamicX509TrustManagerCallback implements DynamicX509TrustManagerCallback {
		@Override
		public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(final CheckServerTrustedCertificateExceptionContext context) {
			final CheckServerTrustedCertificateExceptionResult result = new CheckServerTrustedCertificateExceptionResult();
			result.setTrusted(true);
			return result;
		}
	}

	protected File newTestRepositoryLocalRoot(final String suffix) throws IOException {
		assertThat(suffix).isNotNull();
		final long timestamp = System.currentTimeMillis();
		final int randomNumber = random.nextInt(BigInteger.valueOf(36).pow(5).intValue());
		final String repoName = Long.toString(timestamp, 36) + '-' + Integer.toString(randomNumber, 36) + (suffix.isEmpty() ? "" : "-") + suffix;
		final File localRoot = createFile(getTestRepositoryBaseDir(), repoName);
		addToFilesInRepo(localRoot, localRoot);
		return localRoot;
	}

	private File getLocalRootOrFail(final File file) throws IOException {
		final String filePath = file.getCanonicalPath();
		final Set<File> localRoots = localRepoManagerFactory.getLocalRoots();
		for (final File localRoot : localRoots) {
			final String localRootPath = localRoot.getPath();
			if (filePath.startsWith(localRootPath)) {
				return localRoot;
			}
		}
		throw new IllegalArgumentException("file is not contained in any open repository: " + filePath);
	}

	protected void addToFilesInRepo(File file) throws IOException {
		file = file.getAbsoluteFile();
		final File localRoot = getLocalRootOrFail(file);
		addToFilesInRepo(localRoot, file);
	}
	protected void addToFilesInRepo(final File localRoot, final File file) throws IOException {
//		localRoot = localRoot.getCanonicalFile();
//		file = file.getAbsoluteFile();
//		Set<File> filesInRepo = localRoot2FilesInRepo.get(localRoot);
//		if (filesInRepo == null) {
//			filesInRepo = new HashSet<File>();
//			localRoot2FilesInRepo.put(localRoot, filesInRepo);
//		}
//		filesInRepo.add(file);
		// TODO We should maybe register them, somehow?!
	}

	protected File getTestRepositoryBaseDir() {
		final File dir = createFile(createFile("build"), "repo");
		dir.mkdirs();
		return dir;
	}

	protected File createDirectory(final File parent, final String name) throws IOException {
		final File dir = createFile(parent, name);
		return createDirectory(dir);
	}
	protected File createDirectory(final File dir) throws IOException {
		assertThat(dir.exists()).isFalse();
		dir.mkdir();
		assertThat(dir.isDirectory()).isTrue();
		addToFilesInRepo(dir);
		return dir;
	}

	protected File createFileWithRandomContent(final File parent, final String name) throws IOException {
		final File file = createFile(parent, name);
		return createFileWithRandomContent(file);
	}

	protected File createFileWithRandomContent(final File file) throws IOException {
		assertThat(file.exists()).isFalse(); // prevent accidentally overwriting important data ;-)
		final OutputStream out = file.createOutputStream();
		final byte[] buf = new byte[1 + random.nextInt(10241)];
		final int loops = 1 + random.nextInt(100);
		for (int i = 0; i < loops; ++i) {
			random.nextBytes(buf);
			out.write(buf);
		}
		out.close();
		assertThat(file.isFile()).isTrue();
		addToFilesInRepo(file);
		return file;
	}

	protected void assertDirectoriesAreEqualRecursively(final File dir1, final File dir2) throws IOException {
		assertThat(dir1.isDirectory()).isTrue();
		assertThat(dir2.isDirectory()).isTrue();

		final boolean dir1IsSymbolicLink = dir1.isSymbolicLink();
		final boolean dir2IsSymbolicLink = dir2.isSymbolicLink();

		assertThat(dir1IsSymbolicLink).isEqualTo(dir2IsSymbolicLink);

		if (dir1IsSymbolicLink) {
			final String target1 = dir1.readSymbolicLinkToPathString();
			final String target2 = dir2.readSymbolicLinkToPathString();
			assertThat(target1).isEqualTo(target2);
			return;
		}

		final String[] children1 = dir1.list(new FilenameFilterSkipMetaDir());
		assertThat(children1).isNotNull();

		final String[] children2 = dir2.list(new FilenameFilterSkipMetaDir());
		assertThat(children2).isNotNull();

		Arrays.sort(children1);
		Arrays.sort(children2);

		assertThat(children1).containsOnly(children2);

		for (final String childName : children1) {
			final File child1 = createFile(dir1, childName);
			final File child2 = createFile(dir2, childName);

			final boolean child1IsSymbolicLink = child1.isSymbolicLink();
			final boolean child2IsSymbolicLink = child2.isSymbolicLink();

			assertThat(child1IsSymbolicLink).isEqualTo(child2IsSymbolicLink);

			if (child1IsSymbolicLink) {
				final String target1 = child1.readSymbolicLinkToPathString();
				final String target2 = child2.readSymbolicLinkToPathString();
				assertThat(target1).isEqualTo(target2);
			}
			else if (child1.isFile()) {
				assertThat(child2.isFile());
				assertThat(IOUtil.compareFiles(child1, child2)).isTrue();
			}
			else if (child1.isDirectory())
				assertDirectoriesAreEqualRecursively(child1, child2);
		}
	}
}
