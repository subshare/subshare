package org.subshare.rest.server.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.subshare.core.dto.SsRepoFileDto;

import co.codewizards.cloudstore.core.dto.ChangeSetDto;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.rest.server.service.ChangeSetDtoService;

@Path("_ChangeSetDto/{repositoryName}") // need to redeclare - seems not to be inherited
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class SsChangeSetDtoService extends ChangeSetDtoService {

	@Override
	protected ChangeSetDto getChangeSetDto(final RepoTransport repoTransport, final boolean localSync, final Long lastSyncToRemoteRepoLocalRepositoryRevisionSynced) {
		final ChangeSetDto changeSetDto = super.getChangeSetDto(repoTransport, localSync, lastSyncToRemoteRepoLocalRepositoryRevisionSynced);

		// If this is a virgin repository, there is one single RepoFile (the root) which is
		// *not* signed. In this case, we simply filter it out, so that it is guaranteed that
		// the client *never* receives unsigned data.
		if (changeSetDto.getRepoFileDtos().size() == 1) {
			final SsRepoFileDto ssRepoFileDto = (SsRepoFileDto) changeSetDto.getRepoFileDtos().get(0);
			if (ssRepoFileDto.getSignature() == null)
				changeSetDto.getRepoFileDtos().clear();
		}

		return changeSetDto;
	}
}
