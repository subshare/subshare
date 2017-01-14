package org.subshare.rest.client.locker.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.subshare.core.locker.LockerEncryptedDataFile;

import co.codewizards.cloudstore.rest.client.request.VoidRequest;

public class PutLockerEncryptedDataFile extends VoidRequest {

	private final LockerEncryptedDataFile lockerEncryptedDataFile;

	public PutLockerEncryptedDataFile(final LockerEncryptedDataFile lockerEncryptedDataFile) {
		this.lockerEncryptedDataFile = assertNotNull(lockerEncryptedDataFile, "lockerEncryptedDataFile");
	}

	@Override
	protected Response _execute() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			lockerEncryptedDataFile.write(out);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		final byte[] data = out.toByteArray(); out = null;

		return assignCredentials(createWebTarget("_Locker").request())
				.put(Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM_TYPE));
	}
}
