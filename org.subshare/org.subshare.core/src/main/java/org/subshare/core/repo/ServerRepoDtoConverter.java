package org.subshare.core.repo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.dto.ServerRepoDto;

public class ServerRepoDtoConverter {

	public ServerRepoDto toServerRepoDto(final ServerRepo serverRepo) {
		assertNotNull("serverRepo", serverRepo);
		final ServerRepoDto serverRepoDto = new ServerRepoDto();
		serverRepoDto.setRepositoryId(serverRepo.getRepositoryId());
		serverRepoDto.setName(serverRepo.getName());
		serverRepoDto.setServerId(serverRepo.getServerId());
		serverRepoDto.setUserId(serverRepo.getUserId());
		return serverRepoDto;
	}

	public ServerRepo fromServerRepoDto(final ServerRepoDto serverRepoDto) {
		assertNotNull("serverRepoDto", serverRepoDto);
		final ServerRepo serverRepo = new ServerRepoImpl(serverRepoDto.getRepositoryId());
		serverRepo.setName(serverRepoDto.getName());
		serverRepo.setServerId(serverRepoDto.getServerId());
		serverRepo.setUserId(serverRepoDto.getUserId());
		return serverRepo;
	}
}
