package org.subshare.test;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;
import static org.subshare.test.PgpTestUtil.*;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.ReadUserIdentityAccessDeniedException;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.UserIdentityPayloadDto;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRegistryImpl;
import org.subshare.core.user.UserRepoInvitationManager;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.core.user.UserRepoKeyRingLookup;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.SsRemoteRepository;
import org.subshare.local.persistence.UserIdentityDao;
import org.subshare.local.persistence.UserIdentityLinkDao;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKeyDao;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import mockit.Mock;
import mockit.MockUp;

public abstract class AbstractUserRegistryIT extends AbstractRepoToRepoSyncIT {

	private static final Logger logger = LoggerFactory.getLogger(AbstractUserRegistryIT.class);

	protected User user;
	protected UserRegistry userRegistry;

	protected Map<TestUser, UserRegistry> testUser2UserRegistry = new HashMap<>();
	protected Map<UserRegistry, TestUser> userRegistry2TestUser = new IdentityHashMap<>();
	protected Map<TestUser, String> testUser2Email = new HashMap<>();

	protected Map<TestUser, TestUserEnv> testUser2TestUserEnv = new HashMap<>();

	protected static class TestUserEnv {
		public final File localDestRoot;
		public final LocalRepoManager localDestRepoManagerLocal;
		public final URL remoteRootURLWithPathPrefixForLocalDest;

		public TestUserEnv(File localDestRoot, LocalRepoManager localDestRepoManagerLocal, URL remoteRootURLWithPathPrefixForLocalDest) {
			this.localDestRoot = localDestRoot;
			this.localDestRepoManagerLocal = localDestRepoManagerLocal;
			this.remoteRootURLWithPathPrefixForLocalDest = remoteRootURLWithPathPrefixForLocalDest;
		}
	}

	@Override
	public void before() throws Exception {
		logger.info("*** >>>>>>>>>>>>>> ***");
		logger.info("*** >>> before >>> ***");

		userRegistryImplMockUp = new MockUp<UserRegistryImpl>() {
		    @Mock
		    UserRegistry getInstance() {
		    	UserRegistry ur = userRegistry;
		    	TestUser testUser = userRegistry2TestUser.get(ur);

		    	logger.info("Mocked UserRegistry returning userRegistry: {} (of {})", ur, testUser);
		        return ur;
		    }
		};

		super.before();

		for (final TestUser testUser : TestUser.values()) {
			final UserRegistry userRegistry = createUserRegistry(testUser);
			assertThat(userRegistry).isNotNull();
			final User user = getFirstUserHavingPrivateKey(userRegistry);
			assertThat(user).isNotNull();
			assertThat(user.getEmails()).isNotEmpty();
			final String email = user.getEmails().get(0);
			testUser2UserRegistry.put(testUser, userRegistry);
			userRegistry2TestUser.put(userRegistry, testUser);
			testUser2Email.put(testUser, email);
		}

		logger.info("*** <<< before <<< ***");
		logger.info("*** <<<<<<<<<<<<<< ***");
	}

	@Override
	public void after() throws Exception {
		for (TestUserEnv testUserEnv : testUser2TestUserEnv.values()) {
			if (testUserEnv.localDestRepoManagerLocal != null)
				testUserEnv.localDestRepoManagerLocal.close();

			if (this.localDestRepoManagerLocal == testUserEnv.localDestRepoManagerLocal)
				this.localDestRepoManagerLocal = null;
		}
		super.after();
		final File configDir = ConfigDir.getInstance().getFile();
		configDir.deleteRecursively();
		configDir.mkdir();
	}

	protected void switchLocationTo(final TestUser testUser) throws Exception {
		if (userRegistry != null) {
			TestUser tu = userRegistry2TestUser.get(userRegistry);
			if (tu != null)
				testUser2TestUserEnv.put(tu, new TestUserEnv(localDestRoot, localDestRepoManagerLocal, remoteRootURLWithPathPrefixForLocalDest));
		}

		userRegistry = testUser2UserRegistry.get(testUser);
		assertThat(userRegistry).isNotNull();
		setupPgp(testUser.name(), testUser.getPgpPrivateKeyPassword());

		user = getUser(testUser);
		UserRepoKeyRingLookup.Helper.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(user.getUserRepoKeyRingOrCreate()));

