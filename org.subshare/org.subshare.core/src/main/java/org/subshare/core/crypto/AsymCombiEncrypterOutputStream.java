package org.subshare.core.crypto;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.core.crypto.EncrypterDecrypterStreamUtil.*;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.dto.CryptoKeyType;

public class AsymCombiEncrypterOutputStream extends FilterOutputStream {

	private final CipherTransformation asymmetricCipherTransformation;
	private final AsymmetricKeyParameter publicKey;
	private final KeyParameter symmetricKey;
	private final EncrypterOutputStream symOut;

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

		keyParameterFactory.setCipherTransformation(symmetricCipherTransformation);
		this.symmetricKey = keyParameterFactory.createKeyParameter();
		keyParameterFactory.setCipherTransformation(null);

		writeHeader();

		symOut = new EncrypterOutputStream(out, symmetricCipherTransformation, symmetricKey, ivFactory);
	}

	private void writeHeader() throws IOException {
		out.write(1); // version

		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		final EncrypterOutputStream asymOut = new EncrypterOutputStream(
				bout, asymmetricCipherTransformation, publicKey, null);
		asymOut.setCloseUnderlyingOutputStream(false);
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
