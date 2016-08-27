package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.net.URL;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.repo.ServerRepoManagerImpl;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.core.user.UserRepoInvitationToken;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoToRepoSync;
import co.codewizards.cloudstore.core.util.IOUtil;

public class InviteUserWithNormalAndMetaOnlyRepoIT extends AbstractUserRegistryIT {

	private static final Logger logger = LoggerFactory.getLogger(InviteUserWithNormalAndMetaOnlyRepoIT.class);

//	LocalRepoManager localDestLocalRepoManager;

//	@Override
//	@After
//	public void after() throws Exception {
//		super.after();
//		if (localDestLocalRepoManager != null) {
//			localDestLocalRepoManager.close();
//			localDestLocalRepoManager = null;
//		}
//	}

	@Test
	public void inviteUserAndSync_withMetaOnly_singleWritePermissionOnRoot() throws Exception {
		logger.info("*** >>>>>>>>>>>>>>>>>>>>>>>>> ***");
		logger.info("*** >>> inviteUserAndSync_withMetaOnly_singleWritePermissionOnRoot >>> ***");

		// *** OWNER machine with owner's repository ***
		switchLocationTo(TestUser.marco);

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();

//		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 0);
//		assertUserIdentityInRepoIs(localSrcRoot, 1);
//
//		assertUserIdentityInRepoIs(remoteRoot, 1);

		UserRepoInvitationToken userRepoInvitationToken = createUserRepoInvitationToken("", PermissionType.write, TestUser.khaled);

//		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 1);
//		assertUserIdentityInRepoIs(localSrcRoot, 2);

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

//		// Keep the LocalRepoManager open until @After to make sure that createFileWithRandomContent(...) works fine (I had *sometimes* test failures, otherwise).
//		localDestLocalRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localDestRoot);
		// not necessary, anymore, because done in super-class, already

		// Importing the invitation with the temporary key causes a permanent key to be generated and a request
		// to replace the temporary key by the permanent one.
//		assertInvitationUserRepoKeyPublicKeyInRepoIs(localDestRoot, 0);
//		assertReplacementRequestInRepoIs(localDestRoot, 0);
//		assertUserIdentityInRepoIs(localDestRoot, 0);

		importUserRepoInvitationToken(userRepoInvitationToken);

//		assertInvitationUserRepoKeyPublicKeyInRepoIs(localDestRoot, 1);
//		assertReplacementRequestInRepoIs(localDestRoot, 1);
//		assertReplacementRequestDeletionInRepoIs(localDestRoot, 0);
//		assertUserIdentityInRepoIs(localDestRoot, 0);

//		assertReplacementRequestInRepoIs(remoteRoot, 0);

		// The next sync is done with the temporary key (from the invitation). It downloads the repository and
		// uploads the permanent key with the replacement-request. It also already replaces the temporary key
		// by the permanent key.
		syncFromRemoteToLocalDest();

		File localDestMetaOnly = newTestRepositoryLocalRoot("local-dest-meta-only");
		localDestMetaOnly.mkdir();
		URL remoteRootURLForLocalDestMetaOnly = remoteRootURLWithPathPrefixForLocalDest;
		try (final LocalRepoManager localDestMetaOnlyRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForNewRepository(localDestMetaOnly);) {
			makeMetaOnly(localDestMetaOnly);
			ServerRepoManagerImpl.connectLocalRepositoryWithServerRepository(localDestMetaOnlyRepoManagerLocal, remoteRepositoryId, remoteRootURLForLocalDestMetaOnly);
		}

// TODO ENABLE THIS!
		// This sync cannot decrypt anything, yet.
		try (final RepoToRepoSync repoToRepoSync = createRepoToRepoSync(localDestMetaOnly, remoteRootURLForLocalDestMetaOnly);) {
			repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		}

		// We create a file in the destination repository which we should be able to sync back to the source repo.
		File destFile2xxxreverse = createFileWithRandomContent(localDestRoot, "2/xxxreverse");

//		// Make sure, our replacement request really arrived on the server.
//		assertInvitationUserRepoKeyPublicKeyInRepoIs(remoteRoot, 1);
//		assertReplacementRequestInRepoIs(remoteRoot, 1);
//		assertReplacementRequestDeletionInRepoIs(remoteRoot, 0);

		assertUserIdentityCountInRepoIs(localDestRoot, 3);
		assertUserIdentityLinkCountInRepoIs(localDestRoot, 5);
		assertUserIdentityCountInRepoIs(remoteRoot, 3);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 5);

		// Now we sync 'destFile2xxxreverse' up to the server.
		syncFromRemoteToLocalDest(false);

