package org.subshare.local;

import static co.codewizards.cloudstore.core.util.IOUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.crypto.AsymCombiDecrypterInputStream;
import org.subshare.core.crypto.AsymCombiEncrypterOutputStream;
import org.subshare.core.crypto.CipherTransformation;
import org.subshare.core.crypto.DecrypterInputStream;
import org.subshare.core.crypto.DefaultKeyParameterFactory;
import org.subshare.core.crypto.DummyCryptoUtil;
import org.subshare.core.crypto.EncrypterOutputStream;
import org.subshare.core.crypto.RandomIvFactory;
import org.subshare.core.dto.CryptoKeyPart;
import org.subshare.core.dto.CryptoKeyType;
import org.subshare.core.user.UserRepoKey;
import org.subshare.local.persistence.CryptoLink;

public class CryptreeNodeUtil {

	public static byte[] encryptLarge(final byte[] plain, final UserRepoKey userRepoKey) {
		assertNotNull("plain", plain);
		assertNotNull("userRepoKey", userRepoKey);
		try {
			final AsymmetricKeyParameter publicKey = userRepoKey.getKeyPair().getPublic();
			final ByteArrayOutputStream bout = new ByteArrayOutputStream(plain.length + 10240); // don't know exactly, but I guess 10 KiB should be sufficient
			final AsymCombiEncrypterOutputStream out = new AsymCombiEncrypterOutputStream(bout,
					getCipherTransformation(publicKey), publicKey,
					getSymmetricCipherTransformation(), new DefaultKeyParameterFactory());
			transferStreamData(new ByteArrayInputStream(plain), out);
			return bout.toByteArray();
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	public static byte[] decryptLarge(final byte[] encrypted, final UserRepoKey userRepoKey) {
		assertNotNull("encrypted", encrypted);
		assertNotNull("userRepoKey", userRepoKey);
		try {
			final AsymCombiDecrypterInputStream in = new AsymCombiDecrypterInputStream(
					new ByteArrayInputStream(encrypted), userRepoKey.getKeyPair().getPrivate());
			final ByteArrayOutputStream out = new ByteArrayOutputStream(encrypted.length);
			transferStreamData(in, out);
			return out.toByteArray();
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	public static byte[] encrypt(final byte[] plain, final PlainCryptoKey plainCryptoKey) {
		switch (plainCryptoKey.getCryptoKeyPart()) {
			case privateKey:
				throw new IllegalStateException("Cannot encrypt with private key!");
			case publicKey:
				return encrypt(plain, plainCryptoKey.getPublicKeyParameterOrFail());
			case sharedSecret:
				return encrypt(plain, plainCryptoKey.getKeyParameterOrFail());
			default:
				throw new IllegalStateException("Unknown plainCryptoKey.cryptoKeyPart: " + plainCryptoKey.getCryptoKeyPart());
		}
	}

	public static byte[] decrypt(final byte[] encrypted, final PlainCryptoKey plainCryptoKey) {
		switch (plainCryptoKey.getCryptoKeyPart()) {
			case privateKey:
				return decrypt(encrypted, plainCryptoKey.getPrivateKeyParameterOrFail());
			case publicKey:
				throw new IllegalStateException("Cannot decrypt with public key!");
			case sharedSecret:
				return decrypt(encrypted, plainCryptoKey.getKeyParameterOrFail());
			default:
				throw new IllegalStateException("Unknown plainCryptoKey.cryptoKeyPart: " + plainCryptoKey.getCryptoKeyPart());
		}
	}

	public static byte[] encrypt(final byte[] plain, final CipherParameters key) {
		assertNotNull("plain", plain);
		assertNotNull("key", key);
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream(plain.length + 10240); // don't know exactly, but I guess 10 KiB should be sufficient
			final CipherTransformation cipherTransformation = getCipherTransformation(key);
			final EncrypterOutputStream out = new EncrypterOutputStream(
					bout, cipherTransformation, key,
					CryptoKeyType.asymmetric == cipherTransformation.getType() ? null : new RandomIvFactory());
			transferStreamData(new ByteArrayInputStream(plain), out);
			return bout.toByteArray();
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	public static byte[] decrypt(final byte[] encrypted, final CipherParameters key) {
		assertNotNull("encrypted", encrypted);
		assertNotNull("key", key);
		try {
			final DecrypterInputStream in = new DecrypterInputStream(new ByteArrayInputStream(encrypted), key);
			final ByteArrayOutputStream out = new ByteArrayOutputStream(encrypted.length);
			transferStreamData(in, out);
			return out.toByteArray();
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	public static CryptoLink createCryptoLink(final PlainCryptoKey fromPlainCryptoKey, final PlainCryptoKey toPlainCryptoKey) {
		assertNotNull("fromPlainCryptoKey", fromPlainCryptoKey);
		assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
		final CryptoLink cryptoLink = new CryptoLink();
		cryptoLink.setFromCryptoKey(fromPlainCryptoKey.getCryptoKey());
		cryptoLink.setToCryptoKey(toPlainCryptoKey.getCryptoKey());
		cryptoLink.setToCryptoKeyData(encrypt(toPlainCryptoKey.getEncodedKey(), fromPlainCryptoKey));
		cryptoLink.setToCryptoKeyPart(toPlainCryptoKey.getCryptoKeyPart());
		toPlainCryptoKey.getCryptoKey().getInCryptoLinks().add(cryptoLink);
		return cryptoLink;
	}

	public static CryptoLink createCryptoLink(final UserRepoKey fromUserRepoKey, final PlainCryptoKey toPlainCryptoKey) {
		assertNotNull("fromUserRepoKey", fromUserRepoKey);
		assertNotNull("toPlainCryptoKey", toPlainCryptoKey);
		final CryptoLink cryptoLink = new CryptoLink();
		cryptoLink.setFromUserRepoKeyId(fromUserRepoKey.getUserRepoKeyId());
		cryptoLink.setToCryptoKey(toPlainCryptoKey.getCryptoKey());
		cryptoLink.setToCryptoKeyData(encryptLarge(toPlainCryptoKey.getEncodedKey(), fromUserRepoKey));
		cryptoLink.setToCryptoKeyPart(toPlainCryptoKey.getCryptoKeyPart());
		toPlainCryptoKey.getCryptoKey().getInCryptoLinks().add(cryptoLink);
		return cryptoLink;
	}

	public static CryptoLink createCryptoLink(final PlainCryptoKey toPlainCryptoKey) { // plain-text = UNENCRYPTED!!!
		assertNotNull("toPlainCryptoKey", toPlainCryptoKey);

		if (CryptoKeyPart.publicKey != toPlainCryptoKey.getCryptoKeyPart())
			throw new IllegalArgumentException("You probably do not want to create a plain-text - i.e. *not* encrypted - CryptoLink to a key[part] of type: " + toPlainCryptoKey.getCryptoKeyPart());

		final CryptoLink cryptoLink = new CryptoLink();
		cryptoLink.setToCryptoKey(toPlainCryptoKey.getCryptoKey());
		cryptoLink.setToCryptoKeyData(toPlainCryptoKey.getEncodedKey());
		cryptoLink.setToCryptoKeyPart(toPlainCryptoKey.getCryptoKeyPart());
		toPlainCryptoKey.getCryptoKey().getInCryptoLinks().add(cryptoLink);
		return cryptoLink;
	}

	/**
	 * Gets the configured/preferred transformation compatible for the given key type.
	 * @param key the key - can be a {@linkplain KeyParameter symmetric shared secret} or an
	 * {@linkplain AsymmetricKeyParameter asymmetric public / private key}. Must not be <code>null</code>.
	 * @return the configured/preferred transformation. Never <code>null</code>.
	 */
	private static CipherTransformation getCipherTransformation(final CipherParameters key) {
		assertNotNull("key", key);
		// TODO we need a better way to look up a compatible preferred algorithm fitting the given key. => move this (in a much better way) into the CryptoRegistry!
		if (key instanceof KeyParameter)
			return getSymmetricCipherTransformation();
		else if (key instanceof AsymmetricKeyParameter)
			return CipherTransformation.fromTransformation(DummyCryptoUtil.ASYMMETRIC_ENCRYPTION_TRANSFORMATION);
		else
			throw new IllegalArgumentException("Unexpected key type: " + key.getClass().getName());
	}

	/**
	 * Gets the configured/preferred transformation for symmetric encryption.
	 * @return the configured/preferred transformation for symmetric encryption. Never <code>null</code>.
	 */
	private static CipherTransformation getSymmetricCipherTransformation() {
		return CipherTransformation.fromTransformation(DummyCryptoUtil.SYMMETRIC_ENCRYPTION_TRANSFORMATION);
	}
}
