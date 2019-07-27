package org.subshare.rest.client.transport.request;

import static java.util.Objects.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.RepoFileDtoWithCurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.SsDirectoryDto;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.rest.client.request.MakeDirectory;

public class SsMakeDirectory extends MakeDirectory {

	protected final RepoFileDtoWithCurrentHistoCryptoRepoFileDto repoFileDtoWithCurrentHistoCryptoRepoFileDto;

	public SsMakeDirectory(final String repositoryName, final String path, final RepoFileDtoWithCurrentHistoCryptoRepoFileDto repoFileDtoWithCurrentHistoCryptoRepoFileDto) {
		super(repositoryName, path, SsDirectoryDto.DUMMY_LAST_MODIFIED);
		this.repoFileDtoWithCurrentHistoCryptoRepoFileDto = requireNonNull(repoFileDtoWithCurrentHistoCryptoRepoFileDto, "repoFileDtoWithCurrentHistoCryptoRepoFileDto");

		final CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto = requireNonNull(
				repoFileDtoWithCurrentHistoCryptoRepoFileDto.getCurrentHistoCryptoRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.currentHistoCryptoRepoFileDto");

		requireNonNull(currentHistoCryptoRepoFileDto.getHistoCryptoRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.currentHistoCryptoRepoFileDto.histoCryptoRepoFileDto");

		final RepoFileDto rfdto = requireNonNull(repoFileDtoWithCurrentHistoCryptoRepoFileDto.getRepoFileDto(),
				"repoFileDtoWithCurrentHistoCryptoRepoFileDto.repoFileDto");

		if (! (rfdto instanceof SsDirectoryDto))
			throw new IllegalArgumentException("repoFileDtoWithCurrentHistoCryptoRepoFileDto.repoFileDto is not an instance of SsDirectoryDto, but: " + rfdto.getClass().getName());
	}

	@Override
	protected Response _execute() {
		final WebTarget webTarget = createMakeDirectoryWebTarget();
		return assignCredentials(webTarget.request()).post(Entity.entity(repoFileDtoWithCurrentHistoCryptoRepoFileDto, MediaType.APPLICATION_XML_TYPE));
	}
}