		assertUserIdentitiesNotReadable(localDestRoot);


		// *** OWNER machine with owner's repository ***
		switchLocationTo(TestUser.marco);

//		// ...but is not yet in source repo.
//		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 1);
//		assertReplacementRequestInRepoIs(localSrcRoot, 0);
//		assertReplacementRequestDeletionInRepoIs(localSrcRoot, 0);
//		assertUserIdentityInRepoIs(localSrcRoot, 2);

		File srcFile2xxxreverse = createFile(localSrcRoot, "2/xxxreverse");
		assertThat(srcFile2xxxreverse.exists()).isFalse(); // should not yet exist

		// It takes a different path, depending on whether there are file modifications or not (i.e. only meta-data changed).
		// This is not a nice way to test all possible paths, but the tests are already too slow ;-)
		File srcFile2ccc = random.nextInt(100) < 50 ? null : createFileWithRandomContent(localSrcRoot, "2/ccc");

		syncFromLocalSrcToRemote();

		assertThat(srcFile2xxxreverse.exists()).isTrue(); // now it should exist!
		assertThat(IOUtil.compareFiles(srcFile2xxxreverse, destFile2xxxreverse)).isTrue();

//		// ...and now it is already deleted and replaced by an appropriate deletion object.
//		assertInvitationUserRepoKeyPublicKeyInRepoIs(localSrcRoot, 0);
//		assertReplacementRequestInRepoIs(localSrcRoot, 0);
//		assertReplacementRequestDeletionInRepoIs(localSrcRoot, 1);
//		assertUserIdentityInRepoIs(localSrcRoot, 2); // the one from the invitation should be deleted => 2 instead of 3
//		assertUserIdentitiesReadable(localSrcRoot);


//		// the deletion should already be synced to the server
//		assertInvitationUserRepoKeyPublicKeyInRepoIs(remoteRoot, 0);
//		assertReplacementRequestInRepoIs(remoteRoot, 0);
//		assertReplacementRequestDeletionInRepoIs(remoteRoot, 1);
//		assertUserIdentityInRepoIs(remoteRoot, 2); // the one from the invitation should be deleted => 2 instead of 3



		// *** FRIEND machine with friend's repository ***
		switchLocationTo(TestUser.khaled);

		// This sync now can decrypt everything.
		try (final RepoToRepoSync repoToRepoSync = createRepoToRepoSync(localDestMetaOnly, remoteRootURLForLocalDestMetaOnly);) {
			repoToRepoSync.sync(new LoggerProgressMonitor(logger));
		}

		// check, whether the stuff is really there in the meta-only-repo
		try (final LocalRepoManager localDestMetaOnlyRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localDestMetaOnly);) {
			SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localDestMetaOnlyRepoManagerLocal.getLocalRepoMetaData();
			assertThat(localRepoMetaData.getRepoFileDto("/1 {11 11ä11#+} 1", 0)).isNotNull();
			assertThat(localRepoMetaData.getRepoFileDto("/1 {11 11ä11#+} 1/a", 0)).isNotNull();
			assertThat(localRepoMetaData.getRepoFileDto("/2/xxxreverse", 0)).isNotNull();
		}

		File destFile2ccc = srcFile2ccc == null ? null : createFile(localDestRoot, "2/ccc");
		if (destFile2ccc != null)
			assertThat(destFile2ccc.exists()).isFalse();

		syncFromRemoteToLocalDest();

//		assertInvitationUserRepoKeyPublicKeyInRepoIs(localDestRoot, 0);
//		assertReplacementRequestInRepoIs(localDestRoot, 0);
//		assertReplacementRequestDeletionInRepoIs(localDestRoot, 1);
//		assertUserIdentityInRepoIs(localDestRoot, 2); // the one from the invitation should be deleted => 2 instead of 3

//		assertUserIdentitiesNotReadable(localDestRoot);

		if (destFile2ccc != null) {
			assertThat(destFile2ccc.exists()).isTrue();
			assertThat(IOUtil.compareFiles(srcFile2ccc, destFile2ccc)).isTrue();
		}

		logger.info("*** <<< inviteUserAndSync_withMetaOnly_singleWritePermissionOnRoot <<< ***");
		logger.info("*** <<<<<<<<<<<<<<<<<<<<<<<<< ***");
	}

	private void makeMetaOnly(File localRoot) {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localRoot);) {
			SsLocalRepoMetaData localRepoMetaData = (SsLocalRepoMetaData) localRepoManager.getLocalRepoMetaData();
			localRepoMetaData.makeMetaOnly();
		}
	}
}
