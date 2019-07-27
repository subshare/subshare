package org.subshare.core.sign;

import static co.codewizards.cloudstore.core.util.IOUtil.*;
import static java.util.Objects.*;
import static org.subshare.core.crypto.CryptoConfigUtil.*;
import static org.subshare.core.sign.SignableSigner.*;
import static org.subshare.core.sign.VerifierInputStream.*;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.Signer;
import org.subshare.core.user.UserRepoKey;
import org.subshare.crypto.CryptoRegistry;

public class SignerOutputStream extends FilterOutputStream {

	/**
	 * The first byte in the header identifying the content to be written by the {@link SignerOutputStream}
	 * and thus being readable by the {@link VerifierInputStream}.
	 * <p>
	 * 204 decimal is 11001100 binary.
	 */
	public static final int MAGIC_BYTE = 204;

	private boolean closed;
	private boolean closeUnderlyingStream = true;
	private final UserRepoKey userRepoKey;
	private final SignerTransformation signerTransformation;
	private final Signer signer;
	private final Date signatureCreated;

	public SignerOutputStream(final OutputStream out, final UserRepoKey signingUserRepoKey) throws IOException {
		this(out, signingUserRepoKey, new Date());
	}

	public SignerOutputStream(final OutputStream out, final UserRepoKey signingUserRepoKey, final Date signatureCreated) throws IOException {
		super(out);
		this.userRepoKey = requireNonNull(signingUserRepoKey, "signingUserRepoKey");
		this.signerTransformation = getSignerTransformation();
		this.signatureCreated = signatureCreated;

		try {
			signer = CryptoRegistry.getInstance().createSigner(signerTransformation.getTransformation());
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		signer.init(true, signingUserRepoKey.getKeyPair().getPrivate());

		writeHeader();
	}

	private void writeHeader() throws IOException {
		out.write(MAGIC_BYTE);
		out.write(1); // version

		final int signerTransformationNumeric = signerTransformation.ordinal();
		if (signerTransformationNumeric > MAX_UNSIGNED_2_BYTE_VALUE)
			throw new IllegalStateException("signerTransformationNumeric > " + MAX_UNSIGNED_2_BYTE_VALUE);

		out.write(signerTransformationNumeric);
		out.write(signerTransformationNumeric >>> 8);

		final byte[] signingUserRepoKeyIdBytes = userRepoKey.getUserRepoKeyId().toBytes();
		if (signingUserRepoKeyIdBytes.length != 16)
			throw new IllegalStateException(String.format("signingUserRepoKeyIdBytes.length != 16 :: %s != 16", signingUserRepoKeyIdBytes.length));

		out.write(signingUserRepoKeyIdBytes);

		final byte[] signatureCreatedBytes = longToBytes(signatureCreated.getTime());
		if (signatureCreatedBytes.length != 8)
			throw new IllegalStateException(String.format("signatureCreatedBytes.length != 8 :: %s != 8", signatureCreatedBytes.length));

		signer.update(signatureCreatedBytes, 0, signatureCreatedBytes.length);
		out.write(signatureCreatedBytes);
	}

	private void writeSignature() throws DataLengthException, CryptoException, IOException {
		final byte[] signatureBytes = signer.generateSignature();

		final int footerLength = signatureBytes.length + 4 /* signatureBytesLength */;
		if (footerLength > MAX_FOOTER_LENGTH)
			throw new IllegalStateException(String.format("footerLength > MAX_FOOTER_LENGTH :: %s > %s",
					footerLength, MAX_FOOTER_LENGTH));

		out.write(signatureBytes);

		final int signatureBytesLength = signatureBytes.length;
		out.write(signatureBytesLength);
		out.write(signatureBytesLength >>> 8);
		out.write(signatureBytesLength >>> 16);
		out.write(signatureBytesLength >>> 24);
	}

	@Override
	public void write(final int b) throws IOException {
		assertNotClosed();
		signer.update((byte) b);
		out.write(b);
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		assertNotClosed();
		signer.update(b, off, len);
		out.write(b, off, len);
	}

	@Override
	public void close() throws IOException {
		if (! closed) {
			closed = true;

			try {
				writeSignature();
			} catch (DataLengthException | CryptoException e) {
				throw new IOException(e);
			}

			if (isCloseUnderlyingStream())
				out.close();
		}
	}

	public boolean isCloseUnderlyingStream() {
		return closeUnderlyingStream;
	}

	public void setCloseUnderlyingStream(final boolean closeUnderlyingStream) {
		this.closeUnderlyingStream = closeUnderlyingStream;
	}

	private void assertNotClosed() {
		if (closed)
			throw new IllegalStateException("SignerOutputStream already closed!");
	}
}
