package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.URL;

public class UserRepoInvitation {

	private final URL serverUrl;

	private final String serverPath;

	private final UserRepoKey invitationUserRepoKey;

	public UserRepoInvitation(final URL serverUrl, final String serverPath, final UserRepoKey invitationUserRepoKey) {
		this.serverUrl = assertNotNull("serverUrl", serverUrl);
		this.serverPath = assertNotNull("serverPath", serverPath);
		this.invitationUserRepoKey = assertNotNull("invitationUserRepoKey", invitationUserRepoKey);
	}

	public URL getServerUrl() {
		return serverUrl;
	}

	public String getServerPath() {
		return serverPath;
	}

	public UserRepoKey getInvitationUserRepoKey() {
		return invitationUserRepoKey;
	}
}
