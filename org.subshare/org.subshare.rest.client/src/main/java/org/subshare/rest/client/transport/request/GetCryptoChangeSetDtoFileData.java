package org.subshare.rest.client.transport.request;

import static java.util.Objects.*;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.subshare.core.dto.CryptoChangeSetDto;

import co.codewizards.cloudstore.rest.client.request.AbstractRequest;

public class GetCryptoChangeSetDtoFileData extends AbstractRequest<byte[]> {

	private final String repositoryName;
	private final int multiPartIndex;

	public GetCryptoChangeSetDtoFileData(final String repositoryName, final int multiPartIndex) {
		this.repositoryName = requireNonNull(repositoryName, "repositoryName");
		this.multiPartIndex = multiPartIndex;
	}

	@Override
	public byte[] execute() {
		WebTarget webTarget = createWebTarget(getPath(CryptoChangeSetDto.class), urlEncode(repositoryName),
				"file", Integer.toString(multiPartIndex));

		return assignCredentials(webTarget.request(MediaType.APPLICATION_OCTET_STREAM)).get(byte[].class);
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}

}
