package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.local.persistence.InvitationUserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKeyDao;
import org.subshare.local.persistence.UserRepoKeyPublicKeyReplacementRequestDao;
import org.subshare.local.persistence.UserRepoKeyPublicKeyReplacementRequestDeletionDao;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.util.IOUtil;

public class InviteUserAndSyncIT extends AbstractUserRegistryIT {

	private static final Logger logger = LoggerFactory.getLogger(InviteUserAndSyncIT.class);

	@Test
	public void inviteUserAndSync_singleWritePermissionOnRoot() throws Exception {
		logger.info("*** >>>>>>>>>>>>>>>>>>>>>>>>> ***");
		logger.info("*** >>> inviteUserAndSync_singleWritePermissionOnRoot >>> ***");

		// *** OWNER machine with owner's repository ***
		switchLocationTo(TestUser.marco);

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
//		determineRemotePathPrefix2Encrypted(); // handled differently inside importUserRepoInvitationToken(...)

		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 0);

		assertUserIdentityCountInRepoIs(localSrcRoot, 1);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 1);

		assertUserIdentityCountInRepoIs(remoteRoot, 1);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 1);

		UserRepoInvitationToken userRepoInvitationToken = createUserRepoInvitationToken("", PermissionType.write, TestUser.khaled);

		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 1);
		assertUserIdentityCountInRepoIs(localSrcRoot, 2);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 3); // it's allowed to read itself, even without permissions!

		// We *must* sync again, otherwise the invitation is meaningless: The invitation-UserRepoKey is not yet in the remote DB,
		// thus neither in the local destination and thus it cannot be used to decrypt the crypto-links.
		syncFromLocalSrcToRemote();

		assertUserIdentityCountInRepoIs(remoteRoot, 2);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 3);
		assertUserIdentityCountInRepoIs(localSrcRoot, 2);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 3);
		assertUserIdentitiesReadable(localSrcRoot);


		// *** FRIEND machine with friend's repository ***
		switchLocationTo(TestUser.khaled);
		// create local repo, then connect to server-repo using invitation-token and sync down!
		createLocalDestinationRepo();

