package org.subshare.rest.client.pgp.transport.request;

import static java.util.Objects.*;

import java.io.InputStream;
import java.util.Collection;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpKeyIdList;

import co.codewizards.cloudstore.rest.client.request.AbstractRequest;

public class GetPgpPublicKeys extends AbstractRequest<InputStream> {

	private final PgpKeyIdList pgpKeyIdList;
	private final long changedAfterLocalRevision;

	public GetPgpPublicKeys(final Collection<PgpKeyId> pgpKeyIds, final long changedAfterLocalRevision) {
		requireNonNull(pgpKeyIds, "pgpKeyIds");
		if (pgpKeyIds instanceof PgpKeyIdList)
			this.pgpKeyIdList = (PgpKeyIdList) pgpKeyIds;
		else
			this.pgpKeyIdList = new PgpKeyIdList(pgpKeyIds);

		this.changedAfterLocalRevision = changedAfterLocalRevision;
	}

	@Override
	public InputStream execute() {
		WebTarget webTarget = createWebTarget("_PgpPublicKey", urlEncode(pgpKeyIdList.toString()));

		if (changedAfterLocalRevision >= 0)
			webTarget = webTarget.queryParam("changedAfterLocalRevision", changedAfterLocalRevision);

		final InputStream in = assignCredentials(webTarget.request(MediaType.APPLICATION_OCTET_STREAM_TYPE)).get(InputStream.class);
		return in;
	}

	@Override
	public boolean isResultNullable() {
		return true;
	}
}
