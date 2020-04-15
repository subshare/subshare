package org.subshare.local;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;
import static co.codewizards.cloudstore.local.db.DatabaseAdapterFactory.*;
import static co.codewizards.cloudstore.local.db.ExternalJdbcDatabaseAdapter.*;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRegistryImpl;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.persistence.UserRepoKeyPublicKey;

import co.codewizards.cloudstore.local.db.DatabaseAdapterFactoryRegistry;
import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.DevMode;
import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.io.IOutputStream;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import mockit.Mock;
import mockit.MockUp;

public abstract class AbstractTest {

	protected static String jvmInstanceDir;

	static {
		DevMode.enableDevMode();
		final Uid jvmInstanceId = new Uid(); // for parallel test execution ;-)
		jvmInstanceDir = "build/jvm/" + jvmInstanceId;
		System.setProperty(ConfigDir.SYSTEM_PROPERTY_CONFIG_DIR, jvmInstanceDir + "/.cloudstore");
		System.setProperty(LocalRepoManager.SYSTEM_PROPERTY_KEY_SIZE, "1024");
		System.setProperty("testEnvironment", Boolean.TRUE.toString());
		createFile(jvmInstanceDir).mkdirs();
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

	private static class TestUserRegistry extends UserRegistryImpl {
		private final User user;

		public TestUserRegistry() {
			user = createUser();
			user.setUserId(new Uid());
			user.getPgpKeyIds().add(PgpKey.TEST_DUMMY_PGP_KEY_ID);
			user.getEmails().add("user@domain.tld");
			user.setFirstName("Hans");
			user.setLastName("Müller");
			user.getUserRepoKeyRingOrCreate();
		}

		@Override
		protected void read() {
			// nothing
		}

		@Override
		public synchronized Collection<User> getUsers() {
			return Collections.singleton(user);
		}
	}

	@Before
	public void before() throws Exception {
		PgpRegistry.getInstance().clearCache();

		new MockUp<UserRepoKeyPublicKeyHelper>() {
			@Mock
			private void createUserIdentities(final UserRepoKeyPublicKey userRepoKeyPublicKey) {
				// Our mock should do nothing, because we don't have a real UserRegistry here.
			}
		};
	}

	@After
	public void after() {
		PgpRegistry.getInstance().clearCache();
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
		return createFileWithRandomContent(parent, name, 0);
	}

	protected File createFileWithRandomContent(final File parent, final String name, final long minLength) throws IOException {
		final File file = createFile(parent, name);
		return createFileWithRandomContent(file, minLength);
	}

	protected File createFileWithRandomContent(final File file) throws IOException {
		return createFileWithRandomContent(file, 0);
	}

	protected File createFileWithRandomContent(final File file, final long minLength) throws IOException {
		assertThat(file.exists()).isFalse(); // prevent accidentally overwriting important data ;-)
		final IOutputStream out = file.createOutputStream();
		final byte[] buf = new byte[1 + random.nextInt(10241)];
		final int loops = 1 + random.nextInt(100);
		for (int i = 0; i < loops; ++i) {
			random.nextBytes(buf);
			out.write(buf);
		}
		out.flush();
		while (file.length() < minLength) {
			random.nextBytes(buf);
			out.write(buf);
			out.flush();
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

	protected static void enablePostgresql() {
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_DATABASE_ADAPTER_NAME, "postgresql");

		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_HOST_NAME, getEnvOrFail("TEST_PG_HOST_NAME"));
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_USER_NAME, getEnvOrFail("TEST_PG_USER_NAME"));
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_PASSWORD, getEnvOrFail("TEST_PG_PASSWORD"));

		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_PREFIX, "TEST_");
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_SUFFIX, "_TEST");
		DatabaseAdapterFactoryRegistry.getInstance().clearCache();
	}

	protected static void disablePostgresql() {
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_DATABASE_ADAPTER_NAME);

		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_HOST_NAME);
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_USER_NAME);
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_PASSWORD);

		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_PREFIX);
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CONFIG_KEY_JDBC_DB_NAME_SUFFIX);
		DatabaseAdapterFactoryRegistry.getInstance().clearCache();
	}

	protected static String getEnvOrFail(String key) {
		String value = System.getenv(key);
		if (value == null)
			throw new IllegalStateException("Environment-variable not set: " + key);

		return value;
	}
}
