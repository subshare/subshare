package org.subshare.rest.client.transport.request;

import static java.util.Objects.*;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.subshare.core.dto.CryptoChangeSetDto;

import co.codewizards.cloudstore.rest.client.request.VoidRequest;

public class PutCryptoChangeSetDto extends VoidRequest {

	private final String repositoryName;
	private final CryptoChangeSetDto cryptoChangeSetDto;

	public PutCryptoChangeSetDto(final String repositoryName, final CryptoChangeSetDto cryptoChangeSetDto) {
		this.repositoryName = requireNonNull(repositoryName, "repositoryName");
		this.cryptoChangeSetDto = requireNonNull(cryptoChangeSetDto, "cryptoChangeSetDto");
	}

	@Override
	protected Response _execute() {
		final WebTarget webTarget = createWebTarget(getPath(CryptoChangeSetDto.class), urlEncode(repositoryName));
		return assignCredentials(webTarget.request()).put(Entity.entity(cryptoChangeSetDto, MediaType.APPLICATION_XML_TYPE));
	}
}
