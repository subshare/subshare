package org.subshare.core.user;

import static java.util.Objects.*;

import java.net.URL;

public class UserRepoInvitation {

	private final URL serverUrl;

	private final String serverPath;

	private final UserRepoKey invitationUserRepoKey;

	public UserRepoInvitation(final URL serverUrl, final String serverPath, final UserRepoKey invitationUserRepoKey) {
		this.serverUrl = requireNonNull(serverUrl, "serverUrl");
		this.serverPath = requireNonNull(serverPath, "serverPath");
		this.invitationUserRepoKey = requireNonNull(invitationUserRepoKey, "invitationUserRepoKey");
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
