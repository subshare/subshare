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
import org.subshare.core.dto.SsSymlinkDto;
import org.subshare.core.repo.transport.CryptreeServerFileRepoTransport;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.rest.server.service.MakeSymlinkService;

@Path("_makeSymlink/{repositoryName}") // need to redeclare - seems not to be inherited
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class SsMakeSymlinkService extends MakeSymlinkService {

	@POST
	public void makeSymlink(final RepoFileDtoWithCurrentHistoCryptoRepoFileDto repoFileDtoWithCurrentHistoCryptoRepoFileDto)
	{
		makeSymlink("", repoFileDtoWithCurrentHistoCryptoRepoFileDto);
	}

	@POST
	@Path("{path:.*}")
	public void makeSymlink(@PathParam("path") String path, final RepoFileDtoWithCurrentHistoCryptoRepoFileDto repoFileDtoWithCurrentHistoCryptoRepoFileDto)
	{
		assertNotNull(path, "path");
		assertNotNull(repoFileDtoWithCurrentHistoCryptoRepoFileDto, "repoFileDtoWithCurrentHistoCryptoRepoFileDto");

		final CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto = assertNotNull(
				repoFileDtoWithCurrentHistoCryptoRepoFileDto.getCurrentHistoCryptoRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.currentHistoCryptoRepoFileDto");

		assertNotNull(currentHistoCryptoRepoFileDto.getHistoCryptoRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.currentHistoCryptoRepoFileDto.histoCryptoRepoFileDto");

		final RepoFileDto rfdto = assertNotNull(repoFileDtoWithCurrentHistoCryptoRepoFileDto.getRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.repoFileDto");

		if (! (rfdto instanceof SsSymlinkDto))
			throw new IllegalArgumentException("repoFileDtoWithCurrentHistoCryptoRepoFileDto.repoFileDto is not an instance of SsSymlinkDto, but: " + rfdto.getClass().getName());

		final SsSymlinkDto symlinkDto = (SsSymlinkDto) rfdto;

		try (final CryptreeServerFileRepoTransport repoTransport = (CryptreeServerFileRepoTransport) authenticateAndCreateLocalRepoTransport();) {
			path = repoTransport.unprefixPath(path);
			repoTransport.makeSymlink(path, symlinkDto, currentHistoCryptoRepoFileDto);
		}
	}

	@Override
	@POST
	@Path("_SsMakeSymlinkService_NOT_SUPPORTED_1")
	public void makeSymlink()
	{
		throw new UnsupportedOperationException("Missing DTO!");
	}

	@Override
	@POST
	@Path("_SsMakeSymlinkService_NOT_SUPPORTED_2")
	public void makeSymlink(@PathParam("path") final String path)
	{
		throw new UnsupportedOperationException("Missing DTO!");
	}

}
