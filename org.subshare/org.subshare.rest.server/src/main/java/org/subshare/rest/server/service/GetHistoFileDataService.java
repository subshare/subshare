package org.subshare.rest.server.service;

import static java.util.Objects.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.subshare.core.repo.transport.CryptreeServerFileRepoTransport;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.rest.server.service.AbstractServiceWithRepoToRepoAuth;

@Path("_getHistoFileData/{repositoryName}")
public class GetHistoFileDataService extends AbstractServiceWithRepoToRepoAuth {

	@GET
	@Path("{histoCryptoRepoFileId}/{offset}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public byte[] getHistoFileData(
			@PathParam("histoCryptoRepoFileId") Uid histoCryptoRepoFileId,
			@PathParam("offset") final long offset)
	{
		requireNonNull(histoCryptoRepoFileId, "histoCryptoRepoFileId");

		try (final CryptreeServerFileRepoTransport repoTransport = (CryptreeServerFileRepoTransport) authenticateAndCreateLocalRepoTransport()) {
			return repoTransport.getHistoFileData(histoCryptoRepoFileId, offset);
		}
	}

}
