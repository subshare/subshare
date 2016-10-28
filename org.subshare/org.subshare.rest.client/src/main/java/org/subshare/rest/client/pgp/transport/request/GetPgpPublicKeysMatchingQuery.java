package org.subshare.rest.client.pgp.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.InputStream;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import co.codewizards.cloudstore.rest.client.request.AbstractRequest;

public class GetPgpPublicKeysMatchingQuery extends AbstractRequest<InputStream> {

	private final String queryString;

	public GetPgpPublicKeysMatchingQuery(String queryString) {
		this.queryString = assertNotNull("queryString", queryString);
	}

	@Override
	public InputStream execute() {
		WebTarget webTarget = createWebTarget("_PgpPublicKey/_search", urlEncode(queryString));
		final InputStream in = assignCredentials(webTarget.request(MediaType.APPLICATION_OCTET_STREAM_TYPE)).get(InputStream.class);
		return in;
	}

	@Override
	public boolean isResultNullable() {
		return true;
	}
}
