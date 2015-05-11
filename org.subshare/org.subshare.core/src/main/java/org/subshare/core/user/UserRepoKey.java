package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.subshare.core.dto.InvitationUserRepoKeyPublicKeyDto;
import org.subshare.core.dto.SignatureDto;
import org.subshare.core.dto.UserRepoKeyDto;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.pgp.PgpDecoder;
import org.subshare.core.pgp.PgpEncoder;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;
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
	private final boolean invitation;
	private PublicKeyWithSignature publicKey;

	private final byte[] encryptedSignedPrivateKeyData;
	private final byte[] signedPublicKeyData;

	public UserRepoKey(final UUID serverRepositoryId, final AsymmetricCipherKeyPair keyPair, final Set<PgpKey> pgpKeysForEncryption, final PgpKey pgpKeyForSignature, final Date validTo) {
		this.userRepoKeyId = new Uid();
		this.serverRepositoryId = assertNotNull("serverRepositoryId", serverRepositoryId);
		this.keyPair = assertNotNull("keyPair", keyPair);
		this.validTo = validTo;
		assertNotNull("pgpKeysForEncryption", pgpKeysForEncryption);
		assertNotNull("pgpKeyForSignature", pgpKeyForSignature);
		this.encryptedSignedPrivateKeyData = encryptSignPrivateKeyData(pgpKeysForEncryption, pgpKeyForSignature);
		this.signedPublicKeyData = signPublicKeyData(pgpKeyForSignature);

		// It is an invitation, if the user didn't encrypt+sign it for himself.
		this.invitation = pgpKeysForEncryption.size() != 1 || pgpKeysForEncryption.iterator().next() != pgpKeyForSignature;
	}

	public UserRepoKey(final Uid userRepoKeyId, final UUID serverRepositoryId, final byte[] encryptedSignedPrivateKeyData, final byte[] signedPublicKeyData, final Date validTo, final boolean invitation) {
		this.userRepoKeyId = assertNotNull("userRepoKeyId", userRepoKeyId);
		this.serverRepositoryId = assertNotNull("serverRepositoryId", serverRepositoryId);
		this.encryptedSignedPrivateKeyData = assertNotNull("encryptedSignedPrivateKeyData", encryptedSignedPrivateKeyData);
		this.signedPublicKeyData = assertNotNull("signedPublicKeyData", signedPublicKeyData);
		this.validTo = validTo;
		this.invitation = invitation;

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
					getSignedPublicKeyData(), getValidTo(), isInvitation());

		return publicKey;
	}

	public boolean isInvitation() {
		return invitation;
	}

	/**
	 * Gets the time-stamp until which this {@code UserRepoKey} is valid - or <code>null</code> for non-temporary (i.e. permanent)
	 * keys.
	 * @return
	 */
	public Date getValidTo() {
		return validTo;
	}

	private byte[] encryptSignPrivateKeyData(final Set<PgpKey> pgpKeysForEncryption, final PgpKey pgpKeyForSignature) {
		if (pgpKeysForEncryption.size() == 1 && PgpKey.TEST_DUMMY_PGP_KEY == pgpKeysForEncryption.iterator().next())
			return new byte[0]; // for consistency with signPublicKeyData(...) we return an empty array here, too.

		assertNotNull("pgpKeysForEncryption", pgpKeysForEncryption);
		assertNotNull("pgpKeyForSignature", pgpKeyForSignature);
		final byte[] encodedPrivateKey = CryptoRegistry.getInstance().encodePrivateKey(keyPair.getPrivate());
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final PgpEncoder encoder = PgpRegistry.getInstance().getPgpOrFail().createEncoder(new ByteArrayInputStream(encodedPrivateKey), out);
		encoder.getEncryptPgpKeys().addAll(pgpKeysForEncryption);
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
			throw new RuntimeException(e);
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
			throw new RuntimeException(e);
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
		private final boolean invitation;

		public PublicKey(final Uid userRepoKeyId, final UUID serverRepositoryId, final AsymmetricKeyParameter publicKey, final Date validTo, final boolean invitation) {
			this.userRepoKeyId = assertNotNull("userRepoKeyId", userRepoKeyId);
			this.serverRepositoryId = assertNotNull("serverRepositoryId", serverRepositoryId);
			this.publicKey = assertNotNull("publicKey", publicKey);
			this.validTo = validTo;
			this.invitation = invitation;
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

		public boolean isInvitation() {
			return invitation;
		}
	}

	public static class PublicKeyWithSignature extends PublicKey implements Signable {
		private final byte[] signedPublicKeyData; // OpenPGP-signed!!! Has nothing to do with Signable!

		// BEGIN Signable stuff - has nothing to do with OpenPGP-signature!
		private byte[] publicKeyData;
		private Signature signature;
		// END Signable stuff

		protected PublicKeyWithSignature(Uid userRepoKeyId, UUID serverRepositoryId, AsymmetricKeyParameter publicKey, final byte[] signedPublicKeyData, final Date validTo, final boolean invitation) {
			super(userRepoKeyId, serverRepositoryId, publicKey, validTo, invitation);
			this.signedPublicKeyData = assertNotNull("signedPublicKeyData", signedPublicKeyData);
		}

		public PublicKeyWithSignature(Uid userRepoKeyId, UUID repositoryId, final byte[] signedPublicKeyData, final Date validTo, final boolean invitation) {
			super(userRepoKeyId, repositoryId, verifyPublicKeyData(signedPublicKeyData), validTo, invitation);
			this.signedPublicKeyData = assertNotNull("signedPublicKeyData", signedPublicKeyData);
		}

		public byte[] getSignedPublicKeyData() {
			return signedPublicKeyData;
		}
		public byte[] getPublicKeyData() {
			// publicKeyData is of course the same publicKey-data as the one in signedPublicKeyData, but it is not PGP-signed.
			if (publicKeyData == null)
				publicKeyData = CryptoRegistry.getInstance().encodePublicKey(getPublicKey());

			return publicKeyData;
		}

		@Override
		public int getSignedDataVersion() {
			return 0;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * <b>Important:</b> The implementation in {@code InvitationUserRepoKeyPublicKey} must exactly match the one
		 * in {@code UserRepoKey.PublicKeyWithSignature} and the one in {@link InvitationUserRepoKeyPublicKeyDto}!
		 */
		@Override
		public InputStream getSignedData(final int signedDataVersion) {
			try {
				byte separatorIndex = 0;
				return new MultiInputStream(
						InputStreamSource.Helper.createInputStreamSource(UserRepoKeyDto.PUBLIC_KEY_SIGNED_DATA_TYPE),

						InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
						InputStreamSource.Helper.createInputStreamSource(getUserRepoKeyId()),

						InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
						InputStreamSource.Helper.createInputStreamSource(getServerRepositoryId()),

						InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
						InputStreamSource.Helper.createInputStreamSource(getPublicKeyData())
				);
			} catch (final IOException x) {
				throw new RuntimeException(x);
			}
		}

		@Override
		public Signature getSignature() {
			return signature;
		}
		@Override
		public void setSignature(final Signature signature) {
			if (!equal(this.signature, signature))
				this.signature = SignatureDto.copyIfNeeded(signature);
		}
	}
}
