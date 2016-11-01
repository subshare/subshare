package org.subshare.core.crypto;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.core.crypto.EncrypterDecrypterStreamUtil.*;

import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.dto.CryptoKeyType;

/**
 * {@code AsymCombiEncrypterOutputStream} combines asymmetric and symmetric cryptography to encrypt a given plain-text.
 * <p>
 * Asymmetric encryption can only be used to encrypt a very small piece of data. For example,
 * RSA/ECB/OAEPWITHSHA1ANDMGF1PADDING is able to handle a maximum of 740 bytes when using a 4096 bit RSA key.
 * It is therefore common to encrypt the actual payload with a random one-time-key and to encrypt this key
 * with the asymmetric encryption.
 * <p>
 * An instance of {@code AsymCombiEncrypterOutputStream} does exactly this: It obtains a symmetric key from
 * a {@link KeyParameterFactory}, encrypts this with the given public key, writes this as a header and then
 * appends the actual payload - encrypted symmetrically.
 * <p>
 * The header written to the underlying {@code OutputStream} contains the information which algorithm was
 * used to encrypt the data. This information is thus not needed to be kept separately for decryption. Solely
 * the information which key can decrypt the data needs to be managed outside.
 * <p>
 * Data encrypted with an instance of this class can be decrypted with an instance of
 * {@link AsymCombiDecrypterInputStream}.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 * @see AsymCombiDecrypterInputStream
 * @see EncrypterOutputStream
 */
public class AsymCombiEncrypterOutputStream extends FilterOutputStream {

	/**
	 * The first byte in the header identifying the content to be written by the {@link AsymCombiEncrypterOutputStream}
	 * and thus being readable by the {@link AsymCombiDecrypterInputStream}.
	 * <p>
	 * 195 decimal is 11000011 binary.
	 */
	public static final int MAGIC_BYTE = 195;

	private final CipherTransformation asymmetricCipherTransformation;
	private final AsymmetricKeyParameter publicKey;
	private final KeyParameter symmetricKey;
	private final EncrypterOutputStream symOut;

	/**
	 * Creates an instance of {@code AsymCombiEncrypterOutputStream}.
	 * @param out the underlying {@code OutputStream}. Must not be <code>null</code>. A non-sensitive header
	 * and encrypted data only is written to it.
	 * @param asymmetricCipherTransformation the asymmetric encryption transformation. Must not be <code>null</code>.
	 * This algorithm is used to encrypt a symmetric key (which is obtained from the given {@code keyParameterFactory}).
	 * @param publicKey the asymmetric key used for encryption. Must not be <code>null</code>. This key must be
	 * compatible to {@code asymmetricCipherTransformation}. This key is not sensitive and may be known to the
	 * world (it's called "public" for a reason).
	 * @param symmetricCipherTransformation the symmetric encryption transformation. Must not be <code>null</code>.
	 * This algorithm is used to encrypt the actual data.
	 * @param keyParameterFactory the factory providing the symmetric key. Must not be <code>null</code>.
	 * @throws IOException if writing the header to the underlying {@code OutputStream} fails.
	 */
	public AsymCombiEncrypterOutputStream(final OutputStream out,
			final CipherTransformation asymmetricCipherTransformation,
			final AsymmetricKeyParameter publicKey,
			final CipherTransformation symmetricCipherTransformation,
			final KeyParameterFactory keyParameterFactory) throws IOException
	{
		this(out, asymmetricCipherTransformation, publicKey, symmetricCipherTransformation, keyParameterFactory, null);
	}

	/**
	 * Creates an instance of {@code AsymCombiEncrypterOutputStream}.
	 * @param out the underlying {@code OutputStream}. Must not be <code>null</code>. A non-sensitive header
	 * and encrypted data only is written to it.
	 * @param asymmetricCipherTransformation the asymmetric encryption transformation. Must not be <code>null</code>.
	 * This algorithm is used to encrypt a symmetric key (which is obtained from the given {@code keyParameterFactory}).
	 * @param publicKey the asymmetric key used for encryption. Must not be <code>null</code>. This key must be
	 * compatible to {@code asymmetricCipherTransformation}.
	 * @param symmetricCipherTransformation the symmetric encryption transformation. Must not be <code>null</code>.
	 * This algorithm is used to encrypt the actual data.
	 * @param keyParameterFactory the factory providing the symmetric key. Must not be <code>null</code>.
	 * @param ivFactory the IV factory. May be <code>null</code>. Since the key generated by the given
	 * {@code keyParameterFactory} should be unique, we don't need an IV and it is therefore recommended to
	 * leave this <code>null</code>. An IV is only necessary, if the same key is used multiple times.
	 * @throws IOException if writing the header to the underlying {@code OutputStream} fails.
	 */
	public AsymCombiEncrypterOutputStream(final OutputStream out,
			final CipherTransformation asymmetricCipherTransformation,
			final AsymmetricKeyParameter publicKey,
			final CipherTransformation symmetricCipherTransformation,
			final KeyParameterFactory keyParameterFactory,
			final IvFactory ivFactory) throws IOException
	{
		super(assertNotNull("out", out));
		this.asymmetricCipherTransformation = assertNotNull("asymmetricCipherTransformation", asymmetricCipherTransformation);
		if (CryptoKeyType.asymmetric != asymmetricCipherTransformation.getType())
			throw new IllegalArgumentException("asymmetric != asymmetricCipherTransformation.type");

		this.publicKey = assertNotNull("publicKey", publicKey);

		assertNotNull("symmetricCipherTransformation", symmetricCipherTransformation);
		if (CryptoKeyType.symmetric != symmetricCipherTransformation.getType())
			throw new IllegalArgumentException("symmetric != symmetricCipherTransformation.type");

		assertNotNull("keyParameterFactory", keyParameterFactory).setCipherTransformation(symmetricCipherTransformation);
		this.symmetricKey = keyParameterFactory.createKeyParameter();
		keyParameterFactory.setCipherTransformation(null);

		writeHeader();

		symOut = new EncrypterOutputStream(out, symmetricCipherTransformation, symmetricKey, ivFactory);
	}

	private void writeHeader() throws IOException {
		out.write(MAGIC_BYTE);
		out.write(1); // version

		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		final EncrypterOutputStream asymOut = new EncrypterOutputStream(
				bout, asymmetricCipherTransformation, publicKey, null);
		asymOut.setCloseUnderlyingStream(false);
		try {
			asymOut.write(symmetricKey.getKey());
		} finally {
			asymOut.close();
		}
		final byte[] encryptedSymmetricKey = bout.toByteArray();
		if (encryptedSymmetricKey.length > MAX_UNSIGNED_2_BYTE_VALUE)
			throw new IllegalStateException("encryptedSymmetricKey.length > " + MAX_UNSIGNED_2_BYTE_VALUE);

		out.write(encryptedSymmetricKey.length);
		out.write(encryptedSymmetricKey.length >>> 8);
		out.write(encryptedSymmetricKey);
	}

	@Override
	public void write(final int b) throws IOException {
		symOut.write(b);
	}

	@Override
	public void write(final byte[] b) throws IOException {
		symOut.write(b);
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		symOut.write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		symOut.flush();
	}

	@Override
	public void close() throws IOException {
		symOut.close();
	}

}
