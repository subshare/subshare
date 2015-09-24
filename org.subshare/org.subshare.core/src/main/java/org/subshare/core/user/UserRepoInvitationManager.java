package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;

import org.subshare.core.dto.PermissionType;
import org.subshare.core.pgp.PgpKey;
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

	/**
	 * Creates an invitation for the given {@code user} encrypted with the given {@code userPgpKeys}.
	 *
	 * @param localPath the local path to which we grant permission. Must not be <code>null</code>. To grant
	 * permission to the entire repository, it must be an empty String.
	 * @param user the user to be invited. Must not be <code>null</code>. We grant permission to this user.
	 * @param userPgpKeys the PGP keys used to encrypt the invitation. May be <code>null</code> causing the
	 * invitation to be encrypted with all valid keys. If this is not <code>null</code>, it must contain
	 * exclusively PGP keys belonging to the specified {@code user}.
	 * @param permissionType the permission being granted to the invited {@code user}. Must not be <code>null</code>.
	 * @param validityDurationMillis how long should the invitation token be valid? It expires after this number of
	 * milliseconds.
	 * @return the invitation for the specified {@code user}. Never <code>null</code>.
	 */
	UserRepoInvitationToken createUserRepoInvitationToken(final String localPath, final User user, Set<PgpKey> userPgpKeys, PermissionType permissionType, final long validityDurationMillis);

	ServerRepo importUserRepoInvitationToken(UserRepoInvitationToken userRepoInvitationToken);
}