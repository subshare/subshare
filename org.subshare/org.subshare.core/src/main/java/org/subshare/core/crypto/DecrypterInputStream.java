package org.subshare.core.crypto;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.core.crypto.EncrypterDecrypterStreamUtil.*;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.subshare.crypto.Cipher;
import org.subshare.crypto.CipherOperationMode;
import org.subshare.crypto.CryptoRegistry;

import co.codewizards.cloudstore.core.util.IOUtil;

/**
 * {@code DecrypterInputStream} uses either symmetric or asymmetric cryptography to decrypt a given
 * cipher-text.
 * <p>
 * Data encrypted with an instance of {@link EncrypterOutputStream} can be decrypted with an instance of
 * this class.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 * @see EncrypterOutputStream
 * @see AsymCombiDecrypterInputStream
 */
public class DecrypterInputStream extends FilterInputStream {

	private final Header header;
	private final Cipher cipher;
	private byte[] readBuffer;
	private byte[] cipherBuffer;
	private boolean cipherFinalized;
	private final byte[] singleByteArray = new byte[1];
	private int cipherBufferReadOff = 0;
	private int cipherBufferUsedLen = 0;
	private boolean closed;
	private boolean closeUnderlyingStream = true;

	public DecrypterInputStream(final InputStream in, final CipherParameters key) throws IOException {
		super(assertNotNull("in", in));
		this.header = readHeader();
		assertValidKey(header.cipherTransformation, key);
		this.cipher = createCipher();

		final CipherParameters cipherParameters;
		if (header.iv == null)
			cipherParameters = key;
		else
			cipherParameters = new ParametersWithIV(key, header.iv);

		this.cipher.init(CipherOperationMode.DECRYPT, cipherParameters);
	}

	private Cipher createCipher() {
		try {
			return CryptoRegistry.getInstance().createCipher(header.cipherTransformation.getTransformation());
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new RuntimeException(e);
		}
	}

	protected static class Header {
		public final int version;
		public final CipherTransformation cipherTransformation;
		public final byte[] iv;
		public Header(final int version, final CipherTransformation cipherTransformation, final byte[] iv) {
			this.version = version;
			this.cipherTransformation = assertNotNull("cipherTransformation", cipherTransformation);
			this.iv = iv;
		}
	}

	private Header readHeader() throws IOException {
		final int version = in.read();
		if (version != 1)
			throw new IOException("version != 1 :: version == " + version);

		final int cipherTransformationNumeric = readOrFail() + (readOrFail() << 8);
		if (cipherTransformationNumeric > CipherTransformation.values().length) {
			throw new IOException(String.format(
					"cipherTransformationNumeric > CipherTransformation.values().length :: %s > %s",
					cipherTransformationNumeric, CipherTransformation.values().length));
		}
		final CipherTransformation cipherTransformation = CipherTransformation.values()[cipherTransformationNumeric];

		final int ivLength = readOrFail() + (readOrFail() << 8);
		final byte[] iv;
		if (ivLength < 0)
			throw new IOException("ivLength < 0");
		else if (ivLength == 0)
			iv = null;
		else {
			iv = new byte[ivLength];
			readOrFail(iv, 0, ivLength);
		}
		return new Header(version, cipherTransformation, iv);
	}

	public byte[] getIv() {
		return header.iv;
	}

	@Override
	public int read() throws IOException {
		assertNotClosed();
		int bytesRead;
		while ((bytesRead = read(singleByteArray, 0, singleByteArray.length)) == 0) { }
		if (bytesRead < 0)
			return -1;
		return singleByteArray[0] & 0xff;
	}

