package org.subshare.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.core.user.UserRepoKey;
import org.junit.Test;

public class ReadUserIdentityIT extends AbstractUserRegistryIT {

	@Test
	public void inviteWithReadInvitationAndGrantSeeUserIdentityPermissionLater() throws Exception {
		// *** OWNER machine with owner's repository ***
		switchLocationToOwner();

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();

		assertUserIdentityInRepoIs(localSrcRoot, 1);
		assertUserIdentityInRepoIs(remoteRoot, 1);

		// Create invitation token.
		UserRepoInvitationToken userRepoInvitationToken = createUserRepoInvitationToken("", PermissionType.read);

		// Need to sync the data for the invitation-token! Otherwise the token is useless!
		syncFromLocalSrcToRemote();

		assertUserIdentitiesReadable(localSrcRoot);

		assertUserIdentityInRepoIs(localSrcRoot, 2);
		assertUserIdentityInRepoIs(remoteRoot, 2);


		// *** FRIEND machine with friend's repository ***
		switchLocationToFriend();
		// create local repo, then connect to server-repo using invitation-token and sync down!
		createLocalDestinationRepo();

		// Import invitation token.
		importUserRepoInvitationToken(userRepoInvitationToken);

		// and sync.
		syncFromRemoteToLocalDest();

		assertUserIdentityInRepoIs(localDestRoot, 3);
		assertUserIdentityInRepoIs(remoteRoot, 3);

		assertUserIdentitiesNotReadable(localDestRoot);


		// *** OWNER machine with owner's repository ***
		switchLocationToOwner();

		// grant permission *FIRST*, because this invitation-key is about to be replaced (we want to test this)!
		assertThat(friend.getUserRepoKeyPublicKeys()).hasSize(1);
		UserRepoKey.PublicKeyWithSignature friendPublicKeyForInvitation = friend.getUserRepoKeyPublicKeys().get(0);
		assertThat(friendPublicKeyForInvitation.isInvitation()).isTrue();

		grantPermission("", PermissionType.readUserIdentity, friendPublicKeyForInvitation);

		// sync down after granting - the permission granted to the invitation-key should be transferred to the new, permanent key.
		syncFromLocalSrcToRemote();

		// We expect the sync to have replaced the key in our user-registry.
		assertThat(friend.getUserRepoKeyPublicKeys()).hasSize(1);
		UserRepoKey.PublicKeyWithSignature friendPublicKeyPermanent = friend.getUserRepoKeyPublicKeys().get(0);
		assertThat(friendPublicKeyPermanent.isInvitation()).isFalse();


		// *** FRIEND machine with friend's repository ***
		switchLocationToFriend();

		syncFromRemoteToLocalDest();

		assertUserIdentitiesReadable(localDestRoot);


		// *** OWNER machine with owner's repository ***
		switchLocationToOwner();

		revokePermission("", PermissionType.readUserIdentity, friendPublicKeyPermanent);

		syncFromLocalSrcToRemote();


		// *** FRIEND machine with friend's repository ***
		switchLocationToFriend();

		syncFromRemoteToLocalDest();

		assertUserIdentitiesNotReadable(localDestRoot);
	}

}
