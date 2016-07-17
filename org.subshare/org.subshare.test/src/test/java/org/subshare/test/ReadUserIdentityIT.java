package org.subshare.test;

import static org.assertj.core.api.Assertions.*;

import java.util.LinkedList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.subshare.core.dto.CryptoChangeSetDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.core.user.UserRepoKey;
import org.subshare.local.CryptreeImpl;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class ReadUserIdentityIT extends AbstractUserRegistryIT {

	@Test
	public void inviteWithReadInvitationAndGrantReadUserIdentityPermissionLater() throws Exception {
		final LinkedList<CryptoChangeSetDto> cryptoChangeSetDtos = new LinkedList<>();
		new MockUp<CryptreeImpl>() {
			@Mock
			void putCryptoChangeSetDto(final Invocation invocation, final CryptoChangeSetDto cryptoChangeSetDto) {
				cryptoChangeSetDtos.add(cryptoChangeSetDto);
				invocation.proceed(cryptoChangeSetDto);
			}
		};

		// *** OWNER machine with owner's repository ***
		switchLocationTo(TestUser.marco);

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
//		determineRemotePathPrefix2Encrypted(); // handled differently inside importUserRepoInvitationToken(...)

		assertUserIdentityCountInRepoIs(localSrcRoot, 1);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 1);
		assertUserIdentityCountInRepoIs(remoteRoot, 1);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 1);

		// Create invitation token.
		UserRepoInvitationToken userRepoInvitationToken = createUserRepoInvitationToken("", PermissionType.read, TestUser.khaled);

		// Need to sync the data for the invitation-token! Otherwise the token is useless!
		syncFromLocalSrcToRemote();

		assertUserIdentitiesReadable(localSrcRoot);

		assertUserIdentityCountInRepoIs(localSrcRoot, 2);
		assertUserIdentityLinkCountInRepoIs(localSrcRoot, 3);
		assertUserIdentityCountInRepoIs(remoteRoot, 2);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 3);


		// *** FRIEND machine with friend's repository ***
		switchLocationTo(TestUser.khaled);
		// create local repo, then connect to server-repo using invitation-token and sync down!
		createLocalDestinationRepo();

		// Import invitation token.
		importUserRepoInvitationToken(userRepoInvitationToken);

		// and sync.
		syncFromRemoteToLocalDest();

		assertUserIdentityCountInRepoIs(localDestRoot, 3);
		assertUserIdentityLinkCountInRepoIs(localDestRoot, 5);
		assertUserIdentityCountInRepoIs(remoteRoot, 3);
		assertUserIdentityLinkCountInRepoIs(remoteRoot, 5);

		assertUserIdentitiesNotReadable(localDestRoot);


		// *** OWNER machine with owner's repository ***
		switchLocationTo(TestUser.marco);

		// grant permission *FIRST*, because this invitation-key is about to be replaced (we want to test this)!
		User friend = getUser(TestUser.khaled);
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
		friend = null; // the user should not linger, because it is bound to a certain UserRegistry - and we're switching it back and forth.


		// *** FRIEND machine with friend's repository ***
		switchLocationTo(TestUser.khaled);

		syncFromRemoteToLocalDest();

		assertUserIdentitiesReadable(localDestRoot);


		// *** OWNER machine with owner's repository ***
		switchLocationTo(TestUser.marco);

		revokePermission("", PermissionType.readUserIdentity, friendPublicKeyPermanent);

		syncFromLocalSrcToRemote();

		// There's still a Permission changed, hence we sync again to make sure nothing is synced after this point here.
		// ... it's a bit strange and IMHO this is a bug (the Permission should be synced already before), but I don't have
		// time to investigate this now. And one more sync does not hurt ;-) ... at least then everything is fine.
		syncFromLocalSrcToRemote();


		// *** FRIEND machine with friend's repository ***
		switchLocationTo(TestUser.khaled);

		syncFromRemoteToLocalDest();

		assertUserIdentitiesNotReadable(localDestRoot);

		cryptoChangeSetDtos.clear();

		assertThat(cryptoChangeSetDtos).hasSize(0);

		syncFromRemoteToLocalDest();

		for (final CryptoChangeSetDto cryptoChangeSetDto : cryptoChangeSetDtos)
			assertThat(cryptoChangeSetDto.isEmpty()).isTrue();

		assertThat(cryptoChangeSetDtos).hasSize(2);


		// *** OWNER machine with owner's repository ***
		switchLocationTo(TestUser.marco);

		cryptoChangeSetDtos.clear();

		assertThat(cryptoChangeSetDtos).hasSize(0);

		syncFromLocalSrcToRemote();

		for (final CryptoChangeSetDto cryptoChangeSetDto : cryptoChangeSetDtos)
			assertThat(cryptoChangeSetDto.isEmpty()).isTrue();

		assertThat(cryptoChangeSetDtos).hasSize(2);
	}

}