//		createUserRepoKeyRing(remoteRepositoryId);
		// Importing the invitation with the temporary key causes a permanent key to be generated and a request
		// to replace the temporary key by the permanent one.
		assertInvitationUserRepoKeyPublicKeyInRepoIs(localDestRoot, 0);
		assertReplacementRequestInRepoIs(localDestRoot, 0);
		assertUserIdentityLinkCountInRepoIs(localDestRoot, 0);

		importUserRepoInvitationToken(userRepoInvitationToken);

		assertInvitationUserRepoKeyPublicKeyInRepoIs(localDestRoot, 1);
		assertReplacementRequestInRepoIs(localDestRoot, 1);
		assertReplacementRequestDeletionInRepoIs(localDestRoot, 0);
		assertUserIdentityCountInRepoIs(localDestRoot, 0);
		assertUserIdentityLinkCountInRepoIs(localDestRoot, 0);

		assertReplacementRequestInRepoIs(remoteRoot, 0);

		// The next sync is done with the temporary key (from the invitation). It downloads the repository and
		// uploads the permanent key with the replacement-request. It also already replaces the temporary key
		// by the permanent key.
		syncFromRemoteToLocalDest();

		// We create a file in the destination repository which we should be able to sync back to the source repo.
		File destFile2xxxreverse = createFileWithRandomContent(localDestRoot, "2/xxxreverse");

		// Make sure, our replacement request really arrived on the server.
		assertInvitationUserRepoKeyPublicKeyInRepoIs(remoteRoot, 1);
		assertReplacementRequestInRepoIs(remoteRoot, 1);
		assertReplacementRequestDeletionInRepoIs(remoteRoot, 0);

		assertUserIdentityCountInRepoIs(localDestRoot, 3);
		assertUserIdentityLinkCountInRepoIs(localDestRoot, 5);
		assertUserIdentityCountInRepoIs(remoteRoot, 3);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 5);

		// Now we sync 'destFile2xxxreverse' up to the server.
		syncFromRemoteToLocalDest(false);

		assertUserIdentitiesNotReadable(localDestRoot);


		// *** OWNER machine with owner's repository ***
		switchLocationTo(TestUser.marco);

		// ...but is not yet in source repo.
		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 1);
		assertReplacementRequestInRepoIs(localSrcRoot, 0);
		assertReplacementRequestDeletionInRepoIs(localSrcRoot, 0);
		assertUserIdentityCountInRepoIs(localSrcRoot, 2);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 3);

		File srcFile2xxxreverse = createFile(localSrcRoot, "2/xxxreverse");
		assertThat(srcFile2xxxreverse.exists()).isFalse(); // should not yet exist

		// It takes a different path, depending on whether there are file modifications or not (i.e. only meta-data changed).
		// This is not a nice way to test all possible paths, but the tests are already too slow ;-)
		File srcFile2ccc = random.nextInt(100) < 50 ? null : createFileWithRandomContent(localSrcRoot, "2/ccc");

		syncFromLocalSrcToRemote();

		assertThat(srcFile2xxxreverse.exists()).isTrue(); // now it should exist!
		assertThat(IOUtil.compareFiles(srcFile2xxxreverse, destFile2xxxreverse)).isTrue();

		// ...and now it is already deleted and replaced by an appropriate deletion object.
		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 0);
		assertReplacementRequestInRepoIs(localSrcRoot, 0);
		assertReplacementRequestDeletionInRepoIs(localSrcRoot, 1);
		assertUserIdentityCountInRepoIs(localSrcRoot, 2); // the one from the invitation should be deleted => 2 instead of 3
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 3);
		assertUserIdentitiesReadable(localSrcRoot);


		// the deletion should already be synced to the server
		assertInvitationUserRepoKeyPublicKeyInRepoIs(remoteRoot, 0);
		assertReplacementRequestInRepoIs(remoteRoot, 0);
		assertReplacementRequestDeletionInRepoIs(remoteRoot, 1);
		assertUserIdentityCountInRepoIs(remoteRoot, 2); // the one from the invitation should be deleted => 2 instead of 3
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 3);



		// *** FRIEND machine with friend's repository ***
		switchLocationTo(TestUser.khaled);

		File destFile2ccc = srcFile2ccc == null ? null : createFile(localDestRoot, "2/ccc");
		if (destFile2ccc != null)
			assertThat(destFile2ccc.exists()).isFalse();

		syncFromRemoteToLocalDest();

		assertInvitationUserRepoKeyPublicKeyInRepoIs(localDestRoot, 0);
		assertReplacementRequestInRepoIs(localDestRoot, 0);
		assertReplacementRequestDeletionInRepoIs(localDestRoot, 1);
		assertUserIdentityCountInRepoIs(localDestRoot, 2); // the one from the invitation should be deleted => 2 instead of 3
		assertUserIdentityLinkCountInRepoIs(localDestRoot, 3);

		assertUserIdentitiesNotReadable(localDestRoot);

		if (destFile2ccc != null) {
			assertThat(destFile2ccc.exists()).isTrue();
			assertThat(IOUtil.compareFiles(srcFile2ccc, destFile2ccc)).isTrue();
		}

		logger.info("*** <<< inviteUserAndSync_singleWritePermissionOnRoot <<< ***");
		logger.info("*** <<<<<<<<<<<<<<<<<<<<<<<<< ***");
	}


	@Test
	public void inviteUserAndSync_singleReadPermissionOnRoot() throws Exception {
		logger.info("*** >>>>>>>>>>>>>>>>>>>>>>>>> ***");
		logger.info("*** >>> inviteUserAndSync_singleReadPermissionOnRoot >>> ***");

		// *** OWNER machine with owner's repository ***
		switchLocationTo(TestUser.marco);

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
//		determineRemotePathPrefix2Encrypted(); // handled differently inside importUserRepoInvitationToken(...)

		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 0);
		assertUserIdentityCountInRepoIs(localSrcRoot, 1);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 1);

		assertUserIdentityCountInRepoIs(remoteRoot, 1);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 1);

		UserRepoInvitationToken userRepoInvitationToken = createUserRepoInvitationToken("", PermissionType.read, TestUser.khaled);

		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 1);
		assertUserIdentityCountInRepoIs(localSrcRoot, 2);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 3); // it's allowed to read itself, even without permissions!

		// We *must* sync again, otherwise the invitation is meaningless: The invitation-UserRepoKey is not yet in the remote DB,
		// thus neither in the local destination and thus it cannot be used to decrypt the crypto-links.
		syncFromLocalSrcToRemote();

		assertUserIdentityLinkCountInRepoIs(remoteRoot, 3);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 3);
		assertUserIdentitiesReadable(localSrcRoot);


		// *** FRIEND machine with friend's repository ***
		switchLocationTo(TestUser.khaled);
		// create local repo, then connect to server-repo using invitation-token and sync down!
		createLocalDestinationRepo();

