package org.subshare.test;

import static co.codewizards.cloudstore.core.objectfactory.ObjectFactoryUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.assertj.core.api.Assertions.*;
import static org.subshare.test.PgpTestUtil.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.DebugUserRepoKeyDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.core.pgp.CreatePgpKeyParam.Algorithm;
import org.subshare.core.pgp.ImportKeysResult;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.PgpUserId;
import org.subshare.core.pgp.gnupg.BcWithLocalGnuPgPgp;
import org.subshare.core.pgp.gnupg.GnuPgDir;
import org.subshare.core.pgp.sync.PgpSync;
import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.ServerRepoManagerImpl;
import org.subshare.core.repo.ServerRepoRegistry;
import org.subshare.core.repo.ServerRepoRegistryImpl;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.core.repo.metaonly.MetaOnlyRepoManager;
import org.subshare.core.repo.metaonly.MetaOnlyRepoManagerImpl;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerRegistry;
import org.subshare.core.server.ServerRegistryImpl;
import org.subshare.core.sync.RepoSyncState;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRegistryImpl;
import org.subshare.core.user.UserRepoInvitationManager;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.core.user.UserRepoKeyRing;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.dto.Error;
import co.codewizards.cloudstore.core.dto.RemoteExceptionUtil;
import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.util.UrlUtil;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

public abstract class AbstractMultiUserIT extends AbstractIT {

	private static final Logger logger = LoggerFactory.getLogger(AbstractMultiUserIT.class);

	private TestUser testUser;
	private final List<MockUp<?>> mockUps = new ArrayList<>();

	private final Map<TestUser, String> testUser2ConfigDirString = new HashMap<>();
	private final Map<TestUser, File> testUser2ConfigDirFile = new HashMap<>();
	private final Map<TestUser, Config> testUser2Config = Collections.synchronizedMap(new HashMap<TestUser, Config>());
	private final Map<TestUser, UserRegistry> testUser2UserRegistry = Collections.synchronizedMap(new HashMap<TestUser, UserRegistry>());
	private final Map<TestUser, GnuPgDir> testUser2GnuPgDir = Collections.synchronizedMap(new HashMap<TestUser, GnuPgDir>());
	private final Map<TestUser, Pgp> testUser2Pgp = Collections.synchronizedMap(new HashMap<TestUser, Pgp>());
	private final Map<TestUser, ServerRegistry> testUser2ServerRegistry = Collections.synchronizedMap(new HashMap<TestUser, ServerRegistry>());
	private final Map<TestUser, ServerRepoRegistry> testUser2ServerRepoRegistry = Collections.synchronizedMap(new HashMap<TestUser, ServerRepoRegistry>());
	private final Map<TestUser, File> testUser2LocalRoot = new HashMap<>();
	private final Map<TestUser, PgpAuthenticationCallback> testUser2PgpAuthenticationCallback = Collections.synchronizedMap(new HashMap<TestUser, PgpAuthenticationCallback>());
	private final Map<TestUser, MetaOnlyRepoManager> testUser2MetaOnlyRepoManager = Collections.synchronizedMap(new HashMap<TestUser, MetaOnlyRepoManager>());

	private File remoteRoot;
	private UUID remoteRepositoryId;
	private TestUser ownerTestUser;

	@Override
	public void before() throws Exception {
		super.before();

		testUser = TestUser.server;
		ownerTestUser = null;

		createConfigDirFiles();

		mockUps.add(createConfigDirMockUp());
		mockUps.add(createConfigImplMockUp());
		mockUps.add(createUserRegistryImplMockUp());
		mockUps.add(createServerRegistryImplMockUp());
		mockUps.add(createServerRepoRegistryImplMockUp());
		mockUps.add(createGnuPgDirMockUp());
		mockUps.add(createPgpRegistryMockUp());
		mockUps.add(createMetaOnlyRepoManagerImplMockUp());

		setupPgps();
		testUser = TestUser.server;
	}

