package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.RepoFileDtoWithCurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.SsDirectoryDto;
import org.subshare.core.repo.transport.CryptreeServerFileRepoTransport;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.rest.server.service.MakeDirectoryService;

@Path("_makeDirectory/{repositoryName}") // need to redeclare - seems not to be inherited
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class SsMakeDirectoryService extends MakeDirectoryService {

	@POST
	public void makeDirectory(final RepoFileDtoWithCurrentHistoCryptoRepoFileDto repoFileDtoWithCurrentHistoCryptoRepoFileDto)
	{
		makeDirectory("", repoFileDtoWithCurrentHistoCryptoRepoFileDto);
	}

	@POST
	@Path("{path:.*}")
	public void makeDirectory(@PathParam("path") String path, final RepoFileDtoWithCurrentHistoCryptoRepoFileDto repoFileDtoWithCurrentHistoCryptoRepoFileDto)
	{
		assertNotNull("path", path);
		assertNotNull("repoFileDtoWithCurrentHistoCryptoRepoFileDto", repoFileDtoWithCurrentHistoCryptoRepoFileDto);

		final CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto = assertNotNull(
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.currentHistoCryptoRepoFileDto",
				repoFileDtoWithCurrentHistoCryptoRepoFileDto.getCurrentHistoCryptoRepoFileDto());

		assertNotNull("repoFileDtoWithCurrentHistoCryptoRepoFileDto.currentHistoCryptoRepoFileDto.histoCryptoRepoFileDto",
				currentHistoCryptoRepoFileDto.getHistoCryptoRepoFileDto());

		final RepoFileDto rfdto = assertNotNull("repoFileDtoWithCurrentHistoCryptoRepoFileDto.repoFileDto",
				repoFileDtoWithCurrentHistoCryptoRepoFileDto.getRepoFileDto());

		if (! (rfdto instanceof SsDirectoryDto))
			throw new IllegalArgumentException("repoFileDtoWithCurrentHistoCryptoRepoFileDto.repoFileDto is not an instance of SsDirectoryDto, but: " + rfdto.getClass().getName());

		final SsDirectoryDto directoryDto = (SsDirectoryDto) rfdto;

//		RepoFileContext.setContext(new RepoFileContext(path, rfdto, cryptoRepoFileOnServerDto));
//		try {
//			super.makeDirectory(path);
//		} finally {
//			RepoFileContext.setContext(null);
//		}

		try (final CryptreeServerFileRepoTransport repoTransport = (CryptreeServerFileRepoTransport) authenticateAndCreateLocalRepoTransport();) {
			path = repoTransport.unprefixPath(path);
			repoTransport.makeDirectory(path, directoryDto, currentHistoCryptoRepoFileDto);
		}
	}

	@Override
	@POST
	@Path("_SsMakeDirectoryService_NOT_SUPPORTED_1")
	public void makeDirectory()
	{
		throw new UnsupportedOperationException("Missing directoryDto!");
	}

	@Override
	@POST
	@Path("_SsMakeDirectoryService_NOT_SUPPORTED_2")
	public void makeDirectory(@PathParam("path") final String path)
	{
		throw new UnsupportedOperationException("Missing directoryDto!");
	}

}
