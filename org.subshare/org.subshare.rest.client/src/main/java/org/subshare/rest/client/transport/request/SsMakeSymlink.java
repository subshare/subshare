package org.subshare.rest.client.transport.request;

import static java.util.Objects.*;

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
		this.repoFileDtoWithCurrentHistoCryptoRepoFileDto = requireNonNull(repoFileDtoWithCurrentHistoCryptoRepoFileDto, "repoFileDtoWithCurrentHistoCryptoRepoFileDto");

		final CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto = requireNonNull(
				repoFileDtoWithCurrentHistoCryptoRepoFileDto.getCurrentHistoCryptoRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.currentHistoCryptoRepoFileDto");

		requireNonNull(currentHistoCryptoRepoFileDto.getHistoCryptoRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.currentHistoCryptoRepoFileDto.histoCryptoRepoFileDto");

		final RepoFileDto rfdto = requireNonNull(repoFileDtoWithCurrentHistoCryptoRepoFileDto.getRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.repoFileDto");

		if (! (rfdto instanceof SsSymlinkDto))
			throw new IllegalArgumentException("repoFileDtoWithCurrentHistoCryptoRepoFileDto.repoFileDto is not an instance of SsSymlinkDto, but: " + rfdto.getClass().getName());
	}

	@Override
	protected Response _execute() {
		final WebTarget webTarget = createMakeSymlinkWebTarget();
		return assignCredentials(webTarget.request()).post(Entity.entity(repoFileDtoWithCurrentHistoCryptoRepoFileDto, MediaType.APPLICATION_XML_TYPE));
	}

}