	private MockUp<ConfigDir> createConfigDirMockUp() {
		return new MockUp<ConfigDir>() {
			@Mock
			String getValue() {
				final String result = testUser2ConfigDirString.get(getTestUserOrServer());
				logger.info("MockUp<ConfigDir>.getValue: {}", result);
				assertThat(result).isNotNull();
				return result;
			}
			@Mock
			File getFile() {
				final File result = testUser2ConfigDirFile.get(getTestUserOrServer());
				logger.info("MockUp<ConfigDir>.getFile: {}", result);
				assertThat(result).isNotNull();
				return result;
			}
		};
	}

	private MockUp<ConfigImpl> createConfigImplMockUp() {
		return new MockUp<ConfigImpl>() {
			@Mock
			Config getInstance() {
				final TestUser testUser = getTestUserOrServer();
				logger.info("MockUp<ConfigImpl>.getInstance: testUser={}", testUser);
				synchronized (testUser2Config) {
					Config config = testUser2Config.get(testUser);
					if (config == null) {
						final File configDirFile = testUser2ConfigDirFile.get(testUser);
						assertThat(configDirFile).isNotNull();
						config = createObject(ConfigImpl.class, (ConfigImpl) null, (File) null,
								new File[] { configDirFile.createFile(Config.APP_ID_SIMPLE_ID + Config.PROPERTIES_FILE_NAME_SUFFIX) });
						testUser2Config.put(testUser, config);
					}
					return config;
				}
			}
		};
	}

	private MockUp<UserRegistryImpl> createUserRegistryImplMockUp() {
		return new MockUp<UserRegistryImpl>() {
			@Mock
			UserRegistry getInstance() {
				final TestUser testUser = getTestUserOrServer();
				logger.info("MockUp<UserRegistryImpl>.getInstance: testUser={}", testUser);
				synchronized (testUser2UserRegistry) {
					UserRegistry userRegistry = testUser2UserRegistry.get(testUser);
					if (userRegistry == null) {
						userRegistry = createObject(UserRegistryImpl.class);
						testUser2UserRegistry.put(testUser, userRegistry);
					}
					return userRegistry;
				}
			}
		};
	}

	private MockUp<ServerRegistryImpl> createServerRegistryImplMockUp() {
		return new MockUp<ServerRegistryImpl>() {
			@Mock
			ServerRegistry getInstance() {
				final TestUser testUser = getTestUserOrServer();
				logger.info("MockUp<ServerRegistryImpl>.getInstance: testUser={}", testUser);
				synchronized (testUser2ServerRegistry) {
					ServerRegistry registry = testUser2ServerRegistry.get(testUser);
					if (registry == null) {
						registry = createObject(ServerRegistryImpl.class);
						testUser2ServerRegistry.put(testUser, registry);
					}
					return registry;
				}
			}
		};
	}

	private MockUp<ServerRepoRegistryImpl> createServerRepoRegistryImplMockUp() {
		return new MockUp<ServerRepoRegistryImpl>() {
			@Mock
			ServerRepoRegistry getInstance() {
				final TestUser testUser = getTestUserOrServer();
				logger.info("MockUp<ServerRepoRegistryImpl>.getInstance: testUser={}", testUser);
				synchronized (testUser2ServerRepoRegistry) {
					ServerRepoRegistry registry = testUser2ServerRepoRegistry.get(testUser);
					if (registry == null) {
						registry = createObject(ServerRepoRegistryImpl.class);
						testUser2ServerRepoRegistry.put(testUser, registry);
					}
					return registry;
				}
			}
		};
	}

	private MockUp<GnuPgDir> createGnuPgDirMockUp() {
		return new MockUp<GnuPgDir>() {
			@Mock
			GnuPgDir getInstance() {
				final TestUser testUser = getTestUserOrServer();
				logger.info("MockUp<GnuPgDir>.getInstance: testUser={}", testUser);
				synchronized (testUser2GnuPgDir) {
					GnuPgDir gnuPgDir = testUser2GnuPgDir.get(testUser);
					if (gnuPgDir == null) {
						final File gnuPgDirFile = testUser2ConfigDirFile.get(testUser).getParentFile().createFile(".gnupg");
						gnuPgDir = new GnuPgDir() {
							@Override
							public File getFile() {
								return gnuPgDirFile;
							}
						};
						testUser2GnuPgDir.put(testUser, gnuPgDir);
					}
					return gnuPgDir;
				}
			}
		};
	}

