package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.subshare.core.dto.SsDeleteModificationDto;
import org.subshare.core.repo.transport.CryptreeServerFileRepoTransport;

import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.rest.server.service.AbstractServiceWithRepoToRepoAuth;

@Path("_delete/{repositoryName}")
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class DeleteService extends AbstractServiceWithRepoToRepoAuth {

	@PUT
	public void delete(final SsDeleteModificationDto deleteModificationDto) {
		assertNotNull(deleteModificationDto, "deleteModificationDto");

		try (final RepoTransport repoTransport = authenticateAndCreateLocalRepoTransport();) {
			CryptreeServerFileRepoTransport csfrt = (CryptreeServerFileRepoTransport) repoTransport;
			csfrt.delete(deleteModificationDto);
		}
	}

}
