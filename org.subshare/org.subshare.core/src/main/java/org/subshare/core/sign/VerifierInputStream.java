package org.subshare.core.sign;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.bouncycastle.crypto.Signer;
import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.core.user.UserRepoKeyPublicKeyLookup;
import org.subshare.crypto.CryptoRegistry;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.Uid;

public class VerifierInputStream extends FilterInputStream {

	/**
	 * Maximum length of the footer.
	 * <p>
	 * The footer is composed of signature and signature-length at the end of the stream.
	 * <p>
	 * Because the footer is at the end (we cannot sign before having processed the entire stream and we
	 * want to avoid a 2-pass-process), we must read-ahead until we hit the end-of-stream.
	 */
	protected static int MAX_FOOTER_LENGTH = 4 * 1024;

	private boolean closed;
	private boolean closeUnderlyingStream = true;
	private final Header header;
	private Footer footer;
	private long offset;
	private final Signer signer;

	public VerifierInputStream(final InputStream in, final UserRepoKeyPublicKeyLookup lookup) throws IOException {
		super(new BufferedInputStream(in));
		assertNotNull("lookup", lookup);
		header = readHeader();

		try {
			signer = CryptoRegistry.getInstance().createSigner(header.signerTransformation.getTransformation());
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		final PublicKey userRepoKeyPublicKey = lookup.getUserRepoKeyPublicKey(header.signingUserRepoKeyId);
		if (userRepoKeyPublicKey == null)
			throw new SignatureException(String.format("No public key found for signingUserRepoKeyId=%s!", header.signingUserRepoKeyId));

		signer.init(false, userRepoKeyPublicKey.getPublicKey());

		final byte[] signatureCreatedBytes = longToBytes(header.signatureCreated.getTime());
		signer.update(signatureCreatedBytes, 0, signatureCreatedBytes.length);
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

	protected static class Footer {
		public final long signatureBytesOffset;
		public final byte[] signatureBytes;

		public Footer(final long signatureBytesOffset, final byte[] signatureBytes) {
			this.signatureBytesOffset = signatureBytesOffset;
			this.signatureBytes = assertNotNull("signatureBytes", signatureBytes);
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
		if (readFooterIfNearAhead(1) < 1) {
			verify();
			return -1;
		}

		final int read = in.read();

		if (read >= 0) {
			++offset;
			signer.update((byte) read);
		}

		return read;
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		assertNotClosed();

		final int l = readFooterIfNearAhead(len);
		if (l < 1) {
			verify();
			return -1;
		}

		final int read = in.read(b, off, l);

		if (read > 0) {
			offset += read;
			signer.update(b, 0, read);
		}

		return read;
	}

	private int readFooterIfNearAhead(final int bytesToReadExcludingFooter) throws IOException {
		if (footer == null) {
			final int bytesToSkipTotal = MAX_FOOTER_LENGTH + bytesToReadExcludingFooter;
			in.mark(bytesToSkipTotal);

			int bytesSkippedTotal = 0;
			int bytesSkipped = 0;
			int bytesToSkipRemaining;
			do {
				if (bytesSkipped > 0)
					bytesSkippedTotal += bytesSkipped;

				bytesToSkipRemaining = bytesToSkipTotal - bytesSkippedTotal - 1;
			} while (bytesToSkipRemaining > 0 && 0 < (bytesSkipped = (int) in.skip(bytesToSkipRemaining)));

			if (bytesToSkipRemaining > 0) {
				for (int i = 0; i < bytesToSkipRemaining; ++i) {
					if (in.read() < 0)
						break;

					++bytesSkippedTotal;
				}
			}

			if (in.read() >= 0) {
				// Not yet end-of-stream => return.
				in.reset();
				return bytesToReadExcludingFooter;
			}


			// We are now at end-of-stream => (1) reset, (2) seek to EOF-4 and (3) read 4 bytes signature-length.
			// (1) reset (and mark again)
			in.reset();
			in.mark(bytesToSkipTotal);

			// (2) seek to EOF-4 => calculate offset from current position.
			final int signatureLengthOffset = bytesSkippedTotal - 4;
			_skipOrFail(signatureLengthOffset);

			// (3) read 4 bytes signature-length
			final int signatureBytesLength = readOrFail(in) + (readOrFail(in) << 8) + (readOrFail(in) << 16) + (readOrFail(in) << 24);

			// Assert, that we really are at the end of the stream.
			if (in.read() >= 0)
				throw new IllegalStateException("Not at end-of-stream!");


			// Now, we (1) reset again, (2) seek to the offset of the signature (3) read the signature.
			// (1)
			in.reset();
			in.mark(bytesToSkipTotal);

			// (2)
			final int signatureBytesOffset = signatureLengthOffset - signatureBytesLength;
			_skipOrFail(signatureBytesOffset);

			// (3)
			final byte[] signatureBytes = new byte[signatureBytesLength];
			readOrFail(in, signatureBytes, 0, signatureBytesLength);


			footer = new Footer(
					offset + signatureBytesOffset, // make the local signatureBytesOffset (relative to 'offset') to a global offset (relative to the beginning of the stream)
					signatureBytes);
			in.reset();
		}
		return Math.min(bytesToReadExcludingFooter, (int) (footer.signatureBytesOffset - offset));
	}

	private void _skipOrFail(final int bytesToSkip) throws IOException {
		int bytesToSkipRemaining;
		int bytesSkipped = 0;
		int bytesSkippedTotal = 0;
		do {
			if (bytesSkipped > 0)
				bytesSkippedTotal += bytesSkipped;

			bytesToSkipRemaining = bytesToSkip - bytesSkippedTotal;
		} while (bytesToSkipRemaining > 0 && 0 < (bytesSkipped = (int) in.skip(bytesToSkipRemaining)));

		if (bytesToSkipRemaining > 0) {
			for (int i = 0; i < bytesToSkipRemaining; ++i)
				readOrFail(in);
		}

		if (bytesSkippedTotal != bytesToSkip)
			throw new IllegalStateException(String.format("bytesSkippedTotal != bytesToSkip :: %s != %s", bytesSkippedTotal, bytesToSkip));
	}

	private static final int MAX_SKIP_BUFFER_SIZE = 2048;

	@Override
	public long skip(final long n) throws IOException {
		assertNotClosed();
		long remaining = n;
		int nr;

		if (n <= 0) {
			return 0;
		}

		final int size = (int)Math.min(MAX_SKIP_BUFFER_SIZE, remaining);
		final byte[] skipBuffer = new byte[size];
		while (remaining > 0) {
			nr = read(skipBuffer, 0, (int)Math.min(size, remaining));
			if (nr < 0) {
				break;
			}
			remaining -= nr;
		}

		return n - remaining;
	}

	@Override
	public int available() throws IOException {
		assertNotClosed();
		final int available = in.available();
		return readFooterIfNearAhead(Math.min(available, 16 * 1024));
	}

	@Override
	public void close() throws IOException {
		if (! closed) {
			closed = true;

			if (isCloseUnderlyingStream())
				in.close();
		}
	}

	private void verify() {
		if (footer == null)
			throw new IllegalStateException("Stream not completely read! Did not even encounter footer, yet!");

		if (offset != footer.signatureBytesOffset)
			throw new IllegalStateException("Stream not completely read! offset != footer.signatureBytesOffset");

		if (!signer.verifySignature(footer.signatureBytes))
			throw new SignatureException("Signature not valid!");
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