		if (TestUser.marco != testUser) { // the owner! always!
			final TestUserEnv testUserEnv = testUser2TestUserEnv.get(testUser);
			localDestRoot = testUserEnv == null ? null : testUserEnv.localDestRoot;
			localDestRepoManagerLocal = testUserEnv == null ? null : testUserEnv.localDestRepoManagerLocal;
			remoteRootURLWithPathPrefixForLocalDest = testUserEnv == null ? null : testUserEnv.remoteRootURLWithPathPrefixForLocalDest;
		}
	}

	protected User getUser(final TestUser testUser) {
		assertThat(userRegistry).isNotNull();

		final String email = testUser2Email.get(testUser);
		assertThat(email).isNotNull();

		final Collection<User> users = userRegistry.getUsersByEmail(email);
		assertThat(users).hasSize(1);
		final User user = users.iterator().next();
		assertThat(user).isNotNull();
		return user;
	}

	protected void assertUserIdentitiesReadable(File repoRoot) {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(repoRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();)
			{
				UserRepoKeyPublicKeyDao urkpkDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);
				Cryptree cryptree = getCryptree(transaction);

				for (UserRepoKeyPublicKey publicKey : urkpkDao.getObjects()) {
					UserIdentityPayloadDto userIdentityPayloadDto = cryptree.getUserIdentityPayloadDtoOrFail(publicKey.getUserRepoKeyId());
					logger.info("assertUserIdentitiesReadable: {}", userIdentityPayloadDto);
				}

				transaction.commit();
			}
		}
	}

	protected void assertUserIdentitiesNotReadable(File repoRoot) {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(repoRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();)
			{
				UserRepoKeyPublicKeyDao urkpkDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);
				Cryptree cryptree = getCryptree(transaction);

				for (UserRepoKeyPublicKey publicKey : urkpkDao.getObjects()) {
					if (user.getUserRepoKeyRing().getUserRepoKey(publicKey.getUserRepoKeyId()) != null)
						cryptree.getUserIdentityPayloadDtoOrFail(publicKey.getUserRepoKeyId()); // we must be able to decrypt our own identity!
					else
						try {
							UserIdentityPayloadDto userIdentityPayloadDto = cryptree.getUserIdentityPayloadDtoOrFail(publicKey.getUserRepoKeyId());
							fail(String.format("Succeeded to decrypt %s for userRepoKeyId=%s! The current user should not be able to do so!", userIdentityPayloadDto, publicKey.getUserRepoKeyId()));
						} catch (ReadUserIdentityAccessDeniedException x) {
							doNothing(); // this is exactly, what we expect ;-)
						}
				}

				transaction.commit();
			}
		}
	}

	private RemoteRepository getRemoteRepository(final LocalRepoTransaction transaction) {
		final RemoteRepositoryDao rrDao = transaction.getDao(RemoteRepositoryDao.class);
		final Collection<RemoteRepository> remoteRepositories = rrDao.getObjects();
		assertThat(remoteRepositories).hasSize(1);
		return remoteRepositories.iterator().next();
	}

	private Cryptree getCryptree(final LocalRepoTransaction transaction) {
		final RemoteRepository remoteRepository = getRemoteRepository(transaction);
		assertThat(remoteRepository.getRepositoryId()).isNotNull();
		assertThat(remoteRepository.getRemoteRoot()).isNotNull();

		final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
				transaction, remoteRepository.getRepositoryId(),
				((SsRemoteRepository)remoteRepository).getRemotePathPrefix(),
				getUserRepoKeyRing(cryptreeRepoTransportFactory));

		return cryptree;
	}

	protected User getFirstUserHavingPrivateKey(UserRegistry userRegistry) {
		final PgpRegistry pgpRegistry = PgpRegistry.getInstance();
		final Pgp pgp = pgpRegistry.getPgpOrFail();
		for (final User user : userRegistry.getUsers()) {
			for (final PgpKeyId pgpKeyId : user.getPgpKeyIds()) {
				final PgpKey k = pgp.getPgpKey(pgpKeyId);
				if (k != null && k.isSecretKeyAvailable()) {
					return user;
				}
			}
		}
		throw new IllegalStateException("There is no user having a private key!");
	}

	protected UserRegistry createUserRegistry(TestUser testUser) throws Exception {
		setupPgp(testUser.name(), testUser.getPgpPrivateKeyPassword());

		UserRegistry userRegistry = new UserRegistryImpl() {
			@Override
			protected void read() {
				// We don't read (but invoke callbacks and import PGP keys) ...
				preRead();
				markClean();
				postRead();
				readPgpUsers();
			}
			@Override
			protected synchronized void write() {
				// ... and we don't write!
				markClean();
			}
		};
		return userRegistry;
	}

	protected UserRepoInvitationToken createUserRepoInvitationToken(final String localPath, PermissionType permissionType, TestUser invitedTestUser) {
		final User invitedUser = getUser(invitedTestUser);
		final UserRepoInvitationToken userRepoInvitationToken;
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);)
		{
			final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManager.Helper.getInstance(userRegistry, localRepoManager);
			final Set<PgpKey> userPgpKeys = null;
			userRepoInvitationToken = userRepoInvitationManager.createUserRepoInvitationToken(localPath, invitedUser, userPgpKeys, permissionType, 24 * 3600 * 1000);
		}
		return userRepoInvitationToken;
	}

	protected void importUserRepoInvitationToken(UserRepoInvitationToken userRepoInvitationToken) throws Exception {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localDestRoot);)
		{
			final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManager.Helper.getInstance(userRegistry, localRepoManager);
			userRepoInvitationManager.importUserRepoInvitationToken(userRepoInvitationToken);
		}

		determineRemotePathPrefix2PlainAndEncrypted();
