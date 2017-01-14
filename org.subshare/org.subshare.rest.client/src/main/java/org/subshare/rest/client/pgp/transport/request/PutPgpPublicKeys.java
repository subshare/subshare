package org.subshare.rest.client.pgp.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.InputStream;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import co.codewizards.cloudstore.rest.client.request.VoidRequest;

public class PutPgpPublicKeys extends VoidRequest {

	private final InputStream inputStream;

	public PutPgpPublicKeys(final InputStream inputStream) {
		this.inputStream = assertNotNull(inputStream, "inputStream");
	}

	@Override
	protected Response _execute() {
		return assignCredentials(
				createWebTarget("_PgpPublicKey")
				.request()).put(Entity.entity(inputStream, MediaType.APPLICATION_OCTET_STREAM_TYPE));
	}
}
