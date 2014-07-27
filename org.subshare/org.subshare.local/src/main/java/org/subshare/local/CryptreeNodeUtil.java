package org.subshare.local;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.core.crypto.DummyCryptoUtil.*;

import java.util.Arrays;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.subshare.core.crypto.KeyFactory;
import org.subshare.core.dto.CryptoKeyPart;
import org.subshare.core.user.UserRepoKey;
import org.subshare.crypto.Cipher;
import org.subshare.crypto.CipherOperationMode;
import org.subshare.crypto.CryptoRegistry;
import org.subshare.local.persistence.CryptoLink;

public class CryptreeNodeUtil {

	public static byte[] decrypt(final byte[] encrypted, final UserRepoKey userRepoKey) {
		// TODO better encoding format! And extract this into a nice class! With a test!
		int index = 0;
		if (encrypted[index++] != 1) // version
			throw new IllegalStateException("Wrong encoding?! Wrong version?!");

		final int encryptedKeyLength = (encrypted[index++] & 0xff) + ( (encrypted[index++] & 0xff) << 8 );
		final byte[] encryptedKey = new byte[encryptedKeyLength];
		System.arraycopy(encrypted, index, encryptedKey, 0, encryptedKeyLength);
		index += encryptedKeyLength;

		final byte[] encryptedData = new byte[encrypted.length - index];
		System.arraycopy(encrypted, index, encryptedData, 0, encryptedData.length);

		System.out.println("decrypt: userRepoKeyId: " + userRepoKey.getUserRepoKeyId());
		System.out.println("encrypt: encryptedKeyLength: " + encryptedKeyLength);
		System.out.println("encrypt: encryptedKey: " + Arrays.toString(encryptedKey));
		final byte[] symmetricKeyBytes = decrypt(encryptedKey, userRepoKey.getKeyPair().getPrivate());
		System.out.println("decrypt: symmetricKey: " + Arrays.toString(symmetricKeyBytes));
		final KeyParameter symmetricKey = new KeyParameter(symmetricKeyBytes);

		final byte[] plain = decrypt(encryptedData, symmetricKey);
		return plain;
	}

	public static byte[] encrypt(final byte[] plain, final UserRepoKey userRepoKey) {
		// TODO better encoding format! And extract this into a nice class! With a test!
		final KeyParameter symmetricKey = KeyFactory.getInstance().createSymmetricKey();
		System.out.println("encrypt: userRepoKeyId: " + userRepoKey.getUserRepoKeyId());
		System.out.println("encrypt: symmetricKey: " + Arrays.toString(symmetricKey.getKey()));
		final byte[] encryptedKey = encrypt(symmetricKey.getKey(), userRepoKey.getKeyPair().getPublic());
		System.out.println("encrypt: encryptedKey.length: " + encryptedKey.length);
		System.out.println("encrypt: encryptedKey: " + Arrays.toString(encryptedKey));
		final byte[] encryptedData = encrypt(plain, symmetricKey);
		final byte[] combined = new byte[1 + 2 + encryptedKey.length + encryptedData.length];
		if (encryptedKey.length > 65535)
			throw new IllegalStateException("encryptedKey.length > 65535 :: length cannot be encoded in 2 bytes!");

		int index = 0;
		combined[index++] = 1; // version
		combined[index++] = (byte) encryptedKey.length;
		combined[index++] = (byte) (encryptedKey.length >>> 8);
		System.arraycopy(encryptedKey, 0, combined, index, encryptedKey.length);
		index += encryptedKey.length;
		System.arraycopy(encryptedData, 0, combined, index, encryptedData.length);

		return combined;
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

	public static byte[] decrypt(final byte[] encrypted, final AsymmetricKeyParameter privateKey) {
		try {
			final Cipher cipher = CryptoRegistry.getInstance().createCipher(ASYMMETRIC_ENCRYPTION_TRANSFORMATION);
			cipher.init(CipherOperationMode.DECRYPT, privateKey);
			final byte[] plain = cipher.doFinal(encrypted);
			return plain;
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		}
	}

	public static byte[] encrypt(final byte[] plain, final AsymmetricKeyParameter publicKey) {
		try {
			final Cipher cipher = CryptoRegistry.getInstance().createCipher(ASYMMETRIC_ENCRYPTION_TRANSFORMATION);
			cipher.init(CipherOperationMode.ENCRYPT, publicKey);
			final byte[] encrypted = cipher.doFinal(plain);
			return encrypted;
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		}
	}

	public static byte[] decrypt(final byte[] encrypted, final KeyParameter symmetricKey) {
		try {
			final Cipher cipher = CryptoRegistry.getInstance().createCipher(SYMMETRIC_ENCRYPTION_TRANSFORMATION);
			final ParametersWithIV parameters = new ParametersWithIV(symmetricKey, getNullIV(cipher.getIVSize()));
			cipher.init(CipherOperationMode.DECRYPT, parameters);
			final byte[] plain = cipher.doFinal(encrypted);
			return plain;
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
			throw new RuntimeException(x);
		}
	}

	public static byte[] encrypt(final byte[] plain, final KeyParameter symmetricKey) {
		try {
			final Cipher cipher = CryptoRegistry.getInstance().createCipher(SYMMETRIC_ENCRYPTION_TRANSFORMATION);
			final ParametersWithIV parameters = new ParametersWithIV(symmetricKey, getNullIV(cipher.getIVSize()));
			cipher.init(CipherOperationMode.ENCRYPT, parameters);
			final byte[] encrypted = cipher.doFinal(plain);
			return encrypted;
		} catch (final RuntimeException x) {
			throw x;
		} catch (final Exception x) {
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
		cryptoLink.setToCryptoKeyData(encrypt(toPlainCryptoKey.getEncodedKey(), fromUserRepoKey));
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
}
