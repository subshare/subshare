package org.subshare.test;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.Test;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.core.user.UserRepoKey.PublicKeyWithSignature;

public class Issue5IT extends AbstractUserRegistryIT {

	@Test
	public void issue_5_inviteMultipleFriends() throws Exception {
		// *** OWNER machine with owner's repository ***
		switchLocationTo(TestUser.marco);

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();

		assertUserIdentityCountInRepoIs(localSrcRoot, 1);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 1);
		assertUserIdentityCountInRepoIs(remoteRoot, 1);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 1);

		// Create invitation token for Khaled (first friend).
		UserRepoInvitationToken userRepoInvitationToken = createUserRepoInvitationToken("", PermissionType.read, TestUser.khaled);
		List<PublicKeyWithSignature> userRepoKeyPublicKeys = getUser(TestUser.khaled).getUserRepoKeyPublicKeys(remoteRepositoryId);
		assertThat(userRepoKeyPublicKeys).hasSize(1);
		PublicKeyWithSignature pk = userRepoKeyPublicKeys.get(0);

		assertUserIdentityCountInRepoIs(localSrcRoot, 2);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 3); // 2 user-identities readable by 1 user (2 * 1)
		                                                      // + 1 readable by himself (no permission granted yet, but always able to read self)

		grantPermission("", PermissionType.readUserIdentity, pk);

		// Need to sync the data for the invitation-token! Otherwise the token is useless!
		syncFromLocalSrcToRemote();

		assertUserIdentitiesReadable(localSrcRoot);

		assertUserIdentityCountInRepoIs(localSrcRoot, 2);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 4); // 2 user-identities readable by 2 users (2 * 2)
		assertUserIdentityCountInRepoIs(remoteRoot, 2);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 4);



		// *** Khaled's (first friend) machine with his repository ***
		switchLocationTo(TestUser.khaled);
		// create local repo, then connect to server-repo using invitation-token and sync down!
		createLocalDestinationRepo();

		// Import invitation token.
		importUserRepoInvitationToken(userRepoInvitationToken);

		// and sync.
		syncFromRemoteToLocalDest();

		assertUserIdentitiesReadable(localDestRoot);

		assertUserIdentityCountInRepoIs(localDestRoot, 3); // 3 user-identities for 2 real keys + 1 invitation-key
		assertUserIdentityLinkCountInRepoIs(localDestRoot, 8); // the 3 identities are readable by 2 real keys (3 * 2 = 6)
		                                                       // + the invitation key can read itself => 7
		                                                       // + lingering old data (for the invitation-key - this is cleaned up later)
		assertUserIdentityCountInRepoIs(remoteRoot, 3);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 8);



		// *** OWNER machine with owner's repository ***
		switchLocationTo(TestUser.marco);

		// sync to finalize 1st invitation => invitation-user-repo-key *and* its user-identity should be deleted.
		syncFromLocalSrcToRemote();

		assertUserIdentityCountInRepoIs(localSrcRoot, 2);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 4);
		assertUserIdentityCountInRepoIs(remoteRoot, 2);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 4);


		// Now invite Bieber (2nd friend).
		userRepoInvitationToken = createUserRepoInvitationToken("", PermissionType.read, TestUser.bieber);
		pk = getUser(TestUser.bieber).getUserRepoKeyPublicKeys(remoteRepositoryId).get(0);
		grantPermission("", PermissionType.readUserIdentity, pk);

		// Sync to make the new invitation token usable!
		syncFromLocalSrcToRemote();

		assertUserIdentitiesReadable(localSrcRoot);

		assertUserIdentityCountInRepoIs(localSrcRoot, 3);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 9);
		assertUserIdentityCountInRepoIs(remoteRoot, 3);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 9);



		// *** Khaled's (first friend) machine with his repository ***
		switchLocationTo(TestUser.khaled);

		// Sync Bieber's temporary invitation-user-repo-key into Khaled's repo.
		syncFromRemoteToLocalDest();

		assertUserIdentitiesReadable(localDestRoot);

		assertUserIdentityCountInRepoIs(localDestRoot, 3);
		assertUserIdentityLinkCountInRepoIs(localDestRoot, 9);
		assertUserIdentityCountInRepoIs(remoteRoot, 3);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 9);




		// *** Bieber's (2nd friend) machine with his repository ***
		switchLocationTo(TestUser.bieber);

		// create local repo, then connect to server-repo using invitation-token and sync down!
		createLocalDestinationRepo();

		// Import invitation token.
		importUserRepoInvitationToken(userRepoInvitationToken);

		// and sync.
		syncFromRemoteToLocalDest();

		assertUserIdentitiesReadable(localDestRoot);

		assertUserIdentityCountInRepoIs(localDestRoot, 4); // 4 user-identities for 3 real keys + 1 invitation-key
		assertUserIdentityLinkCountInRepoIs(localDestRoot, 15);
		assertUserIdentityCountInRepoIs(remoteRoot, 4);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 15);



		// *** OWNER machine with owner's repository ***
		// needed to finalize the invitation (i.e. replace the invitation-user-repo-key by the real key).
		switchLocationTo(TestUser.marco);

		syncFromLocalSrcToRemote();



		// *** Khaled's (first friend) machine with his repository ***
		switchLocationTo(TestUser.khaled);

		// Sync - this was when issue 5 showed up.
		syncFromRemoteToLocalDest();
	}
}
