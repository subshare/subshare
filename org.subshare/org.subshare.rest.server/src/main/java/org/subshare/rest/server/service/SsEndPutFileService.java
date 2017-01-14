package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.RepoFileDtoWithCurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.repo.transport.CryptreeServerFileRepoTransport;

import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.rest.server.service.EndPutFileService;

@Path("_endPutFile/{repositoryName}") // need to redeclare - seems not to be inherited
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class SsEndPutFileService extends EndPutFileService {

	@PUT
	@Path("{path:.*}")
	public void endPutFile(@PathParam("path") String path, final RepoFileDtoWithCurrentHistoCryptoRepoFileDto repoFileDtoWithCurrentHistoCryptoRepoFileDto)
	{
		assertNotNull(path, "path");
		assertNotNull(repoFileDtoWithCurrentHistoCryptoRepoFileDto, "repoFileDtoWithCurrentHistoCryptoRepoFileDto");

		final CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto = assertNotNull(
				repoFileDtoWithCurrentHistoCryptoRepoFileDto.getCurrentHistoCryptoRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.currentHistoCryptoRepoFileDto");

		assertNotNull(currentHistoCryptoRepoFileDto.getHistoCryptoRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.currentHistoCryptoRepoFileDto.histoCryptoRepoFileDto");

		RepoFileDto rfdto = assertNotNull(repoFileDtoWithCurrentHistoCryptoRepoFileDto.getRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.repoFileDto");

		if (! (rfdto instanceof SsNormalFileDto))
			throw new IllegalArgumentException("repoFileDtoWithCurrentHistoCryptoRepoFileDto.repoFileDto is not an instance of SsNormalFileDto, but: " + rfdto.getClass().getName());

		final SsNormalFileDto normalFileDto = (SsNormalFileDto) rfdto;

//		RepoFileContext.setContext(new RepoFileContext(path, normalFileDto, cryptoRepoFileOnServerDto));
//		try {
//			final String sha1 = null; // no need
//			super.endPutFile(path, new DateTime(new Date(0)), normalFileDto.getLength(), sha1);
//		} finally {
//			RepoFileContext.setContext(null);
//		}

		final CryptreeServerFileRepoTransport repoTransport = (CryptreeServerFileRepoTransport) authenticateAndCreateLocalRepoTransport();
		try {
			path = repoTransport.unprefixPath(path);
			repoTransport.endPutFile(path, normalFileDto, currentHistoCryptoRepoFileDto);
		} finally {
			repoTransport.close();
		}
	}

	@POST // just annotating to get it out of the way (otherwise the super-method-stuff seems to be inherited)
	@Override
	public void endPutFile(@PathParam("path") final String path,
			@QueryParam("lastModified") final DateTime lastModified,
			@QueryParam("length") final long length,
			@QueryParam("sha1") final String sha1) {
		throw new UnsupportedOperationException("Missing normalFileDto!");
	}
}
