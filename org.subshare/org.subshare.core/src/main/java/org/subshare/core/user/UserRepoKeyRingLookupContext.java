package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.UUID;

public class UserRepoKeyRingLookupContext {

	private final UUID clientRepositoryId;
	private final UUID serverRepositoryId;

	public UserRepoKeyRingLookupContext(final UUID clientRepositoryId, final UUID serverRepositoryId) {
		this.clientRepositoryId = assertNotNull(clientRepositoryId, "clientRepositoryId");
		this.serverRepositoryId = assertNotNull(serverRepositoryId, "serverRepositoryId");
	}

	public UUID getClientRepositoryId() {
		return clientRepositoryId;
	}

	public UUID getServerRepositoryId() {
		return serverRepositoryId;
	}
}
