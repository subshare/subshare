package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.createFile;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.UUID;

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
import org.subshare.local.persistence.InvitationUserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKeyDao;
import org.subshare.local.persistence.UserRepoKeyPublicKeyReplacementRequestDao;
import org.subshare.local.persistence.UserRepoKeyPublicKeyReplacementRequestDeletionDao;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.util.IOUtil;

public class InviteUserAndSyncIT extends AbstractRepoToRepoSyncIT {

	private static final Logger logger = LoggerFactory.getLogger(InviteUserAndSyncIT.class);

	public static final String PUBRING_FILE_NAME = "pubring.gpg";
	public static final String SECRING_FILE_NAME = "secring.gpg";

	private UserRegistry ownerUserRegistry;
	private User owner;
	private UserRegistry friendUserRegistry;
	private User friend;

	private UserRegistry userRegistry; // the currently used one


	@Override
	public void before() throws Exception {
		logger.info("*** >>>>>>>>>>>>>> ***");
		logger.info("*** >>> before >>> ***");

		super.before();

		ownerUserRegistry = createUserRegistry("marco", "test12345");
		owner = getFirstUserHavingPrivateKey(ownerUserRegistry);

		friendUserRegistry = createUserRegistry("khaled", "test678");
		friend = getFirstUserHavingPrivateKey(friendUserRegistry);

		logger.info("*** <<< before <<< ***");
		logger.info("*** <<<<<<<<<<<<<< ***");
	}

	@Test
	public void inviteUserAndSync() throws Exception {
		logger.info("*** >>>>>>>>>>>>>>>>>>>>>>>>> ***");
		logger.info("*** >>> inviteUserAndSync >>> ***");

		// *** OWNER machine with owner's repository ***
		switchLocationToOwner();

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();

		assertInvitationUserRepoKeyPublicKeyIs(localSrcRoot, 0);

		UserRepoInvitationToken userRepoInvitationToken = createUserRepoInvitationToken();

		assertInvitationUserRepoKeyPublicKeyIs(localSrcRoot, 1);

		// We *must* sync again, otherwise the invitation is meaningless: The invitation-UserRepoKey is not yet in the remote DB,
		// thus neither in the local destination and thus it cannot be used to decrypt the crypto-links.
		syncFromLocalSrcToRemote();


		// *** FRIEND machine with friend's repository ***
		switchLocationToFriend();
		// TODO create local repo, connect to server-repo using invitation-token and sync down!
		createLocalDestinationRepo();

//		createUserRepoKeyRing(remoteRepositoryId);
		// Importing the invitation with the temporary key causes a permanent key to be generated and a request
		// to replace the temporary key by the permanent one.
		assertInvitationUserRepoKeyPublicKeyIs(localDestRoot, 0);
		assertReplacementRequestInRepoIs(localDestRoot, 0);

		importUserRepoInvitationToken(userRepoInvitationToken);

		assertInvitationUserRepoKeyPublicKeyIs(localDestRoot, 1);
		assertReplacementRequestInRepoIs(localDestRoot, 1);
		assertReplacementRequestDeletionInRepoIs(localDestRoot, 0);

		assertReplacementRequestInRepoIs(remoteRoot, 0);

		// The next sync is done with the temporary key (from the invitation). It downloads the repository and
		// uploads the permanent key with the replacement-request. It also already replaces the temporary key
		// by the permanent key.
		syncFromRemoteToLocalDest();

		// Make sure, our replacement request really arrived on the server.
		assertInvitationUserRepoKeyPublicKeyIs(remoteRoot, 1);
		assertReplacementRequestInRepoIs(remoteRoot, 1);
		assertReplacementRequestDeletionInRepoIs(remoteRoot, 0);


		// *** OWNER machine with owner's repository ***
		switchLocationToOwner();

		// ...but is not yet in source repo.
		assertInvitationUserRepoKeyPublicKeyIs(localSrcRoot, 1);
		assertReplacementRequestInRepoIs(localSrcRoot, 0);
		assertReplacementRequestDeletionInRepoIs(localSrcRoot, 0);

		// It takes a different path, depending on whether there file modifications  or not (i.e. only meta-data changed).
		// This is not a nice way to test all possible paths, but the tests are already too slow ;-)
		File srcFile2ccc = random.nextInt(100) < 50 ? null : createFileWithRandomContent(localSrcRoot, "2/ccc");

		syncFromLocalSrcToRemote();

		// ...and now it is already deleted and replaced by an appropriate deletion object.
		assertInvitationUserRepoKeyPublicKeyIs(localSrcRoot, 0);
		assertReplacementRequestInRepoIs(localSrcRoot, 0);
		assertReplacementRequestDeletionInRepoIs(localSrcRoot, 1);


		// the deletion should already be synced to the server
		assertInvitationUserRepoKeyPublicKeyIs(remoteRoot, 0);
		assertReplacementRequestInRepoIs(remoteRoot, 0);
		assertReplacementRequestDeletionInRepoIs(remoteRoot, 1);


		// *** FRIEND machine with friend's repository ***
		switchLocationToFriend();

		File destFile2ccc = srcFile2ccc == null ? null : createFile(localDestRoot, "2/ccc");
		if (destFile2ccc != null)
			assertThat(destFile2ccc.exists()).isFalse();

		syncFromRemoteToLocalDest();

		assertInvitationUserRepoKeyPublicKeyIs(localDestRoot, 0);
		assertReplacementRequestInRepoIs(localDestRoot, 0);
		assertReplacementRequestDeletionInRepoIs(localDestRoot, 1);

		if (destFile2ccc != null) {
			assertThat(destFile2ccc.exists()).isTrue();
			assertThat(IOUtil.compareFiles(srcFile2ccc, destFile2ccc)).isTrue();
		}

		logger.info("*** <<< inviteUserAndSync <<< ***");
		logger.info("*** <<<<<<<<<<<<<<<<<<<<<<<<< ***");
	}

