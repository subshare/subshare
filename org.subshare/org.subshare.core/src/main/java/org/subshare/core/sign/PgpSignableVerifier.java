package org.subshare.core.sign;

import static co.codewizards.cloudstore.core.util.IOUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.io.NullOutputStream;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpDecoder;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.PgpSignature;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;

public class PgpSignableVerifier {

	private final Pgp pgp;

	public PgpSignableVerifier() {
		this(PgpRegistry.getInstance().getPgpOrFail());
	}

	public PgpSignableVerifier(final Pgp pgp) {
		this.pgp = requireNonNull(pgp, "pgp");
	}

	public PgpSignature verify(final PgpSignable pgpSignable) throws SignatureException {
		final byte[] pgpSignatureData = requireNonNull(pgpSignable, "pgpSignable").getPgpSignatureData();
		if (pgpSignatureData == null)
			throw new SignatureException("There is no signature! pgpSignable.pgpSignatureData == null");

		final String signedDataType = pgpSignable.getSignedDataType();
		if (isEmpty(signedDataType))
			throw new IllegalArgumentException(String.format("Implementation error in class %s: pgpSignable.getSignedDataType() returned null! %s", pgpSignable.getClass().getName(), pgpSignable));

		try {
			final ByteArrayInputStream in1 = new ByteArrayInputStream(pgpSignatureData);
			final int version = in1.read();
			if (version != 1)
				throw new SignatureException(String.format("pgpSignatureData has unsupported version=%s!", version));

			final int signedDataVersion = readOrFail(in1) + (readOrFail(in1) << 8);

			final int signatureBytesLength = readOrFail(in1) + (readOrFail(in1) << 8) + (readOrFail(in1) << 16) + (readOrFail(in1) << 24);
			final byte[] signatureBytes = new byte[signatureBytesLength];
			readOrFail(in1, signatureBytes, 0, signatureBytesLength);

			final ByteArrayOutputStream additionalSignedDataOut = new ByteArrayOutputStream();
			additionalSignedDataOut.write(signedDataType.getBytes(StandardCharsets.UTF_8));

			try (InputStream signedDataInputStream = pgpSignable.getSignedData(signedDataVersion);) {

				try (final MultiInputStream in = new MultiInputStream(
						InputStreamSource.Helper.createInputStreamSource(new ByteArrayInputStream(additionalSignedDataOut.toByteArray())),
						InputStreamSource.Helper.createInputStreamSource(signedDataInputStream));) {

					final PgpDecoder decoder = pgp.createDecoder(in, new NullOutputStream());
					decoder.setSignInputStream(new ByteArrayInputStream(signatureBytes));
					decoder.decode();

					final PgpSignature pgpSignature = decoder.getPgpSignature();
					// decoder.decode() should already have thrown an exception, if pgpSignature is null, but we should
					// better check ourselves, too - we might only throw a different exception, here (e.g. IllegalStateException).
					if (pgpSignature == null)
						throw new SignatureException("Missing signature!");

					return pgpSignature;
				}
			}
		} catch (final IOException x) {
			throw new SignatureException(x);
		}
	}

}