	@Override
	public int read(final byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		assertNotClosed();

		if (len == 0)
			return 0;

		int readFromCipherBuffer = readFromCipherBuffer(b, off, len);
		if (readFromCipherBuffer > 0)
			return readFromCipherBuffer;

		int consecutiveEmptyReadCount = 0;

		while (true) {
			if (cipherFinalized)
				return -1;

			ensureReadBufferMinLength(len);
			final int bytesRead = in.read(readBuffer, 0, len);
			if (bytesRead == 0) {
				if (++consecutiveEmptyReadCount > 5)
					throw new IOException(String.format("Encountered %s consecutive empty read operations (but no end-of-stream)!", consecutiveEmptyReadCount));
			}
			else {
				consecutiveEmptyReadCount = 0;
				if (bytesRead > 0)
					ensureCipherBufferMinLength(cipher.getOutputSize(bytesRead));

				cipherBufferReadOff = 0;
				cipherBufferUsedLen = 0;
				final int bytesWritten;
				try {
					if (bytesRead > 0)
						bytesWritten = cipher.update(readBuffer, 0, bytesRead, cipherBuffer, 0);
					else {
						cipherFinalized = true;
						bytesWritten = cipher.doFinal(cipherBuffer, 0);
					}
				} catch (DataLengthException | IllegalStateException | CryptoException e) {
					throw new IOException(e);
				}
				if (bytesWritten > 0)
					cipherBufferUsedLen += bytesWritten;
			}

			readFromCipherBuffer = readFromCipherBuffer(b, off, len);
			if (readFromCipherBuffer <= 0 && bytesRead < 0)
				return -1; // end-of-stream

			if (readFromCipherBuffer > 0)
				return readFromCipherBuffer;
		}
	}

	protected int readFromCipherBuffer(final byte[] b, final int off, final int len) throws IOException {
		if (cipherBufferUsedLen > 0) {
			final int length = Math.min(len, cipherBufferUsedLen);
			System.arraycopy(cipherBuffer, cipherBufferReadOff, b, off, length);
			cipherBufferReadOff += length;
			cipherBufferUsedLen -= length;

			if (cipherBufferUsedLen < 0)
				throw new IllegalStateException("cipherBufferUsedLen < 0");

			if (cipherBufferUsedLen == 0)
				cipherBufferReadOff = 0;

			return length;
		}
		return 0;
	}

	@Override
	public long skip(final long n) throws IOException {
		assertNotClosed();
		if (n <= 0)
			return 0;

		final byte[] buffer = new byte[(int) Math.min(n, 16 * 1024)];
		long bytesToSkip = n;
		while (bytesToSkip > 0) {
			final int len = (int) Math.min(bytesToSkip, buffer.length);
			final int bytesRead = read(buffer, 0, len);
			if (bytesRead < 0)
				break;

			bytesToSkip -= bytesRead;
			if (bytesToSkip < 0)
				throw new IllegalStateException("bytesToSkip < 0");
		}
		return n - bytesToSkip;
	}

	@Override
	public int available() throws IOException {
		assertNotClosed();
		final int cipherOutputSize = cipher.getOutputSize(0);

		if (cipherBufferUsedLen > 0)
			return cipherBufferUsedLen;

		if (cipherOutputSize > 0)
			return cipherOutputSize;

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

	private void ensureCipherBufferMinLength(final int minLength) {
		if (minLength < 0)
			throw new IllegalArgumentException("minLength < 0");

		if (cipherBuffer == null || cipherBuffer.length < minLength)
			cipherBuffer = new byte[minLength];
	}

	private void ensureReadBufferMinLength(final int minLength) {
		if (minLength <= 0)
			throw new IllegalArgumentException("minLength <= 0");

		if (readBuffer == null || readBuffer.length < minLength)
			readBuffer = new byte[minLength];
	}

	private int readOrFail() throws IOException {
		return IOUtil.readOrFail(in);
	}

	private void readOrFail(final byte[] buf, final int off, final int len) throws IOException {
		IOUtil.readOrFail(in, buf, off, len);
	}

	private void assertNotClosed() {
		if (closed)
			throw new IllegalStateException("DecrypterInputStream already closed!");
	}

}
