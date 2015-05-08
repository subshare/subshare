package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.subshare.core.dto.LongDto;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpKeyIdList;
import org.subshare.core.pgp.PgpRegistry;

@Path("_PgpPublicKey")
public class PgpPublicKeyService {

	private final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();

	@GET
	@Path("localRevision")
	public LongDto getLocalRevision() {
		return new LongDto(pgp.getLocalRevision());
	}

	// required! without this method an empty pgpKeyIdList causes an exception, because no matching method is found
	@GET
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getPgpPublicKeys(@QueryParam("changedAfterLocalRevision") @DefaultValue("-1") long changedAfterLocalRevision) {
		return getPgpPublicKeys(new PgpKeyIdList(), changedAfterLocalRevision);
	}

	@GET
	@Path("{pgpKeyIdList}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getPgpPublicKeys(
			@PathParam("pgpKeyIdList") final PgpKeyIdList pgpKeyIdList,
			@QueryParam("changedAfterLocalRevision") @DefaultValue("-1") long changedAfterLocalRevision) {
		assertNotNull("pgpKeyIdList", pgpKeyIdList);

		final Set<PgpKey> pgpKeys = new LinkedHashSet<PgpKey>(pgpKeyIdList.size());
		for (final PgpKeyId pgpKeyId : pgpKeyIdList) {
			final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
			if (pgpKey != null) // TODO filter by changedAfterLocalRevision!
				pgpKeys.add(pgpKey);
		}

		if (pgpKeys.isEmpty())
			return null;


		final StreamingOutput result = new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				pgp.exportPublicKeys(pgpKeys, output);
				output.flush();
			}
		};
		return Response.ok(result).build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public void putPgpPublicKeys(final InputStream in) {
		assertNotNull("in", in);
		pgp.importKeys(in);
	}
}
