package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.subshare.core.context.RepoFileContext;
import org.subshare.core.dto.SsNormalFileDto;
import org.subshare.core.dto.CryptoRepoFileOnServerDto;
import org.subshare.core.dto.RepoFileDtoWithCryptoRepoFileOnServerDto;

import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.rest.server.service.EndPutFileService;

@Path("_endPutFile/{repositoryName}") // need to redeclare - seems not to be inherited
@Consumes(MediaType.APPLICATION_XML)
@Produces(MediaType.APPLICATION_XML)
public class SsEndPutFileService extends EndPutFileService {

	@PUT
	@Path("{path:.*}")
	public void endPutFile(@PathParam("path") final String path, final RepoFileDtoWithCryptoRepoFileOnServerDto repoFileDtoWithCryptoRepoFileOnServerDto)
	{
		assertNotNull("path", path);
		assertNotNull("repoFileDtoWithCryptoRepoFileOnServerDto", repoFileDtoWithCryptoRepoFileOnServerDto);

		CryptoRepoFileOnServerDto cryptoRepoFileOnServerDto = assertNotNull("repoFileDtoWithCryptoRepoFileOnServerDto.cryptoRepoFileOnServerDto",
				repoFileDtoWithCryptoRepoFileOnServerDto.getCryptoRepoFileOnServerDto());

		RepoFileDto rfdto = assertNotNull("repoFileDtoWithCryptoRepoFileOnServerDto.repoFileDto",
				repoFileDtoWithCryptoRepoFileOnServerDto.getRepoFileDto());

		if (! (rfdto instanceof SsNormalFileDto))
			throw new IllegalArgumentException("repoFileDtoWithCryptoRepoFileOnServerDto.repoFileDto is not an instance of SsNormalFileDto, but: " + rfdto.getClass().getName());

		final SsNormalFileDto normalFileDto = (SsNormalFileDto) rfdto;

		RepoFileContext.setContext(new RepoFileContext(path, normalFileDto, cryptoRepoFileOnServerDto));
		try {
			final String sha1 = null; // no need
			super.endPutFile(path, new DateTime(new Date(0)), normalFileDto.getLength(), sha1);
		} finally {
			RepoFileContext.setContext(null);
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
