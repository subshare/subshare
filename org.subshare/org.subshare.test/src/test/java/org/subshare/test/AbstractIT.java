package org.subshare.test;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;

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

	@BeforeClass
	public static void abstractIT_beforeClass() {
		subShareServerTestSupport.beforeClass();
	}

	@AfterClass
	public static void abstractIT_afterClass() {
		subShareServerTestSupport.afterClass();
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
}
