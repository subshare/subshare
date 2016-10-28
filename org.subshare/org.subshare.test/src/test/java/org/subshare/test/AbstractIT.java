package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.locker.transport.LockerTransportFactoryRegistry;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.gnupg.GnuPgDir;
import org.subshare.core.pgp.transport.PgpTransportFactoryRegistryImpl;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRegistryImpl;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.FilePaddingLengthRandom;
import org.subshare.local.FilePaddingLengthRandom.LengthCategory;
import org.subshare.rest.client.locker.transport.RestLockerTransportFactory;
import org.subshare.rest.client.pgp.transport.RestPgpTransportFactory;
import org.subshare.rest.client.transport.CryptreeRestRepoTransportFactoryImpl;

import co.codewizards.cloudstore.core.DevMode;
import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.local.FilenameFilterSkipMetaDir;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public abstract class AbstractIT {

	private static final Logger logger = LoggerFactory.getLogger(AbstractIT.class);

	protected static String jvmInstanceDir;

	static {
		DevMode.enableDevMode();
		final Uid jvmInstanceId = new Uid(); // for parallel test execution ;-)
		jvmInstanceDir = "build/jvm/" + jvmInstanceId;
		final String configDirString = jvmInstanceDir + "/.subshare";
		System.setProperty(ConfigDir.SYSTEM_PROPERTY_CONFIG_DIR, configDirString);
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + GnuPgDir.CONFIG_KEY_GNU_PG_DIR, jvmInstanceDir + "/.gnupg");
		System.setProperty(LocalRepoManager.SYSTEM_PROPERTY_KEY_SIZE, "1024");
		System.setProperty("testEnvironment", Boolean.TRUE.toString());

		final File configDir = createFile(configDirString);
		configDir.mkdirs();

		try {
			copyResource(AbstractIT.class, "/logback-test.xml", createFile(configDir, "logback.client.xml"));
			copyResource(AbstractIT.class, "/logback-test.xml", createFile(configDir, "logback.server.xml"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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

	protected static CryptreeRestRepoTransportFactoryImpl cryptreeRepoTransportFactory;
	protected static RestPgpTransportFactory restPgpTransportFactory;
	protected static RestLockerTransportFactory restLockerTransportFactory;

	protected MockUp<UserRegistryImpl> userRegistryImplMockUp;

	@BeforeClass
	public static void abstractIT_beforeClass() throws Exception {
		// In order to make sure our tests are not unnecessarily slowed down, we set everything above 10M to 0:
		for (LengthCategory lengthCategory : FilePaddingLengthRandom.LengthCategory.values()) {
			if (lengthCategory.ordinal() > FilePaddingLengthRandom.LengthCategory._10M.ordinal())
				System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + lengthCategory.getConfigPropertyKey(), "0");
		}

		if (subShareServerTestSupport.beforeClass()) {
			// *IMPORTANT* We must *not* set any other dynamicX509TrustManagerCallbackClass!!! This setting is JVM-wide!
			// We run tests in parallel using multiple JVMs, though.
			cryptreeRepoTransportFactory = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(CryptreeRestRepoTransportFactoryImpl.class);
			cryptreeRepoTransportFactory.setDynamicX509TrustManagerCallbackClass(TestDynamicX509TrustManagerCallback.class);

			restPgpTransportFactory = PgpTransportFactoryRegistryImpl.getInstance().getPgpTransportFactoryOrFail(RestPgpTransportFactory.class);
			restPgpTransportFactory.setDynamicX509TrustManagerCallbackClass(TestDynamicX509TrustManagerCallback.class);

			restLockerTransportFactory = LockerTransportFactoryRegistry.getInstance().getLockerTransportFactory(RestLockerTransportFactory.class);
			restLockerTransportFactory.setDynamicX509TrustManagerCallbackClass(TestDynamicX509TrustManagerCallback.class);
		}

		for (File file : ConfigDir.getInstance().getFile().listFiles()) {
			file.deleteRecursively();
		}
	}

	@AfterClass
	public static void abstractIT_afterClass() throws Exception {
		for (LengthCategory lengthCategory : FilePaddingLengthRandom.LengthCategory.values())
			System.getProperties().remove(Config.SYSTEM_PROPERTY_PREFIX + lengthCategory.getConfigPropertyKey());

		if (subShareServerTestSupport.afterClass()) {
			CryptreeRestRepoTransportFactoryImpl f = cryptreeRepoTransportFactory;
			cryptreeRepoTransportFactory = null;
			if (f != null) {
				f.setDynamicX509TrustManagerCallbackClass(null);
//				f.setUserRepoKeyRingLookup(null);
			}

			RestPgpTransportFactory p = restPgpTransportFactory;
			restPgpTransportFactory = null;
			if (p != null)
				p.setDynamicX509TrustManagerCallbackClass(null);
		}
	}

	@Before
	public void before() throws Exception {
		before_pgpRegistry_clearCache();
		before_setupUserRegistryImplMockUp();
		before_deleteUserRegistryFile();
	}

	protected void before_pgpRegistry_clearCache() {
		PgpRegistry.getInstance().clearCache();
	}

	protected void after_pgpRegistry_clearCache() {
		PgpRegistry.getInstance().clearCache();
	}

	protected void before_setupUserRegistryImplMockUp() {
		// Make sure, we get a clean new instance for every test - not one that might already be initialised statically with the wrong directory + data. Skip, if sub-class already initialised!
		if (userRegistryImplMockUp == null) {
			final UserRegistry userRegistry = new UserRegistryImpl() {
				@Override
				protected void read() {
					// do nothing!
				}
			};
			userRegistryImplMockUp = new MockUp<UserRegistryImpl>() {
				@Mock
				UserRegistry getInstance() {
					return userRegistry;
				}
			};
		}
	}

	protected void before_deleteUserRegistryFile() {
		createFile(ConfigDir.getInstance().getFile(), UserRegistry.USER_REGISTRY_FILE_NAME).delete();
	}

	@After
	public void after() throws Exception {
		if (userRegistryImplMockUp != null) {
			userRegistryImplMockUp.tearDown(); // should be done automatically, but since we need to manage the reference, anyway, we do this explicitly here, too.
			userRegistryImplMockUp = null;
		}
		after_pgpRegistry_clearCache();
	}

	protected UserRepoKeyRing createUserRepoKeyRing(final UUID serverRepositoryId) {
		final UserRegistry userRegistry = new TestUserRegistry();
		final User user = userRegistry.getUsers().iterator().next();
		final UserRepoKeyRing userRepoKeyRing = user.getUserRepoKeyRingOrCreate();
		user.createUserRepoKey(serverRepositoryId);
		user.createUserRepoKey(serverRepositoryId);
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

	protected File createRelativeSymlink(final File symlink, final File target) throws IOException {
		assertThat(symlink.existsNoFollow()).isFalse();
		final File symlinkParent = symlink.getParentFile();

		final String relativeTargetString = symlinkParent.relativize(target);
		symlink.createSymbolicLink(relativeTargetString);

		assertThat(symlink.getAbsoluteFile()).isEqualTo(symlink.getAbsoluteFile());
		assertThat(symlink.existsNoFollow()).isTrue();
		assertThat(symlink.isSymbolicLink()).isTrue();
		addToFilesInRepo(symlink);
		return symlink;
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

	protected static boolean isServerThread() {
		final StackTraceElement[] stackTrace = new Exception().getStackTrace();
		for (final StackTraceElement stackTraceElement : stackTrace) {
			if ("org.eclipse.jetty.server.Server".equals(stackTraceElement.getClassName()))
				return true;
		}
		return false;
	}
}
