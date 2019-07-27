package org.subshare.core.user;

import static java.util.Objects.*;

import java.io.Serializable;

public class UserRepoInvitationToken implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final String CONTENT_TYPE_USER_REPO_INVITATION = "application/vnd.subshare.user-repo-invitation";

	private final byte[] signedEncryptedUserRepoInvitationData;

	public UserRepoInvitationToken(byte[] signedEncryptedUserRepoInvitationData) {
		this.signedEncryptedUserRepoInvitationData = requireNonNull(signedEncryptedUserRepoInvitationData, "signedEncryptedUserRepoInvitationData");
	}

	public byte[] getSignedEncryptedUserRepoInvitationData() {
		return signedEncryptedUserRepoInvitationData;
	}
}
