package org.subshare.core.user;

import static java.util.Objects.*;

import java.util.UUID;

public class UserRepoKeyRingLookupContext {

	private final UUID clientRepositoryId;
	private final UUID serverRepositoryId;

	public UserRepoKeyRingLookupContext(final UUID clientRepositoryId, final UUID serverRepositoryId) {
		this.clientRepositoryId = requireNonNull(clientRepositoryId, "clientRepositoryId");
		this.serverRepositoryId = requireNonNull(serverRepositoryId, "serverRepositoryId");
	}

	public UUID getClientRepositoryId() {
		return clientRepositoryId;
	}

	public UUID getServerRepositoryId() {
		return serverRepositoryId;
	}
}
