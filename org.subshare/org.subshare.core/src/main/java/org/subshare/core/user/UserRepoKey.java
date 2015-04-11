package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
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
 * <p>
 * Since the {@link #getSignedPublicKeyData() signedPublicKeyData} reveals the identity of the owner,
 * the {@link PublicKey} does not contain it, but only non-signed public-key-data!
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public class UserRepoKey {

	private final Uid userRepoKeyId;
	private final UUID serverRepositoryId;
	private final AsymmetricCipherKeyPair keyPair;
	private final Date validTo; // TODO this - at least - should be signed!
	private PublicKeyWithSignature publicKey;

	private final byte[] encryptedSignedPrivateKeyData;
	private final byte[] signedPublicKeyData;

	public UserRepoKey(final UUID serverRepositoryId, final AsymmetricCipherKeyPair keyPair, final PgpKey pgpKeyForEncryption, final PgpKey pgpKeyForSignature, final Date validTo) {
		this.userRepoKeyId = new Uid();
		this.serverRepositoryId = assertNotNull("serverRepositoryId", serverRepositoryId);
		this.keyPair = assertNotNull("keyPair", keyPair);
		this.validTo = validTo;
		assertNotNull("pgpKeyForEncryption", pgpKeyForEncryption);
		assertNotNull("pgpKeyForSignature", pgpKeyForSignature);
		this.encryptedSignedPrivateKeyData = encryptSignPrivateKeyData(pgpKeyForEncryption, pgpKeyForSignature);
		this.signedPublicKeyData = signPublicKeyData(pgpKeyForSignature);
	}

	public UserRepoKey(final Uid userRepoKeyId, final UUID serverRepositoryId, final byte[] encryptedSignedPrivateKeyData, final byte[] signedPublicKeyData, final Date validTo) {
		this.userRepoKeyId = assertNotNull("userRepoKeyId", userRepoKeyId);
		this.serverRepositoryId = assertNotNull("serverRepositoryId", serverRepositoryId);
		this.encryptedSignedPrivateKeyData = assertNotNull("encryptedSignedPrivateKeyData", encryptedSignedPrivateKeyData);
		this.signedPublicKeyData = assertNotNull("signedPublicKeyData", signedPublicKeyData);
		this.validTo = validTo;

		// TODO should we maybe defer the decryption until later, when the key is actually used?!
		this.keyPair = new AsymmetricCipherKeyPair(verifyPublicKeyData(), decryptVerifyPrivateKeyData());
	}

	public Uid getUserRepoKeyId() {
		return userRepoKeyId;
	}

	public UUID getServerRepositoryId() {
		return serverRepositoryId;
	}

	public AsymmetricCipherKeyPair getKeyPair() {
		return keyPair;
	}

	public PublicKeyWithSignature getPublicKey() {
		if (publicKey == null)
			publicKey = new PublicKeyWithSignature(
					getUserRepoKeyId(), getServerRepositoryId(), getKeyPair().getPublic(),
					getSignedPublicKeyData(), getValidTo());

		return publicKey;
	}

	/**
	 * Gets the time-stamp until which this {@code UserRepoKey} is valid - or <code>null</code> for non-temporary (i.e. permanent)
	 * keys.
	 * @return
	 */
	public Date getValidTo() {
		return validTo;
	}

	private byte[] encryptSignPrivateKeyData(final PgpKey pgpKeyForEncryption, final PgpKey pgpKeyForSignature) {
		if (PgpKey.TEST_DUMMY_PGP_KEY == pgpKeyForEncryption)
			return new byte[0]; // for consistency with signPublicKeyData(...) we return an empty array here, too.

		assertNotNull("pgpKeyForEncryption", pgpKeyForEncryption);
		assertNotNull("pgpKeyForSignature", pgpKeyForSignature);
		final byte[] encodedPrivateKey = CryptoRegistry.getInstance().encodePrivateKey(keyPair.getPrivate());
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final PgpEncoder encoder = PgpRegistry.getInstance().getPgpOrFail().createEncoder(new ByteArrayInputStream(encodedPrivateKey), out);
		encoder.getEncryptPgpKeys().add(pgpKeyForEncryption);
		encoder.setSignPgpKey(pgpKeyForSignature);
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
			return new byte[0]; // null causes an IllegalArgumentException => empty array.

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
		return verifyPublicKeyData(signedPublicKeyData);
	}

	private static AsymmetricKeyParameter verifyPublicKeyData(final byte[] signedPublicKeyData) throws SignatureException {
		assertNotNull("signedPublicKeyData", signedPublicKeyData);
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
		private final UUID serverRepositoryId;
		private final AsymmetricKeyParameter publicKey;
		private final Date validTo;

		public PublicKey(final Uid userRepoKeyId, final UUID serverRepositoryId, final AsymmetricKeyParameter publicKey, final Date validTo) {
			this.userRepoKeyId = assertNotNull("userRepoKeyId", userRepoKeyId);
			this.serverRepositoryId = assertNotNull("serverRepositoryId", serverRepositoryId);
			this.publicKey = assertNotNull("publicKey", publicKey);
			this.validTo = validTo;
		}

		public Uid getUserRepoKeyId() {
			return userRepoKeyId;
		}

		public UUID getServerRepositoryId() {
			return serverRepositoryId;
		}

		public AsymmetricKeyParameter getPublicKey() {
			return publicKey;
		}

		public Date getValidTo() {
			return validTo;
		}
	}

	public static class PublicKeyWithSignature extends PublicKey {
		private final byte[] signedPublicKeyData;

		protected PublicKeyWithSignature(Uid userRepoKeyId, UUID serverRepositoryId, AsymmetricKeyParameter publicKey, final byte[] signedPublicKeyData, final Date validTo) {
			super(userRepoKeyId, serverRepositoryId, publicKey, validTo);
			this.signedPublicKeyData = assertNotNull("signedPublicKeyData", signedPublicKeyData);
		}

		public PublicKeyWithSignature(Uid userRepoKeyId, UUID repositoryId, final byte[] signedPublicKeyData, final Date validTo) {
			super(userRepoKeyId, repositoryId, verifyPublicKeyData(signedPublicKeyData), validTo);
			this.signedPublicKeyData = assertNotNull("signedPublicKeyData", signedPublicKeyData);
		}

		public byte[] getSignedPublicKeyData() {
			return signedPublicKeyData;
		}
	}
}
