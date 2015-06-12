package org.subshare.gui.ls;

import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRepoInvitationManager;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class UserRepoInvitationManagerLs {

	private UserRepoInvitationManagerLs() {
	}

	public static UserRepoInvitationManager getUserRepoInvitationManager(final UserRegistry userRegistry, final LocalRepoManager localRepoManager) {
		return LocalServerClient.getInstance().invokeStatic(UserRepoInvitationManager.Helper.class,
				"getInstance",
				userRegistry, localRepoManager);
	}
}