	protected void switchLocationToOwner() throws Exception {
		userRegistry = ownerUserRegistry;
		setupPgp("marco", "test12345");
		cryptreeRepoTransportFactory.setUserRepoKeyRing(owner.getUserRepoKeyRingOrCreate());
	}

	protected void switchLocationToFriend() throws Exception {
		userRegistry = friendUserRegistry;
		setupPgp("khaled", "test678");
		cryptreeRepoTransportFactory.setUserRepoKeyRing(friend.getUserRepoKeyRingOrCreate());
	}

	protected UserRepoInvitationToken createUserRepoInvitationToken() {
		final UserRepoInvitationToken userRepoInvitationToken;
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);)
		{
			final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManager.Helper.getInstance(ownerUserRegistry, localRepoManager);
			userRepoInvitationToken = userRepoInvitationManager.createUserRepoInvitationToken("", friend, 24 * 3600 * 1000);
		}
		return userRepoInvitationToken;
	}

	protected void importUserRepoInvitationToken(UserRepoInvitationToken userRepoInvitationToken) {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localDestRoot);)
		{
			final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManager.Helper.getInstance(friendUserRegistry, localRepoManager);
			userRepoInvitationManager.importUserRepoInvitationToken(userRepoInvitationToken);
		}
	}

	protected void assertReplacementRequestInRepoIs(File repoRoot, long expectedCount) {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(repoRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();)
			{
				UserRepoKeyPublicKeyReplacementRequestDao dao = transaction.getDao(UserRepoKeyPublicKeyReplacementRequestDao.class);
				assertThat(dao.getObjectsCount()).isEqualTo(expectedCount);

				transaction.commit();
			}
		}
	}

	protected void assertReplacementRequestDeletionInRepoIs(File repoRoot, long expectedCount) {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(repoRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();)
			{
				UserRepoKeyPublicKeyReplacementRequestDeletionDao dao = transaction.getDao(UserRepoKeyPublicKeyReplacementRequestDeletionDao.class);
				assertThat(dao.getObjectsCount()).isEqualTo(expectedCount);

				transaction.commit();
			}
		}
	}

	protected void assertInvitationUserRepoKeyPublicKeyIs(File repoRoot, long expectedCount) {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(repoRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManager.beginReadTransaction();)
			{
				long count = 0;
				UserRepoKeyPublicKeyDao dao = transaction.getDao(UserRepoKeyPublicKeyDao.class);
				for (UserRepoKeyPublicKey k : dao.getObjects()) {
					if (k instanceof InvitationUserRepoKeyPublicKey)
						++count;
				}

				assertThat(count).isEqualTo(expectedCount);

				transaction.commit();
			}
		}
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

	private static void copyResource(final String sourceResName, final File destinationFile) throws IOException {
		logger.info("copyResource: sourceResName='{}' destinationFile='{}'", sourceResName, destinationFile);
		IOUtil.copyResource(InviteUserAndSyncIT.class, sourceResName, destinationFile);
	}
}
