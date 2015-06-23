package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.subshare.core.dto.PermissionType;
import org.subshare.core.repo.ServerRepo;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;

public interface UserRepoInvitationManager {

	static class Helper {
		public static UserRepoInvitationManager getInstance(final UserRegistry userRegistry, final LocalRepoManager localRepoManager) {
			assertNotNull("userRegistry", userRegistry);
			assertNotNull("localRepoManager", localRepoManager);

			final ServiceLoader<UserRepoInvitationManager> serviceLoader = ServiceLoader.load(UserRepoInvitationManager.class);
			final Iterator<UserRepoInvitationManager> iterator = serviceLoader.iterator();
			UserRepoInvitationManager result = null;
			while (iterator.hasNext()) {
				final UserRepoInvitationManager manager = iterator.next();

				if (result == null || result.getPriority() < manager.getPriority())
					result = manager;
			}

			if (result == null)
				throw new IllegalStateException("No UserRepoInvitationManager implementation found!");

			result.setUserRegistry(userRegistry);
			result.setLocalRepoManager(localRepoManager);
			return result;
		}
	}

	int getPriority();

	UserRegistry getUserRegistry();
	void setUserRegistry(UserRegistry userRegistry);

	LocalRepoManager getLocalRepoManager();
	void setLocalRepoManager(LocalRepoManager localRepoManager);

	UserRepoInvitationToken createUserRepoInvitationToken(final String localPath, final User user, PermissionType permissionType, final long validityDurationMillis);

	ServerRepo importUserRepoInvitationToken(UserRepoInvitationToken userRepoInvitationToken);
}