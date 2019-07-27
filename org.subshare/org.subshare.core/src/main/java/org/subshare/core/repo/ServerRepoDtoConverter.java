package org.subshare.core.repo;

import static java.util.Objects.*;

import org.subshare.core.dto.ServerRepoDto;

public class ServerRepoDtoConverter {

	public ServerRepoDto toServerRepoDto(final ServerRepo serverRepo) {
		requireNonNull(serverRepo, "serverRepo");
		final ServerRepoDto serverRepoDto = new ServerRepoDto();
		serverRepoDto.setRepositoryId(serverRepo.getRepositoryId());
		serverRepoDto.setName(serverRepo.getName());
		serverRepoDto.setServerId(serverRepo.getServerId());
		serverRepoDto.setUserId(serverRepo.getUserId());

		serverRepoDto.setChanged(serverRepo.getChanged());
		return serverRepoDto;
	}

	public ServerRepo fromServerRepoDto(final ServerRepoDto serverRepoDto) {
		requireNonNull(serverRepoDto, "serverRepoDto");
		final ServerRepo serverRepo = new ServerRepoImpl(serverRepoDto.getRepositoryId());
		serverRepo.setName(serverRepoDto.getName());
		serverRepo.setServerId(serverRepoDto.getServerId());
		serverRepo.setUserId(serverRepoDto.getUserId());

		serverRepo.setChanged(serverRepoDto.getChanged());
		return serverRepo;
	}
}