	private MockUp<PgpRegistry> createPgpRegistryMockUp() {
		return new MockUp<PgpRegistry>() {
			@Mock
			Pgp getPgpOrFail(Invocation invocation) {
				final TestUser testUser = getTestUserOrServer();
				logger.info("MockUp<PgpRegistry>.getPgpOrFail: testUser={}", testUser);
				synchronized (testUser2Pgp) {
					Pgp pgp = testUser2Pgp.get(testUser);
					if (pgp == null) {
						pgp = createObject(BcWithLocalGnuPgPgp.class);
						testUser2Pgp.put(testUser, pgp);
					}
					logger.info("MockUp<PgpRegistry>.getPgpOrFail: {}@{}", pgp.getClass().getSimpleName(), Integer.toHexString(System.identityHashCode(pgp)));
					for (PgpKey pgpKey : pgp.getMasterKeys()) {
						logger.info("MockUp<PgpRegistry>.getPgpOrFail: * {}", pgpKey);
					}
					return pgp;
				}
			}

			@Mock
			PgpAuthenticationCallback getPgpAuthenticationCallback() {
				final TestUser testUser = getTestUserOrServer();
				logger.info("MockUp<PgpRegistry>.getPgpAuthenticationCallback: testUser={}", testUser);
				return testUser2PgpAuthenticationCallback.get(testUser);
			}

			@Mock
			void setPgpAuthenticationCallback(final PgpAuthenticationCallback pgpAuthenticationCallback) {
				final TestUser testUser = getTestUserOrServer();
				logger.info("MockUp<PgpRegistry>.setPgpAuthenticationCallback: testUser={}", testUser);
				testUser2PgpAuthenticationCallback.put(testUser, pgpAuthenticationCallback);
			}
		};
	}

	private MockUp<MetaOnlyRepoManagerImpl> createMetaOnlyRepoManagerImplMockUp() {
		return new MockUp<MetaOnlyRepoManagerImpl>() {
			@Mock
			MetaOnlyRepoManager getInstance() {
				final TestUser testUser = getTestUserOrServer();
				logger.info("MockUp<MetaOnlyRepoManagerImpl>.getInstance: testUser={}", testUser);
				synchronized (testUser2MetaOnlyRepoManager) {
					MetaOnlyRepoManager metaOnlyRepoManager = testUser2MetaOnlyRepoManager.get(testUser);
					if (metaOnlyRepoManager == null) {
						metaOnlyRepoManager = createObject(MetaOnlyRepoManagerImpl.class);
						testUser2MetaOnlyRepoManager.put(testUser, metaOnlyRepoManager);
					}
					return metaOnlyRepoManager;
				}
			}
		};
	}

	private void setupPgps() throws Exception {
		for (TestUser testUser : TestUser.values()) {
			if (TestUser.server != testUser) {
				switchLocationTo(testUser);
				setupPgp(testUser.name(), testUser.getPgpPrivateKeyPassword());
				logger.info("setupPgps: testUser={}", testUser);
				for (PgpKey pgpKey : getPgp().getMasterKeys()) {
					logger.info("setupPgps: * pgpKey={}", pgpKey);
				}
			}
		}
	}

	@Override
	public void after() throws Exception {
		while (! mockUps.isEmpty()) {
			MockUp<?> mockUp = mockUps.remove(mockUps.size() - 1);
			mockUp.tearDown();
		}
		testUser2MetaOnlyRepoManager.clear();
		testUser2PgpAuthenticationCallback.clear();
		testUser2Pgp.clear();
		testUser2GnuPgDir.clear();
		testUser2ServerRepoRegistry.clear();
		testUser2ServerRegistry.clear();
		testUser2UserRegistry.clear();
		testUser2Config.clear();
		testUser2ConfigDirFile.clear();
		testUser2ConfigDirString.clear();
		testUser = null;
		ownerTestUser = null;
		remoteRoot = null;
		remoteRepositoryId = null;
		super.after();
	}

