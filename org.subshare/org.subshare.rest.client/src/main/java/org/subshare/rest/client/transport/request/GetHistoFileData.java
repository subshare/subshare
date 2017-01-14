package org.subshare.rest.client.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.rest.client.request.AbstractRequest;

public class GetHistoFileData extends AbstractRequest<byte[]> {
	private final String repositoryName;
	private final Uid histoCryptoRepoFileId;
	private final long offset;

	public GetHistoFileData(final String repositoryName, Uid histoCryptoRepoFileId, final long offset) {
		this.repositoryName = assertNotNull(repositoryName, "repositoryName");
		this.histoCryptoRepoFileId = assertNotNull(histoCryptoRepoFileId, "histoCryptoRepoFileId");
		this.offset = offset;
	}

	@Override
	public byte[] execute() {
		WebTarget webTarget = createWebTarget("_getHistoFileData",
				urlEncode(repositoryName),
				histoCryptoRepoFileId.toString(),
				Long.toString(offset));

//		if (offset > 0) // defaults to 0
//			webTarget = webTarget.queryParam("offset", offset);

		return assignCredentials(webTarget.request(MediaType.APPLICATION_OCTET_STREAM)).get(byte[].class);
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}
}
