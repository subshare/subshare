package org.subshare.test;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;
import static org.subshare.test.PgpTestUtil.*;

import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mockit.Mock;
import mockit.MockUp;

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
import org.subshare.local.persistence.SsRemoteRepository;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.UserIdentityLinkDao;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKeyDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;

public class AbstractUserRegistryIT extends AbstractRepoToRepoSyncIT {

	private static final Logger logger = LoggerFactory.getLogger(AbstractUserRegistryIT.class);

	protected UserRegistry ownerUserRegistry;
	protected User owner;
	protected UserRegistry friendUserRegistry;
	protected User friend;
	protected UserRegistry userRegistry;

	@Override
	public void before() throws Exception {
		logger.info("*** >>>>>>>>>>>>>> ***");
		logger.info("*** >>> before >>> ***");

		userRegistryImplMockUp = new MockUp<UserRegistryImpl>() {
		    @Mock
		    UserRegistry getInstance() {
		    	UserRegistry ur = userRegistry;

		    	String userRegistryName = null;

		    	if (ownerUserRegistry == ur)
		    		userRegistryName = "ownerUserRegistry";

		    	if (friendUserRegistry == ur)
		    		userRegistryName = "friendUserRegistry";

		    	logger.info("Mocked UserRegistry returning userRegistry: {} (={})", ur, userRegistryName);
		        return ur;
		    }
		};

		super.before();

		ownerUserRegistry = createUserRegistry("marco", "test12345");
		owner = getFirstUserHavingPrivateKey(ownerUserRegistry);

		friendUserRegistry = createUserRegistry("khaled", "test678");
		friend = getFirstUserHavingPrivateKey(friendUserRegistry);

		logger.info("*** <<< before <<< ***");
		logger.info("*** <<<<<<<<<<<<<< ***");
	}

	@Override
	public void after() throws Exception {
		super.after();
		final File configDir = ConfigDir.getInstance().getFile();
		configDir.deleteRecursively();
		configDir.mkdir();
	}

	protected void switchLocationToOwner() throws Exception {
		userRegistry = ownerUserRegistry;
		assignOwnerAndFriendFromCurrentUserRegistry();
		setupPgp("marco", "test12345");
		UserRepoKeyRingLookup.Helper.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(owner.getUserRepoKeyRingOrCreate()));
	}

	protected void switchLocationToFriend() throws Exception {
		userRegistry = friendUserRegistry;
		assignOwnerAndFriendFromCurrentUserRegistry();
		setupPgp("khaled", "test678");
		UserRepoKeyRingLookup.Helper.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(friend.getUserRepoKeyRingOrCreate()));
	}

	private void assignOwnerAndFriendFromCurrentUserRegistry() {
		Collection<User> users = userRegistry.getUsersByEmail(owner.getEmails().get(0));
		assertThat(users).hasSize(1);
		owner = users.iterator().next();

		users = userRegistry.getUsersByEmail(friend.getEmails().get(0));
		assertThat(users).hasSize(1);
		friend = users.iterator().next();
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

	protected UserRegistry createUserRegistry(String ownerName, final String passphrase) throws Exception {
		setupPgp(ownerName, passphrase);

		UserRegistry userRegistry = new UserRegistryImpl() { // protected constructor => subclass ;-)
		};
		return userRegistry;
	}

	protected UserRepoInvitationToken createUserRepoInvitationToken(final String localPath, PermissionType permissionType) {
		final UserRepoInvitationToken userRepoInvitationToken;
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);)
		{
			final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManager.Helper.getInstance(ownerUserRegistry, localRepoManager);
			final Set<PgpKey> userPgpKeys = null;
			userRepoInvitationToken = userRepoInvitationManager.createUserRepoInvitationToken(localPath, friend, userPgpKeys, permissionType, 24 * 3600 * 1000);
		}
		return userRepoInvitationToken;
	}

	protected void importUserRepoInvitationToken(UserRepoInvitationToken userRepoInvitationToken) throws Exception {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localDestRoot);)
		{
			final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManager.Helper.getInstance(friendUserRegistry, localRepoManager);
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
		final User user;
		if (userRegistry == ownerUserRegistry)
			user = owner;
		else if (userRegistry == friendUserRegistry)
			user = friend;
		else
			throw new IllegalStateException("userRegistry is neither ownerUserRegistry nor friendUserRegistry!");

		user.createUserRepoKey(serverRepositoryId);
		return user.getUserRepoKeyRing();
	}

	protected void assertUserIdentityInRepoIs(File repoRoot, long expectedCount) {
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
}
