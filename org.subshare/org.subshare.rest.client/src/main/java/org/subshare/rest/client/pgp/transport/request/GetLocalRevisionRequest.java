package org.subshare.rest.client.pgp.transport.request;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.subshare.core.dto.LongDto;

import co.codewizards.cloudstore.rest.client.request.AbstractRequest;

public class GetLocalRevisionRequest extends AbstractRequest<Long> {

	@Override
	public Long execute() {
		final WebTarget webTarget = createWebTarget("_PgpPublicKey", "_localRevision");
		final LongDto localRevision = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(LongDto.class);
		return localRevision.getValue();
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}
}
