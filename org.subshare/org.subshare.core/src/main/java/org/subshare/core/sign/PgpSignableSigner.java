package org.subshare.core.sign;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static org.subshare.core.sign.SignableSigner.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.io.NullOutputStream;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpEncoder;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;

public class PgpSignableSigner {

	private final Pgp pgp;
	private final PgpKey pgpKey;

	public PgpSignableSigner(final PgpKey pgpKey) {
		this(PgpRegistry.getInstance().getPgpOrFail(), pgpKey);
	}

	public PgpSignableSigner(final Pgp pgp, final PgpKey pgpKey) {
		this.pgp = assertNotNull("pgp", pgp);
		this.pgpKey = assertNotNull("pgpKey", pgpKey);
	}

	public void sign(final PgpSignable pgpSignable) {
		assertNotNull("pgpSignable", pgpSignable);

		final String signedDataType = pgpSignable.getSignedDataType();
		if (isEmpty(signedDataType))
			throw new IllegalArgumentException(String.format("Implementation error in class %s: pgpSignable.getSignedDataType() returned null! %s", pgpSignable.getClass().getName(), pgpSignable));

		final int signedDataVersion = pgpSignable.getSignedDataVersion();
		if (signedDataVersion > MAX_UNSIGNED_2_BYTE_VALUE)
			throw new IllegalStateException("signedDataVersion > " + MAX_UNSIGNED_2_BYTE_VALUE);

		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.write(1); // version

			out.write(signedDataVersion);
			out.write(signedDataVersion >>> 8);

			final ByteArrayOutputStream signatureOut = new ByteArrayOutputStream();

			final ByteArrayOutputStream additionalSignedDataOut = new ByteArrayOutputStream();
			additionalSignedDataOut.write(signedDataType.getBytes(StandardCharsets.UTF_8));

			try (final InputStream signedDataInputStream = pgpSignable.getSignedData(signedDataVersion);) {
				try (final InputStream in = new MultiInputStream(
						InputStreamSource.Helper.createInputStreamSource(new ByteArrayInputStream(additionalSignedDataOut.toByteArray())),
						InputStreamSource.Helper.createInputStreamSource(signedDataInputStream));) {

					final PgpEncoder pgpEncoder = pgp.createEncoder(in, new NullOutputStream());
					pgpEncoder.setSignPgpKey(pgpKey);
					pgpEncoder.setSignOutputStream(signatureOut);
					pgpEncoder.encode();
				}
			}

			final byte[] signatureBytes = signatureOut.toByteArray();

			final int signatureBytesLength = signatureBytes.length;
			out.write(signatureBytesLength);
			out.write(signatureBytesLength >>> 8);
			out.write(signatureBytesLength >>> 16);
			out.write(signatureBytesLength >>> 24);
			out.write(signatureBytes);

			pgpSignable.setPgpSignatureData(out.toByteArray());
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}
}
