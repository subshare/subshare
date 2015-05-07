package org.subshare.rest.client.pgp.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpKeyIdList;

import co.codewizards.cloudstore.rest.client.request.AbstractRequest;

public class GetPgpPublicKeys extends AbstractRequest<InputStream> {

	private final PgpKeyIdList pgpKeyIdList;

	public GetPgpPublicKeys(final PgpKeyId ... pgpKeyIds) {
		this(new PgpKeyIdList(Arrays.asList(assertNotNull("pgpKeyIds", pgpKeyIds))));
	}

	public GetPgpPublicKeys(final Collection<PgpKeyId> pgpKeyIds) {
		assertNotNull("pgpKeyIds", pgpKeyIds);
		if (pgpKeyIds instanceof PgpKeyIdList)
			this.pgpKeyIdList = (PgpKeyIdList) pgpKeyIds;
		else
			this.pgpKeyIdList = new PgpKeyIdList(pgpKeyIds);
	}

	@Override
	public InputStream execute() {
		final WebTarget webTarget = createWebTarget("_PgpPublicKey", urlEncode(pgpKeyIdList.toString()));
		final InputStream in = assignCredentials(webTarget.request(MediaType.APPLICATION_XML)).get(InputStream.class);
		return in;
	}

	@Override
	public boolean isResultNullable() {
		return true;
	}
}
