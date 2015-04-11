package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.net.URL;

public class UserRepoInvitation {

	private final URL serverUrl;

	private UserRepoKey invitationUserRepoKey;

	public UserRepoInvitation(final URL serverUrl, final UserRepoKey invitationUserRepoKey) {
		this.serverUrl = assertNotNull("serverUrl", serverUrl);
		this.invitationUserRepoKey = assertNotNull("invitationUserRepoKey", invitationUserRepoKey);
	}

	public URL getServerUrl() {
		return serverUrl;
	}

	public UserRepoKey getInvitationUserRepoKey() {
		return invitationUserRepoKey;
	}
}
