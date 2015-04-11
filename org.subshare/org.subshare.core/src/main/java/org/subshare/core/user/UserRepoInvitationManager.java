package org.subshare.core.user;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.subshare.core.Cryptree;

public interface UserRepoInvitationManager {

	static class Helper {
		public static UserRepoInvitationManager getInstance(final UserRegistry userRegistry, final Cryptree cryptree) {
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
			result.setCryptree(cryptree);
			return result;
		}
	}

	int getPriority();

	UserRegistry getUserRegistry();
	void setUserRegistry(UserRegistry userRegistry);

	Cryptree getCryptree();
	void setCryptree(Cryptree cryptree);

	UserRepoInvitationToken createUserRepoInvitationToken(final String localPath, final User user, final long validityDurationMillis);

	void importUserRepoInvitationToken(UserRepoInvitationToken userRepoInvitationToken);

}