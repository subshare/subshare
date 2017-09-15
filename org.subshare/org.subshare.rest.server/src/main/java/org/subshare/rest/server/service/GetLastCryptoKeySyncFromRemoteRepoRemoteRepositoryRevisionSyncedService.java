package org.subshare.rest.server.service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.subshare.core.repo.transport.CryptreeServerFileRepoTransport;

import co.codewizards.cloudstore.rest.server.service.AbstractServiceWithRepoToRepoAuth;

@Path("_getLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced/{repositoryName}")
public class GetLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSyncedService extends AbstractServiceWithRepoToRepoAuth {

	// Invoked during syncUp, only.
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Long getLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced()
	{
		try (final CryptreeServerFileRepoTransport repoTransport = (CryptreeServerFileRepoTransport) authenticateAndCreateLocalRepoTransport()) {
			return repoTransport.getLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced();
		}
	}
}
