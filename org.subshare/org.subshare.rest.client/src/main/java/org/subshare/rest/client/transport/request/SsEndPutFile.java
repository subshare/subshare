package org.subshare.rest.client.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Date;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.RepoFileDtoWithCurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.SsNormalFileDto;

import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.rest.client.request.EndPutFile;

public class SsEndPutFile extends EndPutFile {

	protected final RepoFileDtoWithCurrentHistoCryptoRepoFileDto repoFileDtoWithCurrentHistoCryptoRepoFileDto;

	public SsEndPutFile(final String repositoryName, final String path, final RepoFileDtoWithCurrentHistoCryptoRepoFileDto repoFileDtoWithCurrentHistoCryptoRepoFileDto) {
		super(repositoryName, path, new DateTime(new Date(0)), 0, null);
//		this.normalFileDto = assertNotNull("normalFileDto", normalFileDto);

		this.repoFileDtoWithCurrentHistoCryptoRepoFileDto = assertNotNull(repoFileDtoWithCurrentHistoCryptoRepoFileDto, "repoFileDtoWithCurrentHistoCryptoRepoFileDto");

		final CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto = assertNotNull(
				repoFileDtoWithCurrentHistoCryptoRepoFileDto.getCurrentHistoCryptoRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.currentHistoCryptoRepoFileDto");

		assertNotNull(currentHistoCryptoRepoFileDto.getHistoCryptoRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.currentHistoCryptoRepoFileDto.histoCryptoRepoFileDto");

		final RepoFileDto rfdto = assertNotNull(repoFileDtoWithCurrentHistoCryptoRepoFileDto.getRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.repoFileDto");

		if (! (rfdto instanceof SsNormalFileDto))
			throw new IllegalArgumentException("repoFileDtoWithCryptoRepoFileOnServerDto.repoFileDto is not an instance of SsNormalFileDto, but: " + rfdto.getClass().getName());
	}

	@Override
	public Response _execute() {
		return assignCredentials(
				createWebTarget("_endPutFile", urlEncode(repositoryName), encodePath(path))
				.request()).put(Entity.entity(repoFileDtoWithCurrentHistoCryptoRepoFileDto, MediaType.APPLICATION_XML_TYPE));
	}
}
