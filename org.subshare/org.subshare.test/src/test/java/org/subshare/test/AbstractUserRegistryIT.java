package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.createFile;
import static co.codewizards.cloudstore.core.util.Util.doNothing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import mockit.Mock;
import mockit.MockUp;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.ReadUserIdentityAccessDeniedException;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.UserIdentityPayloadDto;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.gnupg.GnuPgDir;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRepoInvitationManager;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.persistence.SsRemoteRepository;
import org.subshare.local.persistence.UserIdentityLinkDao;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKeyDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.client.CloudStoreClient;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;

public class AbstractUserRegistryIT extends AbstractRepoToRepoSyncIT {

	private static final Logger logger = LoggerFactory.getLogger(AbstractUserRegistryIT.class);

	public static final String PUBRING_FILE_NAME = "pubring.gpg";
	public static final String SECRING_FILE_NAME = "secring.gpg";

	protected UserRegistry ownerUserRegistry;
	protected User owner;
	protected UserRegistry friendUserRegistry;
	protected User friend;
	protected UserRegistry userRegistry;

	@Override
	public void before() throws Exception {
		logger.info("*** >>>>>>>>>>>>>> ***");
		logger.info("*** >>> before >>> ***");

		new MockUp<UserRegistry>() {
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
		cryptreeRepoTransportFactory.setUserRepoKeyRing(owner.getUserRepoKeyRingOrCreate());
	}

	protected void switchLocationToFriend() throws Exception {
		userRegistry = friendUserRegistry;
		assignOwnerAndFriendFromCurrentUserRegistry();
		setupPgp("khaled", "test678");
		cryptreeRepoTransportFactory.setUserRepoKeyRing(friend.getUserRepoKeyRingOrCreate());
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
				cryptreeRepoTransportFactory.getUserRepoKeyRing());

		return cryptree;
	}

	protected User getFirstUserHavingPrivateKey(UserRegistry userRegistry) {
		final PgpRegistry pgpRegistry = PgpRegistry.getInstance();
		final Pgp pgp = pgpRegistry.getPgpOrFail();
		for (final User user : userRegistry.getUsers()) {
			for (final Long pgpKeyId : user.getPgpKeyIds()) {
				final PgpKey k = pgp.getPgpKey(pgpKeyId);
				if (k != null && k.isPrivateKeyAvailable()) {
					return user;
				}
			}
		}
		throw new IllegalStateException("There is no user having a private key!");
	}

	protected UserRegistry createUserRegistry(String ownerName, final String passphrase) throws Exception {
		setupPgp(ownerName, passphrase);

		UserRegistry userRegistry = new UserRegistry() { // protected constructor => subclass ;-)
		};
		return userRegistry;
	}

	protected void setupPgp(String ownerName, final String passphrase) throws Exception {
		logger.info("setupPgp: ownerName={}", ownerName);
		final String gpgDir = "gpg/" + ownerName;

		final File gnuPgDir = GnuPgDir.getInstance().getFile();
		gnuPgDir.mkdir();
		copyResource(gpgDir + '/' + PUBRING_FILE_NAME, createFile(gnuPgDir, PUBRING_FILE_NAME));
		copyResource(gpgDir + '/' + SECRING_FILE_NAME, createFile(gnuPgDir, SECRING_FILE_NAME));

		final PgpRegistry pgpRegistry = PgpRegistry.getInstance();

		pgpRegistry.setPgpAuthenticationCallback(new PgpAuthenticationCallback() {
			@Override
			public char[] getPassphrase(final PgpKey pgpKey) {
				return passphrase.toCharArray();
			}
		});

		pgpRegistry.clearCache();
	}

	protected UserRepoInvitationToken createUserRepoInvitationToken(final String localPath, PermissionType permissionType) {
		final UserRepoInvitationToken userRepoInvitationToken;
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);)
		{
			final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManager.Helper.getInstance(ownerUserRegistry, localRepoManager);
			userRepoInvitationToken = userRepoInvitationManager.createUserRepoInvitationToken(localPath, friend, permissionType, 24 * 3600 * 1000);
		}
		return userRepoInvitationToken;
	}

	protected void importUserRepoInvitationToken(UserRepoInvitationToken userRepoInvitationToken) throws Exception {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localDestRoot);)
		{
			final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManager.Helper.getInstance(friendUserRegistry, localRepoManager);
			userRepoInvitationManager.importUserRepoInvitationToken(userRepoInvitationToken);
		}

		// TODO should the UserRepoInvitationManager already do this? implicitly? or explicitly?
		new CloudStoreClient("requestRepoConnection", localDestRoot.getPath(), remoteRootURLWithPathPrefixForLocalDest.toExternalForm()).execute();
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

	private static void copyResource(final String sourceResName, final File destinationFile) throws IOException {
		logger.info("copyResource: sourceResName='{}' destinationFile='{}'", sourceResName, destinationFile);
		IOUtil.copyResource(InviteUserAndSyncIT.class, sourceResName, destinationFile);
	}

}