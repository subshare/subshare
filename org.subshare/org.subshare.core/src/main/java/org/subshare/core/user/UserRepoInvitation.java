package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.net.URL;

public class UserRepoInvitation {

	private final URL serverUrl;

	private final UserRepoKey invitationUserRepoKey;

//	private final UserRepoKey.PublicKey signingUserRepoKeyPublicKey;

	public UserRepoInvitation(final URL serverUrl, final UserRepoKey invitationUserRepoKey) { // , final UserRepoKey.PublicKey signingUserRepoKeyPublicKey) {
		this.serverUrl = assertNotNull("serverUrl", serverUrl);
		this.invitationUserRepoKey = assertNotNull("invitationUserRepoKey", invitationUserRepoKey);
//		this.signingUserRepoKeyPublicKey = assertNotNull("signingUserRepoKeyPublicKey", signingUserRepoKeyPublicKey);
//
//		if (!signingUserRepoKeyPublicKey.getUserRepoKeyId().equals(invitationUserRepoKey.getPublicKey().getSignature().getSigningUserRepoKeyId()))
//			throw new IllegalArgumentException("signingUserRepoKeyPublicKey.userRepoKeyId != invitationUserRepoKey.publicKey.signature.signingUserRepoKeyId");
	}

	public URL getServerUrl() {
		return serverUrl;
	}

	public UserRepoKey getInvitationUserRepoKey() {
		return invitationUserRepoKey;
	}

//	public UserRepoKey.PublicKey getSigningUserRepoKeyPublicKey() {
//		return signingUserRepoKeyPublicKey;
//	}
}
