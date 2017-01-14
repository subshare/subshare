package org.subshare.rest.server.service;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;

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
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpKeyIdList;
import org.subshare.core.pgp.transport.PgpTransport;
import org.subshare.core.pgp.transport.PgpTransportFactory;
import org.subshare.core.pgp.transport.PgpTransportFactoryRegistryImpl;
import org.subshare.core.pgp.transport.local.LocalPgpTransportFactory;

@Path("_PgpPublicKey")
public class PgpPublicKeyService {

	@GET
	@Path("_localRevision")
	public LongDto getLocalRevision() {
		try (final PgpTransport localPgpTransport = createLocalPgpTransport();) {
			return new LongDto(localPgpTransport.getLocalRevision());
		}
	}

	@GET
	@Path("_search/{queryString}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response searchPgpPublicKeys(@PathParam("queryString") final String queryString) {
		assertNotNull(queryString, "queryString");

		final StreamingOutput result = new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				try (final PgpTransport localPgpTransport = createLocalPgpTransport();) {
					localPgpTransport.exportPublicKeysMatchingQuery(queryString, castStream(output));
					output.flush();
				}
			}
		};
		return Response.ok(result).build();
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
			@QueryParam("changedAfterLocalRevision") @DefaultValue("-1") final long changedAfterLocalRevision) {
		assertNotNull(pgpKeyIdList, "pgpKeyIdList");

		final StreamingOutput result = new StreamingOutput() {
			@Override
			public void write(OutputStream output) throws IOException, WebApplicationException {
				try (final PgpTransport localPgpTransport = createLocalPgpTransport();) {
					localPgpTransport.exportPublicKeys(new HashSet<PgpKeyId>(pgpKeyIdList), changedAfterLocalRevision, castStream(output));
					output.flush();
				}
			}
		};
		return Response.ok(result).build();
	}

	@PUT
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public void putPgpPublicKeys(final InputStream in) {
		assertNotNull(in, "in");
		try (final PgpTransport localPgpTransport = createLocalPgpTransport();) {
			localPgpTransport.importKeys(castStream(in));
		}
	}

	private PgpTransport createLocalPgpTransport() {
		final PgpTransportFactory localPgpTransportFactory = PgpTransportFactoryRegistryImpl.getInstance().getPgpTransportFactoryOrFail(LocalPgpTransportFactory.class);
		return localPgpTransportFactory.createPgpTransport(LocalPgpTransportFactory.LOCAL_URL);
	}
}
