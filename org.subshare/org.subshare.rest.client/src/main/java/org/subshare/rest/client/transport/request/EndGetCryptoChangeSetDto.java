package org.subshare.rest.client.transport.request;

import static java.util.Objects.*;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.subshare.core.dto.CryptoChangeSetDto;

import co.codewizards.cloudstore.rest.client.request.VoidRequest;

public class EndGetCryptoChangeSetDto extends VoidRequest {

	private final String repositoryName;

	public EndGetCryptoChangeSetDto(final String repositoryName) {
		this.repositoryName = requireNonNull(repositoryName, "repositoryName");
	}

	@Override
	protected Response _execute() {
		final WebTarget webTarget = createWebTarget(getPath(CryptoChangeSetDto.class), urlEncode(repositoryName), "endGet");
		return assignCredentials(webTarget.request()).post(null);
	}
}
