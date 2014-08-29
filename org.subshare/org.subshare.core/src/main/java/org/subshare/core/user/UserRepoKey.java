package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.subshare.core.pgp.PgpDecoder;
import org.subshare.core.pgp.PgpEncoder;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.crypto.CryptoRegistry;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.Uid;

/**
 * A user's master-key for one single repository.
 * <p>
 * In order to prevent the provider from knowing who has written what and who has access to what, every
 * user might indeed have multiple such keys per repository. The provider will only know the public keys,
 * but he does not know who owns them - not even how many users there really are.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class UserRepoKey {

	private final UserRepoKeyRing userRepoKeyRing;
	private final Uid userRepoKeyId;
	private final UUID repositoryId;
	private final AsymmetricCipherKeyPair keyPair;
	private PublicKey publicKey;

	private final byte[] encryptedSignedPrivateKeyData;
	private final byte[] signedPublicKeyData;

	public UserRepoKey(final UserRepoKeyRing userRepoKeyRing, final UUID repositoryId, final AsymmetricCipherKeyPair keyPair, final PgpKey pgpKey) {
		this.userRepoKeyRing = assertNotNull("userRepoKeyRing", userRepoKeyRing);
		this.userRepoKeyId = new Uid();
		this.repositoryId = assertNotNull("repositoryId", repositoryId);
		this.keyPair = assertNotNull("keyPair", keyPair);
		assertNotNull("pgpKey", pgpKey);
		this.encryptedSignedPrivateKeyData = encryptSignPrivateKeyData(pgpKey);
		this.signedPublicKeyData = signPublicKeyData(pgpKey);
	}

	public UserRepoKey(final UserRepoKeyRing userRepoKeyRing, final Uid userRepoKeyId, final UUID repositoryId, final byte[] encryptedSignedPrivateKeyData, final byte[] signedPublicKeyData) {
		this.userRepoKeyRing = assertNotNull("userRepoKeyRing", userRepoKeyRing);
		this.userRepoKeyId = assertNotNull("userRepoKeyId", userRepoKeyId);
		this.repositoryId = assertNotNull("repositoryId", repositoryId);
		this.encryptedSignedPrivateKeyData = assertNotNull("encryptedSignedPrivateKeyData", encryptedSignedPrivateKeyData);
		this.signedPublicKeyData = assertNotNull("signedPublicKeyData", signedPublicKeyData);
		this.keyPair = new AsymmetricCipherKeyPair(verifyPublicKeyData(), decryptVerifyPrivateKeyData());
	}

	public UserRepoKeyRing getUserRepoKeyRing() {
		return userRepoKeyRing;
	}

	public Uid getUserRepoKeyId() {
		return userRepoKeyId;
	}

	public UUID getRepositoryId() {
		return repositoryId;
	}

	public AsymmetricCipherKeyPair getKeyPair() {
		return keyPair;
	}

	public PublicKey getPublicKey() {
		if (publicKey == null)
			publicKey = new PublicKey(
					getUserRepoKeyId(), getRepositoryId(), getKeyPair().getPublic());

		return publicKey;
	}

	private byte[] encryptSignPrivateKeyData(final PgpKey pgpKey) {
		if (PgpKey.TEST_DUMMY_PGP_KEY == pgpKey)
			return null;

		assertNotNull("pgpKey", pgpKey);
		final byte[] encodedPrivateKey = CryptoRegistry.getInstance().encodePrivateKey(keyPair.getPrivate());
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final PgpEncoder encoder = PgpRegistry.getInstance().getPgpOrFail().createEncoder(new ByteArrayInputStream(encodedPrivateKey), out);
		encoder.getEncryptPgpKeys().add(pgpKey);
		encoder.setSignPgpKey(pgpKey);
		try {
			encoder.encode();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return out.toByteArray();
	}

	private AsymmetricKeyParameter decryptVerifyPrivateKeyData() {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final PgpDecoder decoder = PgpRegistry.getInstance().getPgpOrFail().createDecoder(new ByteArrayInputStream(encryptedSignedPrivateKeyData), out);
		try {
			decoder.decode();
			final byte[] privateKeyData = out.toByteArray();
			final AsymmetricKeyParameter privateKey = CryptoRegistry.getInstance().decodePrivateKey(privateKeyData);
			return privateKey;
		} catch (final IOException e) {
			throw new RuntimeException();
		}
	}

	private byte[] signPublicKeyData(final PgpKey pgpKey) {
		if (PgpKey.TEST_DUMMY_PGP_KEY == pgpKey)
			return null;

		assertNotNull("pgpKey", pgpKey);
		final byte[] encodedPublicKey = CryptoRegistry.getInstance().encodePublicKey(keyPair.getPublic());
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final PgpEncoder encoder = PgpRegistry.getInstance().getPgpOrFail().createEncoder(new ByteArrayInputStream(encodedPublicKey), out);
		encoder.setSignPgpKey(pgpKey);
		try {
			encoder.encode();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return out.toByteArray();
	}

	private AsymmetricKeyParameter verifyPublicKeyData() throws SignatureException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final PgpDecoder decoder = PgpRegistry.getInstance().getPgpOrFail().createDecoder(new ByteArrayInputStream(signedPublicKeyData), out);
		try {
			decoder.decode();
			final byte[] publicKeyData = out.toByteArray();
			final AsymmetricKeyParameter publicKey = CryptoRegistry.getInstance().decodePublicKey(publicKeyData);
			return publicKey;
		} catch (final IOException e) {
			throw new RuntimeException();
		}
	}

	public byte[] getEncryptedSignedPrivateKeyData() {
		return encryptedSignedPrivateKeyData;
	}
	public byte[] getSignedPublicKeyData() {
		return signedPublicKeyData;
	}

	public static class PublicKey {
		private final Uid userRepoKeyId;
		private final UUID repositoryId;
		private final AsymmetricKeyParameter publicKey;

		public PublicKey(final Uid userRepoKeyId, final UUID repositoryId, final AsymmetricKeyParameter publicKey) {
			this.userRepoKeyId = assertNotNull("userRepoKeyId", userRepoKeyId);
			this.repositoryId = assertNotNull("repositoryId", repositoryId);
			this.publicKey = assertNotNull("publicKey", publicKey);
		}

		public Uid getUserRepoKeyId() {
			return userRepoKeyId;
		}

		public UUID getRepositoryId() {
			return repositoryId;
		}

		public AsymmetricKeyParameter getPublicKey() {
			return publicKey;
		}
	}
}