//		createUserRepoKeyRing(remoteRepositoryId);
		// Importing the invitation with the temporary key causes a permanent key to be generated and a request
		// to replace the temporary key by the permanent one.
		assertInvitationUserRepoKeyPublicKeyInRepoIs(localDestRoot, 0);
		assertReplacementRequestInRepoIs(localDestRoot, 0);
		assertUserIdentityLinkCountInRepoIs(localDestRoot, 0);

		importUserRepoInvitationToken(userRepoInvitationToken);

		assertInvitationUserRepoKeyPublicKeyInRepoIs(localDestRoot, 1);
		assertReplacementRequestInRepoIs(localDestRoot, 1);
		assertReplacementRequestDeletionInRepoIs(localDestRoot, 0);

		assertReplacementRequestInRepoIs(remoteRoot, 0);

		// The next sync is done with the temporary key (from the invitation). It downloads the repository and
		// uploads the permanent key with the replacement-request. It also already replaces the temporary key
		// by the permanent key.
		syncFromRemoteToLocalDest();

		// Make sure, our replacement request really arrived on the server.
		assertInvitationUserRepoKeyPublicKeyInRepoIs(remoteRoot, 1);
		assertReplacementRequestInRepoIs(remoteRoot, 1);
		assertReplacementRequestDeletionInRepoIs(remoteRoot, 0);

		assertUserIdentityCountInRepoIs(localDestRoot, 3);
		assertUserIdentityLinkCountInRepoIs(localDestRoot, 5);
		assertUserIdentityCountInRepoIs(remoteRoot, 3);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 5);
		assertUserIdentitiesNotReadable(localDestRoot);


		// *** OWNER machine with owner's repository ***
		switchLocationTo(TestUser.marco);

		// ...but is not yet in source repo.
		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 1);
		assertReplacementRequestInRepoIs(localSrcRoot, 0);
		assertReplacementRequestDeletionInRepoIs(localSrcRoot, 0);
		assertUserIdentityCountInRepoIs(localSrcRoot, 2);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 3);

		// It takes a different path, depending on whether there are file modifications or not (i.e. only meta-data changed).
		// This is not a nice way to test all possible paths, but the tests are already too slow ;-)
		File srcFile2ccc = random.nextInt(100) < 50 ? null : createFileWithRandomContent(localSrcRoot, "2/ccc");

		syncFromLocalSrcToRemote();

		// ...and now it is already deleted and replaced by an appropriate deletion object.
		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 0);
		assertReplacementRequestInRepoIs(localSrcRoot, 0);
		assertReplacementRequestDeletionInRepoIs(localSrcRoot, 1);
		assertUserIdentityCountInRepoIs(localSrcRoot, 2); // the one from the invitation should be deleted => 2 instead of 3
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 3);
		assertUserIdentitiesReadable(localSrcRoot);


		// the deletion should already be synced to the server
		assertInvitationUserRepoKeyPublicKeyInRepoIs(remoteRoot, 0);
		assertReplacementRequestInRepoIs(remoteRoot, 0);
		assertReplacementRequestDeletionInRepoIs(remoteRoot, 1);
		assertUserIdentityCountInRepoIs(remoteRoot, 2); // the one from the invitation should be deleted => 2 instead of 3
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 3);


		// *** FRIEND machine with friend's repository ***
		switchLocationTo(TestUser.khaled);

		File destFile2ccc = srcFile2ccc == null ? null : createFile(localDestRoot, "2/ccc");
		if (destFile2ccc != null)
			assertThat(destFile2ccc.exists()).isFalse();

		syncFromRemoteToLocalDest();

		assertInvitationUserRepoKeyPublicKeyInRepoIs(localDestRoot, 0);
		assertReplacementRequestInRepoIs(localDestRoot, 0);
		assertReplacementRequestDeletionInRepoIs(localDestRoot, 1);
		assertUserIdentityCountInRepoIs(localDestRoot, 2); // the one from the invitation should be deleted => 2 instead of 3
		assertUserIdentityLinkCountInRepoIs(localDestRoot, 3);

		assertUserIdentitiesNotReadable(localDestRoot);

		if (destFile2ccc != null) {
			assertThat(destFile2ccc.exists()).isTrue();
			assertThat(IOUtil.compareFiles(srcFile2ccc, destFile2ccc)).isTrue();
		}

		logger.info("*** <<< inviteUserAndSync_singleReadPermissionOnRoot <<< ***");
		logger.info("*** <<<<<<<<<<<<<<<<<<<<<<<<< ***");
	}


