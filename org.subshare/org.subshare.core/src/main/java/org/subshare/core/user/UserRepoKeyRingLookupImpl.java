package org.subshare.core.user;

import static java.util.Objects.*;

import java.util.List;
import java.util.UUID;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.ServerRepoRegistryImpl;

import co.codewizards.cloudstore.core.Uid;

public class UserRepoKeyRingLookupImpl implements UserRepoKeyRingLookup {

	@Override
	public UserRepoKeyRing getUserRepoKeyRing(final UserRepoKeyRingLookupContext context) {
		requireNonNull(context, "context");

		final UUID serverRepositoryId = context.getServerRepositoryId();

		final List<ServerRepo> serverRepos = ServerRepoRegistryImpl.getInstance().getServerRepos();
		for (final ServerRepo serverRepo : serverRepos) {
			if (serverRepositoryId.equals(serverRepo.getRepositoryId()))
				return getUserRepoKeyRing(serverRepo);
		}
		throw new IllegalArgumentException("No ServerRepo found with serverRepositoryId=" + serverRepositoryId);
	}

	private UserRepoKeyRing getUserRepoKeyRing(final ServerRepo serverRepo) {
		requireNonNull(serverRepo, "serverRepo");
		final Uid userId = serverRepo.getUserId();
		requireNonNull(userId, "serverRepo.userId");

		final User user = UserRegistryImpl.getInstance().getUserByUserIdOrFail(userId);
		return user.getUserRepoKeyRingOrCreate();
	}
}
