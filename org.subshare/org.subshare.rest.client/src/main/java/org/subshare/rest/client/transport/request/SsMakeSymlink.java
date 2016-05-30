package org.subshare.rest.client.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Date;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.RepoFileDtoWithCurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.SsSymlinkDto;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.rest.client.request.MakeSymlink;

public class SsMakeSymlink extends MakeSymlink {

	protected final RepoFileDtoWithCurrentHistoCryptoRepoFileDto repoFileDtoWithCurrentHistoCryptoRepoFileDto;

	public SsMakeSymlink(final String repositoryName, final String path, final RepoFileDtoWithCurrentHistoCryptoRepoFileDto repoFileDtoWithCurrentHistoCryptoRepoFileDto) {
		super(repositoryName, path, SsSymlinkDto.DUMMY_TARGET, new Date(0));
		this.repoFileDtoWithCurrentHistoCryptoRepoFileDto = assertNotNull("repoFileDtoWithCurrentHistoCryptoRepoFileDto", repoFileDtoWithCurrentHistoCryptoRepoFileDto);

		final CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto = assertNotNull(
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.currentHistoCryptoRepoFileDto",
				repoFileDtoWithCurrentHistoCryptoRepoFileDto.getCurrentHistoCryptoRepoFileDto());

		assertNotNull("repoFileDtoWithCurrentHistoCryptoRepoFileDto.currentHistoCryptoRepoFileDto.histoCryptoRepoFileDto",
				currentHistoCryptoRepoFileDto.getHistoCryptoRepoFileDto());

		final RepoFileDto rfdto = assertNotNull("repoFileDtoWithCurrentHistoCryptoRepoFileDto.repoFileDto",
				repoFileDtoWithCurrentHistoCryptoRepoFileDto.getRepoFileDto());

		if (! (rfdto instanceof SsSymlinkDto))
			throw new IllegalArgumentException("repoFileDtoWithCurrentHistoCryptoRepoFileDto.repoFileDto is not an instance of SsSymlinkDto, but: " + rfdto.getClass().getName());
	}

	@Override
	protected Response _execute() {
		final WebTarget webTarget = createMakeSymlinkWebTarget();
		return assignCredentials(webTarget.request()).post(Entity.entity(repoFileDtoWithCurrentHistoCryptoRepoFileDto, MediaType.APPLICATION_XML_TYPE));
	}

}
