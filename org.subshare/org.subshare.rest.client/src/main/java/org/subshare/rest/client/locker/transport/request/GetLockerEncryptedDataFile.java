package org.subshare.rest.client.locker.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.subshare.core.locker.LockerEncryptedDataFile;
import org.subshare.core.pgp.PgpKeyId;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.rest.client.request.AbstractRequest;

public class GetLockerEncryptedDataFile extends AbstractRequest<LockerEncryptedDataFile> {

	private final PgpKeyId pgpKeyId;
	private final String lockerContentName;
	private final Uid lockerContentVersion;

	public GetLockerEncryptedDataFile(final PgpKeyId pgpKeyId, final String lockerContentName, final Uid lockerContentVersion) {
		this.pgpKeyId = assertNotNull(pgpKeyId, "pgpKeyId");
		this.lockerContentName = assertNotNull(lockerContentName, "lockerContentName");
		this.lockerContentVersion = assertNotNull(lockerContentVersion, "lockerContentVersion");
	}

	@Override
	public LockerEncryptedDataFile execute() {
		final WebTarget webTarget = createWebTarget("_Locker",
				urlEncode(pgpKeyId.toString()), urlEncode(lockerContentName), urlEncode(lockerContentVersion.toString()));

		InputStream in;
		try {
			in = assignCredentials(webTarget.request(MediaType.APPLICATION_OCTET_STREAM_TYPE)).get(InputStream.class);
		} catch (final WebApplicationException x) {
			if (x.getResponse().getStatus() == Status.NOT_FOUND.getStatusCode())
				return null;

			throw x;
		}
		try {
			try {
				if (!in.markSupported())
					in = new BufferedInputStream(in);

				in.mark(10);

				if (in.read() < 0)
					return null;

				in.reset();

				final LockerEncryptedDataFile result = new LockerEncryptedDataFile(in);
				return result;
			} finally {
				in.close();
			}
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	@Override
	public boolean isResultNullable() {
		return true;
	}
}
