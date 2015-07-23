package org.subshare.rest.client.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Date;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.subshare.core.dto.SsDirectoryDto;
import org.subshare.core.dto.RepoFileDtoWithCryptoRepoFileOnServerDto;

import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.rest.client.request.MakeDirectory;

public class SsMakeDirectory extends MakeDirectory {

	protected final RepoFileDtoWithCryptoRepoFileOnServerDto repoFileDtoWithCryptoRepoFileOnServerDto;

	public SsMakeDirectory(final String repositoryName, final String path, final RepoFileDtoWithCryptoRepoFileOnServerDto repoFileDtoWithCryptoRepoFileOnServerDto) {
		super(repositoryName, path, new Date(0));
		this.repoFileDtoWithCryptoRepoFileOnServerDto = assertNotNull("repoFileDtoWithCryptoRepoFileOnServerDto", repoFileDtoWithCryptoRepoFileOnServerDto);

		assertNotNull("repoFileDtoWithCryptoRepoFileOnServerDto.cryptoRepoFileOnServerDto",
				repoFileDtoWithCryptoRepoFileOnServerDto.getCryptoRepoFileOnServerDto());

		RepoFileDto rfdto = assertNotNull("repoFileDtoWithCryptoRepoFileOnServerDto.repoFileDto",
				repoFileDtoWithCryptoRepoFileOnServerDto.getRepoFileDto());

		if (! (rfdto instanceof SsDirectoryDto))
			throw new IllegalArgumentException("repoFileDtoWithCryptoRepoFileOnServerDto.repoFileDto is not an instance of SsDirectoryDto, but: " + rfdto.getClass().getName());
	}

	@Override
	protected Response _execute() {
		WebTarget webTarget = createWebTarget("_makeDirectory", urlEncode(repositoryName), encodePath(path));

		if (lastModified != null)
			webTarget = webTarget.queryParam("lastModified", new DateTime(lastModified));

		return assignCredentials(webTarget.request()).post(Entity.entity(repoFileDtoWithCryptoRepoFileOnServerDto, MediaType.APPLICATION_XML_TYPE));
	}

}
