package org.subshare.test;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Set;

import org.subshare.core.crypto.KeyFactory;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.rest.client.transport.CryptreeRepoTransportFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import co.codewizards.cloudstore.core.config.ConfigDir;
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

	private static CryptreeRepoTransportFactory cryptreeRepoTransportFactory;

	@BeforeClass
	public static void abstractIT_beforeClass() {
		if (subShareServerTestSupport.beforeClass()) {
			// *IMPORTANT* We run *all* tests in parallel in the same JVM. Therefore, we must - in this entire project - *not*
			// set any other dynamicX509TrustManagerCallbackClass!!! This setting is JVM-wide!
			cryptreeRepoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(CryptreeRepoTransportFactory.class);
			cryptreeRepoTransportFactory.setDynamicX509TrustManagerCallbackClass(TestDynamicX509TrustManagerCallback.class);

			cryptreeRepoTransportFactory.setUserRepoKeyRing(createUserRepoKeyRing());
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

	protected static UserRepoKeyRing createUserRepoKeyRing() {
		final UserRepoKeyRing userRepoKeyRing = new UserRepoKeyRing();
		createUserRepoKey(userRepoKeyRing);
		createUserRepoKey(userRepoKeyRing);
		return userRepoKeyRing;
	}

	protected static UserRepoKey createUserRepoKey(final UserRepoKeyRing userRepoKeyRing) {
		final UserRepoKey userRepoKey = new UserRepoKey(userRepoKeyRing, KeyFactory.getInstance().createAsymmetricKeyPair());
		userRepoKeyRing.addUserRepoKey(userRepoKey);
		return userRepoKey;
	}

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
		final File localRoot = new File(getTestRepositoryBaseDir(), repoName);
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
		final File dir = new File(new File("build"), "repo");
		dir.mkdirs();
		return dir;
	}

	protected File createDirectory(final File parent, final String name) throws IOException {
		final File dir = new File(parent, name);
		return createDirectory(dir);
	}
	protected File createDirectory(final File dir) throws IOException {
		assertThat(dir).doesNotExist();
		dir.mkdir();
		assertThat(dir).isDirectory();
		addToFilesInRepo(dir);
		return dir;
	}

	protected File createFileWithRandomContent(final File parent, final String name) throws IOException {
		final File file = new File(parent, name);
		return createFileWithRandomContent(file);
	}

	protected File createFileWithRandomContent(final File file) throws IOException {
		assertThat(file).doesNotExist(); // prevent accidentally overwriting important data ;-)
		final OutputStream out = new FileOutputStream(file);
		final byte[] buf = new byte[1 + random.nextInt(10241)];
		final int loops = 1 + random.nextInt(100);
		for (int i = 0; i < loops; ++i) {
			random.nextBytes(buf);
			out.write(buf);
		}
		out.close();
		assertThat(file).isFile();
		addToFilesInRepo(file);
		return file;
	}

	protected void assertDirectoriesAreEqualRecursively(final File dir1, final File dir2) throws IOException {
		assertThat(dir1).isDirectory();
		assertThat(dir2).isDirectory();

		final boolean dir1IsSymbolicLink = Files.isSymbolicLink(dir1.toPath());
		final boolean dir2IsSymbolicLink = Files.isSymbolicLink(dir2.toPath());

		assertThat(dir1IsSymbolicLink).isEqualTo(dir2IsSymbolicLink);

		if (dir1IsSymbolicLink) {
			final Path target1 = Files.readSymbolicLink(dir1.toPath());
			final Path target2 = Files.readSymbolicLink(dir2.toPath());
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
			final File child1 = new File(dir1, childName);
			final File child2 = new File(dir2, childName);

			final boolean child1IsSymbolicLink = Files.isSymbolicLink(child1.toPath());
			final boolean child2IsSymbolicLink = Files.isSymbolicLink(child2.toPath());

			assertThat(child1IsSymbolicLink).isEqualTo(child2IsSymbolicLink);

			if (child1IsSymbolicLink) {
				final Path target1 = Files.readSymbolicLink(child1.toPath());
				final Path target2 = Files.readSymbolicLink(child2.toPath());
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
