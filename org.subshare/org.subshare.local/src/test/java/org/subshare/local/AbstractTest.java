package org.subshare.local;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.createFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRepoKeyRing;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public abstract class AbstractTest {
	static {
		final Uid jvmInstanceId = new Uid(); // for parallel test execution ;-)
		System.setProperty(ConfigDir.SYSTEM_PROPERTY_CONFIG_DIR, "build/" + jvmInstanceId + "/.cloudstore");
		System.setProperty(LocalRepoManager.SYSTEM_PROPERTY_KEY_SIZE, "1024");
		System.setProperty("testEnvironment", Boolean.TRUE.toString());
		createFile("build/" + jvmInstanceId).mkdir();
	}

	protected static final SecureRandom random = new SecureRandom();

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
			user.getPgpKeyIds().add(PgpKey.TEST_DUMMY_PGP_KEY_ID);
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

//	protected static UserRepoKey createUserRepoKey(final UserRepoKeyRing userRepoKeyRing, final UUID repositoryId) {
//		final UserRepoKey userRepoKey = new UserRepoKey(userRepoKeyRing, repositoryId, KeyFactory.getInstance().createAsymmetricKeyPair());
//		userRepoKeyRing.addUserRepoKey(userRepoKey);
//		return userRepoKey;
//	}

	protected File newTestRepositoryLocalRoot(final String suffix) throws IOException {
		assertThat(suffix).isNotNull();
		final long timestamp = System.currentTimeMillis();
		final int randomNumber = random.nextInt(BigInteger.valueOf(36).pow(5).intValue());
		final String repoName = Long.toString(timestamp, 36) + '-' + Integer.toString(randomNumber, 36) + (suffix.isEmpty() ? "" : "-") + suffix;
		final File localRoot = createFile(getTestRepositoryBaseDir(), repoName);
//		addToFilesInRepo(localRoot, localRoot);
		return localRoot;
	}

	protected File getTestRepositoryBaseDir() {
		final File dir = createFile(createFile("build"), "repo");
		dir.mkdirs();
		return dir;
	}

	protected static LocalRepoManager createLocalRepoManagerForNewRepository(final File localRoot) {
		return LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForNewRepository(localRoot);
	}

	protected static LocalRepoManager createLocalRepoManagerForExistingRepository(final File localRoot) {
		return LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);
	}

	protected static Cryptree getCryptree(final LocalRepoTransaction transaction, final UUID remoteRepositoryId, final String remotePathPrefix, final UserRepoKeyRing userRepoKeyRing) {
		return CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
				transaction, remoteRepositoryId, remotePathPrefix, userRepoKeyRing);
	}

	protected void createDirectoriesAndFiles(final File localRoot, final String ... localPaths) throws IOException {
		for (final String localPath : localPaths) {
			if (localPath.isEmpty() || "/".equals(localPath))
				continue;

			if (localPath.endsWith("/"))
				createDirectory(localRoot, localPath.substring(0, localPath.length() - 1));
			else
				createFileWithRandomContent(localRoot, localPath);
		}
	}

	protected File createDirectory(final File parent, final String name) throws IOException {
		final File dir = createFile(parent, name);
		return createDirectory(dir);
	}
	protected File createDirectory(final File dir) throws IOException {
		assertThat(dir.exists()).isFalse();
		dir.mkdir();
		assertThat(dir.isDirectory()).isTrue();
//		addToFilesInRepo(dir);
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
//		addToFilesInRepo(file);
		return file;
	}

	protected static String removeFinalSlash(final String path) {
		if (path.endsWith("/"))
			return path.substring(0, path.length() - 1);
		else
			return path;
	}
}