	protected TestUser getTestUserOrServer() {
		TestUser tu = assertNotNull("testUser", testUser);
		if (isServerThread())
			tu = TestUser.server;
		return tu;
	}

	private void createConfigDirFiles() {
		for (TestUser testUser : TestUser.values())
			createConfigDirFile(testUser);
	}

	private void createConfigDirFile(TestUser testUser) {
		final String configDirString = jvmInstanceDir + '/' + testUser + "/.subshare";
		final File configDirFile = createFile(configDirString).getAbsoluteFile();
		configDirFile.deleteRecursively();
		configDirFile.mkdirs();
		testUser2ConfigDirString.put(testUser, configDirString);
		testUser2ConfigDirFile.put(testUser, configDirFile);
	}

	protected void switchLocationTo(final TestUser testUser) throws Exception {
		this.testUser = assertNotNull("testUser", testUser);
		System.out.println();
		logger.info("");
		logger.info("********************************************************************");
		logger.info("*** {} ***", testUser);
	}

	protected void createLocalSourceAndRemoteRepo() throws Exception {
		assertThat(ownerTestUser).isNull();
		assertThat(remoteRoot).isNull();
		assertThat(remoteRepositoryId).isNull();

		final TestUser testUser = ownerTestUser = getTestUserOrServer();
		final File localSrcRoot = newTestRepositoryLocalRoot("local-src-" + testUser.name());
		assertThat(localSrcRoot.exists()).isFalse();
		localSrcRoot.mkdirs();
		assertThat(localSrcRoot.isDirectory()).isTrue();
		testUser2LocalRoot.put(testUser, localSrcRoot);

		final Server server = getServerOrCreate();
		final User user = getUserOrCreate(testUser);
		final ServerRepo serverRepo = ServerRepoManagerImpl.getInstance().createRepository(localSrcRoot, server, user);
		remoteRepositoryId = serverRepo.getRepositoryId();

//		remoteRoot = newTestRepositoryLocalRoot("remote");
//		assertThat(remoteRoot.exists()).isFalse();
//		remoteRoot.mkdirs();
//		assertThat(remoteRoot.isDirectory()).isTrue();
//
//		try (final LocalRepoManager localLocalRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localSrcRoot)) {
//			try (final LocalRepoManager remoteLocalRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(remoteRoot)) {
//				remoteRepositoryId = remoteLocalRepoManager.getRepositoryId();
//				URL remoteRootURLWithPathPrefix = getRemoteRootURLWithPathPrefix();
//
////				ownerUserRepoKeyRing = createUserRepoKeyRing();
////				UserRepoKeyRingLookup.Helper.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(ownerUserRepoKeyRing));
//
//				new CloudStoreClient("requestRepoConnection", localSrcRoot.getAbsolutePath(), remoteRootURLWithPathPrefix.toExternalForm()).execute();
//				//	acceptRepoConnection is not needed, because already accepted implicitly by *signed* request
//			}
//		}
	}

	protected Server getServerOrCreate() throws Exception {
		final ServerRegistry serverRegistry = ServerRegistryImpl.getInstance();
		final URL serverUrl = new URL(getSecureUrl());
		Server server = serverRegistry.getServerForRemoteRoot(serverUrl);
		if (server == null) {
			server = serverRegistry.createServer();
			server.setUrl(serverUrl);
			server.setName(serverUrl.getHost() + ':' + serverUrl.getPort());
			serverRegistry.getServers().add(server);
			syncPgp(server);
		}
		return server;
	}