//		// TO DO should the UserRepoInvitationManager already do this? implicitly? or explicitly? ... ALREADY IMPLICITLY DONE (since 2015-06-23)
//		new CloudStoreClient("requestRepoConnection", localDestRoot.getPath(), remoteRootURLWithPathPrefixForLocalDest.toExternalForm()).execute();
	}

	@Override
	protected void determineRemotePathPrefix2Encrypted() {
		throw new UnsupportedOperationException(); // handled differently inside importUserRepoInvitationToken(...)
	}

	private void determineRemotePathPrefix2PlainAndEncrypted() throws Exception {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localDestRoot);)
		{
			Map<UUID, URL> remoteRepositoryId2RemoteRootMap = localRepoManager.getRemoteRepositoryId2RemoteRootMap();
			assertThat(remoteRepositoryId2RemoteRootMap).isNotEmpty();
			assertThat(remoteRepositoryId2RemoteRootMap).hasSize(1);

			UUID remoteRepositoryId = remoteRepositoryId2RemoteRootMap.keySet().iterator().next();
			URL remoteUrl = remoteRepositoryId2RemoteRootMap.values().iterator().next();
			String path = remoteUrl.toString();
			int index = path.indexOf(remoteRepositoryId.toString());
			assertThat(index).isGreaterThanOrEqualTo(0);
			path = path.substring(index + remoteRepositoryId.toString().length());
			remotePathPrefix2Encrypted = path;
		}

		if (remotePathPrefix2Encrypted.isEmpty())
			remotePathPrefix2Plain = "";
		else {
			String cryptoRepoFileIdStr = remotePathPrefix2Encrypted;
			int lastSlashIndex = cryptoRepoFileIdStr.lastIndexOf('/');
			assertThat(lastSlashIndex).isGreaterThanOrEqualTo(0);
			cryptoRepoFileIdStr = cryptoRepoFileIdStr.substring(lastSlashIndex + 1);
			Uid cryptoRepoFileId = new Uid(cryptoRepoFileIdStr);

			try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);)
			{
				try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginReadTransaction();)
				{
					final CryptoRepoFile cryptoRepoFile = transaction.getDao(CryptoRepoFileDao.class).getCryptoRepoFileOrFail(cryptoRepoFileId);
					remotePathPrefix2Plain = cryptoRepoFile.getLocalPathOrFail();
					transaction.commit();
				}
			}
		}

		remoteRootURLWithPathPrefixForLocalDest = getRemoteRootURLWithPathPrefixForLocalDest(remoteRepositoryId);
	}

	@Override
	protected UserRepoKeyRing createUserRepoKeyRing(UUID serverRepositoryId) {
		// super-method uses TestUserRegistry => override and use real UserRegistry here!
//		final User user;
//		if (userRegistry == ownerUserRegistry)
//			user = owner;
//		else if (userRegistry == friendUserRegistry)
//			user = friend;
//		else
//			throw new IllegalStateException("userRegistry is neither ownerUserRegistry nor friendUserRegistry!");

		assertThat(user).isNotNull();
		user.createUserRepoKey(serverRepositoryId);
		return user.getUserRepoKeyRing();
	}

	protected void assertUserIdentityLinkCountInRepoIs(File repoRoot, long expectedCount) {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(repoRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();)
			{
				UserIdentityLinkDao dao = transaction.getDao(UserIdentityLinkDao.class);
				assertThat(dao.getObjectsCount()).isEqualTo(expectedCount);

				transaction.commit();
			}
		}
	}

	protected void assertUserIdentityCountInRepoIs(File repoRoot, long expectedCount) {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(repoRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();)
			{
				UserIdentityDao dao = transaction.getDao(UserIdentityDao.class);
				assertThat(dao.getObjectsCount()).isEqualTo(expectedCount);

				transaction.commit();
			}
		}
	}
}
