package org.subshare.core.crypto;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.core.crypto.EncrypterDecrypterStreamUtil.*;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.subshare.core.dto.CryptoKeyType;
import org.subshare.crypto.Cipher;
import org.subshare.crypto.CipherOperationMode;
import org.subshare.crypto.CryptoRegistry;

public class EncrypterOutputStream extends FilterOutputStream {

	private final CipherTransformation cipherTransformation;
	private final Cipher cipher;
	private final byte[] iv;
	private byte[] cipherBuffer;
	private boolean closed;
	private boolean closeUnderlyingOutputStream = true;

	public EncrypterOutputStream(final OutputStream out,
			final CipherTransformation cipherTransformation,
			final CipherParameters key,
			final IvFactory ivFactory) throws IOException {
		super(assertNotNull("out", out));
		this.cipherTransformation = assertNotNull("cipherTransformation", cipherTransformation);
		assertValidKey(cipherTransformation, key);
		this.cipher = createCipher();

		if (ivFactory == null)
			iv = null;
		else {
			if (CryptoKeyType.asymmetric == cipherTransformation.getType())
				throw new IllegalArgumentException("Cannot use an IV in asymmetric encryption!");

			ivFactory.setCipher(cipher);
			iv = ivFactory.createIv();
			ivFactory.setCipher(null);
		}

		final CipherParameters cipherParameters;
		if (iv == null)
			cipherParameters = key;
		else
			cipherParameters = new ParametersWithIV(key, iv);

		this.cipher.init(CipherOperationMode.ENCRYPT, cipherParameters);
		writeHeader();
	}

	private Cipher createCipher() {
		try {
			return CryptoRegistry.getInstance().createCipher(cipherTransformation.getTransformation());
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new RuntimeException(e);
		}
	}

	private void writeHeader() throws IOException {
		out.write(1); // version

		final int cipherTransformationNumeric = cipherTransformation.ordinal();
		if (cipherTransformationNumeric > MAX_UNSIGNED_2_BYTE_VALUE)
			throw new IllegalStateException("cipherTransformationNumeric > " + MAX_UNSIGNED_2_BYTE_VALUE);

		out.write(cipherTransformationNumeric);
		out.write(cipherTransformationNumeric >>> 8);

		final int ivLength;
		if (iv == null)
			ivLength = 0;
		else
			ivLength = iv.length;

		if (ivLength > MAX_UNSIGNED_2_BYTE_VALUE)
			throw new IllegalStateException("ivLength > " + MAX_UNSIGNED_2_BYTE_VALUE);

		out.write(ivLength);
		out.write(ivLength >>> 8);

		if (ivLength > 0)
			out.write(iv);
	}

	@Override
	public void write(final int b) throws IOException {
		assertNotClosed();
		final int outputSize = cipher.getOutputSize(1);
		ensureCipherBufferMinLength(outputSize);

		final int bytesWritten;
		try {
			bytesWritten = cipher.update((byte) b, cipherBuffer, 0);
		} catch (DataLengthException | IllegalStateException | CryptoException e) {
			throw new IOException(e);
		}
		if (bytesWritten > 0)
			out.write(cipherBuffer, 0, bytesWritten);
	}

	@Override
	public void write(final byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		assertNotClosed();
		final int outputSize = cipher.getOutputSize(len);
		ensureCipherBufferMinLength(outputSize);

		final int bytesWritten;
		try {
			bytesWritten = cipher.update(b, off, len, cipherBuffer, 0);
		} catch (DataLengthException | IllegalStateException | CryptoException e) {
			throw new IOException(e);
		}
		if (bytesWritten > 0)
			out.write(cipherBuffer, 0, bytesWritten);
	}

	@Override
	public void flush() throws IOException {
		// We cannot flush the cipher. Thus we mostly ignore this method.
		out.flush();
	}

	private void assertNotClosed() {
		if (closed)
			throw new IllegalStateException("Cipher already closed!");
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			closed = true;

			final int outputSize = cipher.getOutputSize(0);
			ensureCipherBufferMinLength(outputSize);

			final int bytesWritten;
			try {
				bytesWritten = cipher.doFinal(cipherBuffer, 0);
			} catch (DataLengthException | IllegalStateException | CryptoException e) {
				throw new IOException(e);
			}
			if (bytesWritten > 0)
				out.write(cipherBuffer, 0, bytesWritten);
		}

		if (isCloseUnderlyingOutputStream())
			out.close();
	}

	public boolean isCloseUnderlyingOutputStream() {
		return closeUnderlyingOutputStream;
	}
	public void setCloseUnderlyingOutputStream(final boolean closeUnderlyingOutputStream) {
		this.closeUnderlyingOutputStream = closeUnderlyingOutputStream;
	}

	private void ensureCipherBufferMinLength(final int minLength) {
		if (minLength < 0)
			throw new IllegalArgumentException("minLength < 0");

		if (cipherBuffer == null || cipherBuffer.length < minLength)
			cipherBuffer = new byte[minLength];
	}
}
