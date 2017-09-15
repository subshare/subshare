package org.subshare.rest.client.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.subshare.core.dto.CryptoChangeSetDto;

import co.codewizards.cloudstore.rest.client.request.AbstractRequest;

public class GetCryptoChangeSetDto extends AbstractRequest<CryptoChangeSetDto> {

	private final String repositoryName;
	private final Long lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced;

	public GetCryptoChangeSetDto(final String repositoryName, final Long lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced) {
		this.repositoryName = assertNotNull(repositoryName, "repositoryName");
		this.lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced = lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced;
	}

	@Override
	public CryptoChangeSetDto execute() {
		WebTarget webTarget = createWebTarget(getPath(CryptoChangeSetDto.class), urlEncode(repositoryName));

		if (lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced != null)
			webTarget = webTarget.queryParam("lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced", lastCryptoKeySyncToRemoteRepoLocalRepositoryRevisionSynced);

		final CryptoChangeSetDto dto = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(CryptoChangeSetDto.class);
		return dto;
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}
}