// TODO For this test, I first have to refactor the way the repo-connection is established: (1) It should connect
// to the URL from the invitation-token. (2) The request-accept-connection-handling from CloudStore should be replaced
// by a Subshare-specific handshake mechanism that allows everyone having read access (=> use UserRepoKeyPublicKey-based signature/encryption)
// to connect.
//	@Test
//	public void inviteUserAndSync_twoReadPermissionsOnSubdirs() throws Exception {


	@Test
	public void inviteUserAndSync_singleReadPermissionOnSubdir() throws Exception {
		logger.info("*** >>>>>>>>>>>>>>>>>>>>>>>>> ***");
		logger.info("*** >>> inviteUserAndSync_singleReadPermissionOnSubdir >>> ***");

		// *** OWNER machine with owner's repository ***
		switchLocationTo(TestUser.marco);

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
//		determineRemotePathPrefix2Encrypted(); // handled differently inside importUserRepoInvitationToken(...)

		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 0);

		UserRepoInvitationToken userRepoInvitationToken = createUserRepoInvitationToken("1 {11 11ä11} 1", PermissionType.read, TestUser.khaled);

		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 1);

		// We *must* sync again, otherwise the invitation is meaningless: The invitation-UserRepoKey is not yet in the remote DB,
		// thus neither in the local destination and thus it cannot be used to decrypt the crypto-links.
		syncFromLocalSrcToRemote();


		// *** FRIEND machine with friend's repository ***
		switchLocationTo(TestUser.khaled);
		// TODO create local repo, connect to server-repo using invitation-token and sync down!
		createLocalDestinationRepo();

//		createUserRepoKeyRing(remoteRepositoryId);
		// Importing the invitation with the temporary key causes a permanent key to be generated and a request
		// to replace the temporary key by the permanent one.
		assertInvitationUserRepoKeyPublicKeyInRepoIs(localDestRoot, 0);
		assertReplacementRequestInRepoIs(localDestRoot, 0);

		importUserRepoInvitationToken(userRepoInvitationToken);

		assertInvitationUserRepoKeyPublicKeyInRepoIs(localDestRoot, 1);
		assertReplacementRequestInRepoIs(localDestRoot, 1);
		assertReplacementRequestDeletionInRepoIs(localDestRoot, 0);

		assertReplacementRequestInRepoIs(remoteRoot, 0);

		// The next sync is done with the temporary key (from the invitation). It downloads the repository and
		// uploads the permanent key with the replacement-request. It also already replaces the temporary key
		// by the permanent key.
		syncFromRemoteToLocalDest();

		// Make sure, our replacement request really arrived on the server.
		assertInvitationUserRepoKeyPublicKeyInRepoIs(remoteRoot, 1);
		assertReplacementRequestInRepoIs(remoteRoot, 1);
		assertReplacementRequestDeletionInRepoIs(remoteRoot, 0);


		// *** OWNER machine with owner's repository ***
		switchLocationTo(TestUser.marco);

		// ...but is not yet in source repo.
		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 1);
		assertReplacementRequestInRepoIs(localSrcRoot, 0);
		assertReplacementRequestDeletionInRepoIs(localSrcRoot, 0);

//		// It takes a different path, depending on whether there are file modifications or not (i.e. only meta-data changed).
//		// This is not a nice way to test all possible paths, but the tests are already too slow ;-)
//		File srcFile2ccc = random.nextInt(100) < 50 ? null : createFileWithRandomContent(localSrcRoot, "2/ccc");

		syncFromLocalSrcToRemote();

		// ...and now it is already deleted and replaced by an appropriate deletion object.
		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 0);
		assertReplacementRequestInRepoIs(localSrcRoot, 0);
		assertReplacementRequestDeletionInRepoIs(localSrcRoot, 1);


		// the deletion should already be synced to the server
		assertInvitationUserRepoKeyPublicKeyInRepoIs(remoteRoot, 0);
		assertReplacementRequestInRepoIs(remoteRoot, 0);
		assertReplacementRequestDeletionInRepoIs(remoteRoot, 1);


		// *** FRIEND machine with friend's repository ***
		switchLocationTo(TestUser.khaled);

//		File destFile2ccc = srcFile2ccc == null ? null : createFile(localDestRoot, "2/ccc");
//		if (destFile2ccc != null)
//			assertThat(destFile2ccc.exists()).isFalse();

		syncFromRemoteToLocalDest();

		assertInvitationUserRepoKeyPublicKeyInRepoIs(localDestRoot, 0);
		assertReplacementRequestInRepoIs(localDestRoot, 0);
		assertReplacementRequestDeletionInRepoIs(localDestRoot, 1);

//		if (destFile2ccc != null) {
//			assertThat(destFile2ccc.exists()).isTrue();
//			assertThat(IOUtil.compareFiles(srcFile2ccc, destFile2ccc)).isTrue();
//		}

		logger.info("*** <<< inviteUserAndSync_singleReadPermissionOnSubdir <<< ***");
		logger.info("*** <<<<<<<<<<<<<<<<<<<<<<<<< ***");
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

	protected void assertInvitationUserRepoKeyPublicKeyInRepoIs(File repoRoot, long expectedCount) {
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
}