	protected void createLocalDestinationRepo(UserRepoInvitationToken userRepoInvitationToken) throws Exception {
		final TestUser testUser = getTestUserOrServer();
		final File localDestRoot = newTestRepositoryLocalRoot("local-dest-" + testUser.name());
		assertThat(localDestRoot.exists()).isFalse();
		localDestRoot.mkdirs();
		assertThat(localDestRoot.isDirectory()).isTrue();

		testUser2LocalRoot.put(testUser, localDestRoot);

		final UserRegistry userRegistry = UserRegistryImpl.getInstance();
//		final URL remoteRootURLWithPathPrefix = getRemoteRootURLWithPathPrefix();
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localDestRoot)) {
			final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManager.Helper.getInstance(userRegistry, localRepoManager);
			userRepoInvitationManager.importUserRepoInvitationToken(userRepoInvitationToken);

//			if (! getUserRepoKeyRing(cryptreeRepoTransportFactory).getUserRepoKeys(remoteRepositoryId).isEmpty()) {
//				new CloudStoreClient("requestRepoConnection", localDestRoot.getPath(), remoteRootURLWithPathPrefix.toExternalForm()).execute();
//				//	acceptRepoConnection is not needed, because already accepted implicitly by *signed* request
//			}
		}
	}

	protected void syncMetaOnlyRepos() throws Exception {
		List<RepoSyncState> repoSyncStates = MetaOnlyRepoManagerImpl.getInstance().sync();
		assertThat(repoSyncStates).isNotEmpty();
		for (RepoSyncState repoSyncState : repoSyncStates) {
			Error error = repoSyncState.getError();
			if (error != null)
				RemoteExceptionUtil.throwOriginalExceptionIfPossible(error);
		}
	}

	protected URL getRemoteRootURLBase() throws MalformedURLException {
		assertNotNull("remoteRepositoryId", remoteRepositoryId);
		return new URL(getSecureUrl() + "/" + remoteRepositoryId);
	}

	protected URL getRemoteRootURLWithPathPrefix() throws MalformedURLException {
//		final TestUser testUser = getTestUserOrServer(); // maybe needed later to determine path-prefix on partial check-out.
		String remotePathPrefix1Encrypted = ""; // TODO maybe add this later?!
		assertNotNull("remoteRepositoryId", remoteRepositoryId);
		return UrlUtil.appendNonEncodedPath(getRemoteRootURLBase(),  remotePathPrefix1Encrypted);
	}

	protected void populateLocalSourceRepo() throws Exception {
		final File localSrcRoot = testUser2LocalRoot.get(assertNotNull("ownerTestUser", ownerTestUser));
		assertNotNull("localSrcRoot", localSrcRoot);

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

		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot)) {
			localRepoManagerLocal.localSync(new LoggerProgressMonitor(logger));
		}
	}

	protected void syncLocalWithRemoteRepo() throws Exception {
		TestUser testUser = getTestUserOrServer();
		final File localRoot = assertNotNull("testUser2LocalRoot.get(" + testUser + ")", testUser2LocalRoot.get(testUser));
		try (final RepoToRepoSync repoToRepoSync = RepoToRepoSync.create(localRoot, getRemoteRootURLWithPathPrefix())) {
			repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		}
	}

	protected UserRepoInvitationToken createUserRepoInvitationToken(final String localPath, PermissionType permissionType, TestUser invitedTestUser) {
		final TestUser testUser = getTestUserOrServer();
		final User invitedUser = getUserOrCreate(invitedTestUser);
		final UserRepoInvitationToken userRepoInvitationToken;
		final File localRoot = assertNotNull("testUser2LocalRoot.get(" + testUser + ")", testUser2LocalRoot.get(testUser));
		final UserRegistry userRegistry = UserRegistryImpl.getInstance();
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);) {
			final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManager.Helper.getInstance(userRegistry, localRepoManager);
			final Set<PgpKey> userPgpKeys = null;
			userRepoInvitationToken = userRepoInvitationManager.createUserRepoInvitationToken(localPath, invitedUser, userPgpKeys, permissionType, 24 * 3600 * 1000);
		}
		return userRepoInvitationToken;
	}

	protected User getUserOrCreate(final TestUser testUser) {
		assertNotNull("testUser", testUser);
		UserRegistry userRegistry = UserRegistryImpl.getInstance();
		Collection<User> users = userRegistry.getUsersByEmail(testUser.getEmail());
		User user = null;
		if (! users.isEmpty())
			user = users.iterator().next();

		if (user == null) {
			user = userRegistry.createUser();
			user.setFirstName(testUser.name().substring(0, 1).toUpperCase() + testUser.name().substring(1));
			user.getEmails().add(testUser.getEmail());
			userRegistry.addUser(user);
		}

		for (PgpKey pgpKey : getPgp().getMasterKeys()) {
			if (user.getPgpKeyIds().contains(pgpKey.getPgpKeyId()))
				continue;

			for (String pgpUserId : pgpKey.getUserIds()) {
				if (pgpUserId.contains(testUser.getEmail())) {
					user.getPgpKeyIds().add(pgpKey.getPgpKeyId());
					break;
				}
			}
		}

		return user;
	}

	protected List<PgpKey> importKeys(byte[] pgpKeyData) {
		final Pgp pgp = getPgp();
		final ImportKeysResult importKeysResult = pgp.importKeys(new ByteArrayInputStream(pgpKeyData));
		final List<PgpKey> result = new ArrayList<>(importKeysResult.getPgpKeyId2ImportedMasterKey().size());
		for (PgpKeyId pgpKeyId : importKeysResult.getPgpKeyId2ImportedMasterKey().keySet()) {
			PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
			result.add(assertNotNull("pgpKey", pgpKey));
		}
		return result;
	}

	protected PgpKey createPgpKey() {
		final TestUser testUser = getTestUserOrServer();
		final User user = getUserOrCreate(testUser);
		final CreatePgpKeyParam createPgpKeyParam = new CreatePgpKeyParam();
		createPgpKeyParam.setAlgorithm(Algorithm.RSA);
		createPgpKeyParam.setStrength(min(Algorithm.RSA.getSupportedStrengths())); // shorter key is faster. not used in production, anyway.
		createPgpKeyParam.setPassphrase(testUser.getPgpPrivateKeyPassword().toCharArray());
		for (final String email : user.getEmails()) {
			createPgpKeyParam.getUserIds().add(new PgpUserId(email));
		}
		final PgpKey pgpKey = getPgp().createPgpKey(createPgpKeyParam);
		user.getPgpKeyIds().add(pgpKey.getPgpKeyId());
		logger.info("createPgpKey: pgpKey={}, user={}", pgpKey, user);
		return pgpKey;
	}

	protected Collection<DebugUserRepoKeyDto> getDebugUserRepoKeyDtos() {
		final TestUser testUser = getTestUserOrServer();
		final File localRoot = assertNotNull("testUser2LocalRoot.get(" + testUser + ")", testUser2LocalRoot.get(testUser));
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot)) {
			SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
			Collection<DebugUserRepoKeyDto> debugUserRepoKeyDtos = localRepoMetaData.getDebugUserRepoKeyDtos();
			return debugUserRepoKeyDtos;
		}
	}

	protected Pgp getPgp() {
		return PgpRegistry.getInstance().getPgpOrFail();
	}

	private static int min(List<Integer> values) {
		int result = Integer.MAX_VALUE;
		for (int v : values) {
			if (result > v)
				result = v;
		}
		return result;
	}

	protected void syncPgp() {
		List<Server> servers = ServerRegistryImpl.getInstance().getServers();
		for (Server server : servers)
			syncPgp(server);
	}

	protected void syncPgp(Server server) {
		try (PgpSync pgpSync = new PgpSync(server);) {
			pgpSync.sync();
		}
	}

	@Override
	protected void before_pgpRegistry_clearCache() {
		// not needed! we create a new instance each time.
	}
	@Override
	protected void after_pgpRegistry_clearCache() {
		// not needed! we create a new instance each time.
	}

	@Override
	protected void before_setupUserRegistryImplMockUp() {
		// mocked in our sub-class' before()!
	}

	@Override
	protected void before_deleteUserRegistryFile() {
		// not needed! we delete and recreate the entire config-dir.
	}

	@Override
	protected UserRepoKeyRing createUserRepoKeyRing(final UUID serverRepositoryId) {
		throw new UnsupportedOperationException("Use UserRegistryImpl.getInstance() instead!");
	}
}
