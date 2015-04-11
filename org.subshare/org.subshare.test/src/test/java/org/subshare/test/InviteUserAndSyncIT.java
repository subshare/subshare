package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.createFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.gnupg.GnuPgDir;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRepoInvitation;
import org.subshare.core.user.UserRepoInvitationManager;
import org.subshare.core.user.UserRepoKeyRing;
import org.junit.Test;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.util.IOUtil;

public class InviteUserAndSyncIT extends AbstractRepoToRepoSyncIT {

	public static final String PUBRING_FILE_NAME = "pubring.gpg";
	public static final String SECRING_FILE_NAME = "secring.gpg";

	private UserRegistry ownerUserRegistry;
	private User owner;
	private UserRegistry friendUserRegistry;
	private User friend;

	private UserRegistry userRegistry; // the currently used one


	@Override
	public void before() throws Exception {
		super.before();

		ownerUserRegistry = createUserRegistry("marco", "test12345");
		owner = getFirstUserHavingPrivateKey(ownerUserRegistry);

		friendUserRegistry = createUserRegistry("khaled", "test678");
		friend = getFirstUserHavingPrivateKey(friendUserRegistry);
	}

	@Test
	public void inviteUserAndSync() throws Exception {
		// *** OWNER machine with owner's repository ***
		switchLocationToOwner();

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();

		UserRepoInvitation userRepoInvitation = createUserRepoInvitation();

		// We *must* sync again, otherwise the invitation is meaningless: The invitation-UserRepoKey is not yet in the remote DB,
		// thus neither in the local destination and thus it cannot be used to decrypt the crypto-links.
		syncFromLocalSrcToRemote();

		// TODO this should be a serialized and encrypted token!

		// *** FRIEND machine with friend's repository ***
		switchLocationToFriend();
		// TODO create local repo, connect to server-repo using invitation-token and sync down!
		createLocalDestinationRepo();

//		createUserRepoKeyRing(remoteRepositoryId);
		importUserRepoInvitation(userRepoInvitation); // TODO this should cause the temporary invitation-UserRepoKey to be somehow replaced by a permanent one.

		syncFromRemoteToLocalDest();
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

	protected UserRepoInvitation createUserRepoInvitation() {
		final UserRepoInvitation userRepoInvitation;
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginWriteTransaction();)
			{
				final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
						transaction, remoteRepositoryId,
						remotePathPrefix2Encrypted,
						cryptreeRepoTransportFactory.getUserRepoKeyRing());

				final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManager.Helper.getInstance(ownerUserRegistry, cryptree);
				userRepoInvitation = userRepoInvitationManager.createUserRepoInvitation("", friend, 24 * 3600 * 1000);

				transaction.commit();
			}
		}
		return userRepoInvitation;
	}

	protected void importUserRepoInvitation(UserRepoInvitation userRepoInvitation) {
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localDestRoot);)
		{
			try (final LocalRepoTransaction transaction = localRepoManagerLocal.beginWriteTransaction();)
			{
				final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
						transaction, remoteRepositoryId,
						remotePathPrefix2Encrypted,
						cryptreeRepoTransportFactory.getUserRepoKeyRing());

				final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManager.Helper.getInstance(friendUserRegistry, cryptree);
				userRepoInvitationManager.importUserRepoInvitation(userRepoInvitation);

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

	@SuppressWarnings("deprecation")
	protected User getFirstUserHavingPrivateKey(UserRegistry userRegistry) {
		final PgpRegistry pgpRegistry = PgpRegistry.getInstance();
		pgpRegistry.clearCache();
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
		final String gpgDir = "gpg/" + ownerName;

		GnuPgDir.getInstance().getFile().mkdir();
		try (
				OutputStream out = createFile(GnuPgDir.getInstance().getFile(), PUBRING_FILE_NAME).createOutputStream();
				InputStream in = InviteUserAndSyncIT.class.getResourceAsStream(gpgDir + '/' + PUBRING_FILE_NAME);
				) {
			IOUtil.transferStreamData(in, out);
		}
		try (
				OutputStream out = createFile(GnuPgDir.getInstance().getFile(), SECRING_FILE_NAME).createOutputStream();
				InputStream in = InviteUserAndSyncIT.class.getResourceAsStream(gpgDir + '/' + SECRING_FILE_NAME);
				) {
			IOUtil.transferStreamData(in, out);
		}

		PgpRegistry.getInstance().setPgpAuthenticationCallback(new PgpAuthenticationCallback() {
			@Override
			public char[] getPassphrase(final PgpKey pgpKey) {
				return passphrase.toCharArray();
			}
		});
	}
}
