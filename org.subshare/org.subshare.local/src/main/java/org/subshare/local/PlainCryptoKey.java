package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.dto.CryptoKeyPart;
import org.subshare.crypto.CryptoRegistry;
import org.subshare.local.persistence.CryptoKey;

public class PlainCryptoKey {

	private final CryptoKey cryptoKey;

	private final CryptoKeyPart cryptoKeyPart;

	private final CipherParameters cipherParameters;

	public PlainCryptoKey(final CryptoKey cryptoKey, final CryptoKeyPart cryptoKeyPart, final CipherParameters cipherParameters) {
		this.cryptoKey = assertNotNull("cryptoKey", cryptoKey);
		this.cryptoKeyPart = assertNotNull("cryptoKeyPart", cryptoKeyPart);
		this.cipherParameters = assertNotNull("cipherParameters", cipherParameters);

		switch (cryptoKeyPart) {
			case privateKey:
			case publicKey:
				if (!(cipherParameters instanceof AsymmetricKeyParameter))
					throw new IllegalArgumentException("cryptoKeyPart indicates asymmetric cryptography, but cipherParameters is not an instance of AsymmetricKeyParameter!");
				break;
			case sharedSecret:
				if (!(cipherParameters instanceof KeyParameter))
					throw new IllegalArgumentException("cryptoKeyPart indicates symmetric cryptography, but cipherParameters is not an instance of KeyParameter!");
				break;
			default:
				throw new IllegalArgumentException("Unknown cryptoKeyPart: " + cryptoKeyPart);
		}
	}

	public PlainCryptoKey(final CryptoKey cryptoKey, final CryptoKeyPart cryptoKeyPart, final byte[] encoded) {
		this(cryptoKey, cryptoKeyPart, getDecodedKey(cryptoKeyPart, encoded));
	}

	private static CipherParameters getDecodedKey(final CryptoKeyPart cryptoKeyPart, final byte[] encoded) {
		try {
			switch (cryptoKeyPart) {
				case privateKey:
					return CryptoRegistry.getInstance().decodePrivateKey(encoded);
				case publicKey:
					return CryptoRegistry.getInstance().decodePublicKey(encoded);
				case sharedSecret:
					return new KeyParameter(encoded);
				default:
					throw new IllegalArgumentException("Unknown cryptoKeyPart: " + cryptoKeyPart);
			}
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		}
	}

	public CryptoKey getCryptoKey() {
		return cryptoKey;
	}

	public CryptoKeyPart getCryptoKeyPart() {
		return cryptoKeyPart;
	}

	public CipherParameters getCipherParameters() {
		return cipherParameters;
	}

	public KeyParameter getKeyParameterOrFail() {
		if (CryptoKeyPart.sharedSecret != cryptoKeyPart)
			throw new IllegalStateException("This is not a shared secret! this.cryptoKeyPart=" + cryptoKeyPart);

		if (cipherParameters instanceof KeyParameter)
			return (KeyParameter)cipherParameters;
		else
			throw new IllegalStateException("cipherParameters is not an instance of KeyParameter!");
	}

	public AsymmetricKeyParameter getPublicKeyParameterOrFail() {
		if (CryptoKeyPart.publicKey != cryptoKeyPart)
			throw new IllegalStateException("This is not a public key! this.cryptoKeyPart=" + cryptoKeyPart);

		if (cipherParameters instanceof AsymmetricKeyParameter)
			return (AsymmetricKeyParameter)cipherParameters;
		else
			throw new IllegalStateException("cipherParameters is not an instance of AsymmetricKeyParameter!");
	}

	public AsymmetricKeyParameter getPrivateKeyParameterOrFail() {
		if (CryptoKeyPart.privateKey != cryptoKeyPart)
			throw new IllegalStateException("This is not a private key! this.cryptoKeyPart=" + cryptoKeyPart);

		if (cipherParameters instanceof AsymmetricKeyParameter)
			return (AsymmetricKeyParameter)cipherParameters;
		else
			throw new IllegalStateException("cipherParameters is not an instance of AsymmetricKeyParameter!");
	}

	public byte[] getEncodedKey() {
		try {
			switch (cryptoKeyPart) {
				case privateKey:
					return CryptoRegistry.getInstance().encodePrivateKey(getPrivateKeyParameterOrFail());
				case publicKey:
					return CryptoRegistry.getInstance().encodePublicKey(getPublicKeyParameterOrFail());
				case sharedSecret:
					return getKeyParameterOrFail().getKey();
				default:
					throw new IllegalArgumentException("Unknown cryptoKeyPart: " + cryptoKeyPart);
			}
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		}
	}
}
