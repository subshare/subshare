package org.subshare.core.crypto;

import static java.util.Objects.*;
import static org.subshare.core.crypto.EncrypterDecrypterStreamUtil.*;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.subshare.core.dto.CryptoKeyType;
import org.subshare.crypto.Cipher;
import org.subshare.crypto.CipherOperationMode;
import org.subshare.crypto.CryptoRegistry;

/**
 * {@code EncrypterOutputStream} uses either symmetric or asymmetric cryptography to encrypt a given
 * plain-text.
 * <p>
 * When using symmetric cryptography, an instance of this class can encrypt an unlimited amount of data.
 * <p>
 * But asymmetric cryptography is not able to handle large amounts of data. For example,
 * RSA/ECB/OAEPWITHSHA1ANDMGF1PADDING is able to handle a maximum of 740 bytes when using a 4096 bit RSA key.
 * It is therefore common to encrypt the actual payload with a random one-time-key and to encrypt this key
 * with the asymmetric encryption. This class, however, does <i>not</i> do this! If you need such a combined
 * asymmetric+symmetric approach, you must use {@link AsymCombiEncrypterOutputStream} instead!
 * <p>
 * The header written to the underlying {@code OutputStream} contains the information which algorithm was
 * used to encrypt the data. This information is thus not needed to be kept separately for decryption. Solely
 * the information which key can decrypt the data needs to be managed outside.
 * <p>
 * Data encrypted with an instance of this class can be decrypted with an instance of
 * {@link DecrypterInputStream}.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 * @see DecrypterInputStream
 * @see AsymCombiEncrypterOutputStream
 */
public class EncrypterOutputStream extends FilterOutputStream {

	/**
	 * The first byte in the header identifying the content to be written by the {@link EncrypterOutputStream}
	 * and thus being readable by the {@link DecrypterInputStream}.
	 * <p>
	 * 60 decimal is 00111100 binary.
	 */
	public static final int MAGIC_BYTE = 60;

	private final CipherTransformation cipherTransformation;
	private final Cipher cipher;
	private final byte[] iv;
	private byte[] cipherBuffer;
	private boolean closed;
	private boolean closeUnderlyingStream = true;

	/**
	 * Convencience constructor to be used for asymmetric encryption or symmetric encryption without IV.
	 * <p>
	 * Do not use this constructor for symmetric encryption, if the same key may be used to encrypt multiple
	 * different plain-texts! Using the same key multiple times without an IV is extremely insecure! An
	 * attacker might easily calculate your secret key, if you ignore this warning!
	 * @param out the underlying {@code OutputStream}. Must not be <code>null</code>. A non-sensitive header
	 * and encrypted data only is written to it.
	 * @param cipherTransformation the encryption transformation which can be either asymmetric or symmetric.
	 * Must not be <code>null</code>.
	 * @param key the key used for encryption. Must not be <code>null</code>. This key must be compatible to
	 * {@code cipherTransformation}. For symmetric cryptography, it must be an instance of
	 * {@link KeyParameter}. For asymmetric cryptography, it must be a public key and an instance of
	 * {@link AsymmetricKeyParameter}.
	 * @throws IOException if writing the header to the underlying {@code OutputStream} fails.
	 */
	public EncrypterOutputStream(final OutputStream out,
			final CipherTransformation cipherTransformation,
			final CipherParameters key) throws IOException {
		this(out, cipherTransformation, key, null);
	}

	/**
	 * Creates an instance of {@code EncrypterOutputStream}.
	 * @param out the underlying {@code OutputStream}. Must not be <code>null</code>. A non-sensitive header
	 * and encrypted data only is written to it.
	 * @param cipherTransformation the encryption transformation which can be either asymmetric or symmetric.
	 * Must not be <code>null</code>.
	 * @param key the key used for encryption. Must not be <code>null</code>. This key must be compatible to
	 * {@code cipherTransformation}. For symmetric cryptography, it must be an instance of
	 * {@link KeyParameter}. For asymmetric cryptography, it must be a public key and an instance of
	 * {@link AsymmetricKeyParameter}.
	 * @param ivFactory the IV factory. May be <code>null</code> to <i>not</i> use an IV (which is equivalent
	 * to an IV containing exclusively 0). When using asymmetric cryptography, this must be <code>null</code>.
	 * When using symmetric cryptography, this should be <code>null</code>, if the given {@code key} is used
	 * only once. If the same key is used multiple times, you must use an IV, because encrypting multiple
	 * different plain-texts with the same key and without IV is extremely insecure! An attacker might easily
	 * calculate your secret key, if you ignore this warning!
	 * @throws IOException if writing the header to the underlying {@code OutputStream} fails.
	 */
	public EncrypterOutputStream(final OutputStream out,
			final CipherTransformation cipherTransformation,
			final CipherParameters key,
			final IvFactory ivFactory) throws IOException {
		super(requireNonNull(out, "out"));
		this.cipherTransformation = requireNonNull(cipherTransformation, "cipherTransformation");
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
		out.write(MAGIC_BYTE);
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
			throw new IllegalStateException("EncrypterOutputStream already closed!");
	}

	@Override
	public void close() throws IOException {
		if (! closed) {
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

	private void ensureCipherBufferMinLength(final int minLength) {
		if (minLength < 0)
			throw new IllegalArgumentException("minLength < 0");

		if (cipherBuffer == null || cipherBuffer.length < minLength)
			cipherBuffer = new byte[minLength];
	}
}
