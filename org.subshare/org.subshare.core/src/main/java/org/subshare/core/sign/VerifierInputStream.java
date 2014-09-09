package org.subshare.core.sign;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.subshare.core.user.UserRepoKeyPublicKeyLookup;

import co.codewizards.cloudstore.core.dto.Uid;

public class VerifierInputStream extends FilterInputStream {

	private boolean closed;
	private boolean closeUnderlyingStream = true;
	private final Header header;
	private final UserRepoKeyPublicKeyLookup lookup;

	public VerifierInputStream(final InputStream in, final UserRepoKeyPublicKeyLookup lookup) throws IOException {
		super(in);
		this.lookup = assertNotNull("lookup", lookup);
		header = readHeader();
	}

	protected static class Header {
		public final int version;
		public final SignerTransformation signerTransformation;
		public final Uid signingUserRepoKeyId;
		public final Date signatureCreated;
		public Header(final int version, final SignerTransformation signerTransformation, final Uid signingUserRepoKeyId, final Date signatureCreated) {
			this.version = version;
			this.signerTransformation = assertNotNull("signerTransformation", signerTransformation);
			this.signingUserRepoKeyId = assertNotNull("signingUserRepoKeyId", signingUserRepoKeyId);
			this.signatureCreated = assertNotNull("signatureCreated", signatureCreated);
		}
	}

	private Header readHeader() throws IOException {
		final int version = in.read();
		if (version != 1)
			throw new IOException("version != 1 :: version == " + version);

		final int signerTransformationNumeric = readOrFail(in) + (readOrFail(in) << 8);
		if (signerTransformationNumeric > SignerTransformation.values().length) {
			throw new IOException(String.format(
					"signerTransformationNumeric > SignerTransformation.values().length :: %s > %s",
					signerTransformationNumeric, SignerTransformation.values().length));
		}
		final SignerTransformation signerTransformation = SignerTransformation.values()[signerTransformationNumeric];

		final byte[] signingUserRepoKeyIdBytes = new byte[16];
		readOrFail(in, signingUserRepoKeyIdBytes, 0, signingUserRepoKeyIdBytes.length);
		final Uid signingUserRepoKeyId = new Uid(signingUserRepoKeyIdBytes);

		final byte[] signatureCreatedBytes = new byte[8];
		readOrFail(in, signatureCreatedBytes, 0, signatureCreatedBytes.length);
		final Date signatureCreated = new Date(bytesToLong(signatureCreatedBytes));

		return new Header(version, signerTransformation, signingUserRepoKeyId, signatureCreated);
	}

	@Override
	public int read() throws IOException {
		assertNotClosed();
		return in.read();
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		assertNotClosed();
		return in.read(b, off, len);
	}

	@Override
	public long skip(final long n) throws IOException {
		assertNotClosed();
		return in.skip(n);
	}

	@Override
	public int available() throws IOException {
		assertNotClosed();
		return in.available();
	}

	@Override
	public void close() throws IOException {
		if (! closed) {
			closed = true;

			if (isCloseUnderlyingStream())
				in.close();
		}
	}

	@Override
	public void mark(final int readlimit) { }

	@Override
	public void reset() throws IOException {
		throw new IOException("mark/reset not supported");
	}

	@Override
	public boolean markSupported() {
		return false;
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
