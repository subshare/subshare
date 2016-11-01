package org.subshare.core.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

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
import org.subshare.core.sign.Signature;
import org.subshare.crypto.CryptoRegistry;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;

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
public class UserRepoKeyImpl implements UserRepoKey {
	private final Uid userRepoKeyId;
	private final UUID serverRepositoryId;
	private final AsymmetricCipherKeyPair keyPair;
	private final Date validTo; // TODO this - at least - should be signed!
	private final boolean invitation;
	private PublicKeyWithSignatureImpl publicKey;

	private final byte[] encryptedSignedPrivateKeyData;
	private final byte[] signedPublicKeyData;

	public UserRepoKeyImpl(final UUID serverRepositoryId, final AsymmetricCipherKeyPair keyPair, final Set<PgpKey> pgpKeysForEncryption, final PgpKey pgpKeyForSignature, final Date validTo) {
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

	public UserRepoKeyImpl(final Uid userRepoKeyId, final UUID serverRepositoryId, final byte[] encryptedSignedPrivateKeyData, final byte[] signedPublicKeyData, final Date validTo, final boolean invitation) {
		this.userRepoKeyId = assertNotNull("userRepoKeyId", userRepoKeyId);
		this.serverRepositoryId = assertNotNull("serverRepositoryId", serverRepositoryId);
		this.encryptedSignedPrivateKeyData = assertNotNull("encryptedSignedPrivateKeyData", encryptedSignedPrivateKeyData);
		this.signedPublicKeyData = assertNotNull("signedPublicKeyData", signedPublicKeyData);
		this.validTo = validTo;
		this.invitation = invitation;

		// TODO should we maybe defer the decryption until later, when the key is actually used?!
		this.keyPair = new AsymmetricCipherKeyPair(verifyPublicKeyData(), decryptVerifyPrivateKeyData());
	}

	@Override
	public Uid getUserRepoKeyId() {
		return userRepoKeyId;
	}

	@Override
	public UUID getServerRepositoryId() {
		return serverRepositoryId;
	}

	@Override
	public AsymmetricCipherKeyPair getKeyPair() {
		return keyPair;
	}

	@Override
	public PublicKeyWithSignature getPublicKey() {
		if (publicKey == null)
			publicKey = new PublicKeyWithSignatureImpl(
					getUserRepoKeyId(), getServerRepositoryId(), getKeyPair().getPublic(),
					getSignedPublicKeyData(), getValidTo(), isInvitation());

		return publicKey;
	}

	@Override
	public boolean isInvitation() {
		return invitation;
	}

	/**
	 * Gets the time-stamp until which this {@code UserRepoKey} is valid - or <code>null</code> for non-temporary (i.e. permanent)
	 * keys.
	 * @return
	 */
	@Override
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
			if (decoder.getPgpSignature() == null)
				throw new SignatureException("Missing signature!");

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
			if (decoder.getPgpSignature() == null)
				throw new SignatureException("Missing signature!");

			final byte[] publicKeyData = out.toByteArray();
			final AsymmetricKeyParameter publicKey = CryptoRegistry.getInstance().decodePublicKey(publicKeyData);
			return publicKey;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public byte[] getEncryptedSignedPrivateKeyData() {
		return encryptedSignedPrivateKeyData;
	}
	@Override
	public byte[] getSignedPublicKeyData() {
		return signedPublicKeyData;
	}

	public static class PublicKeyImpl implements UserRepoKey.PublicKey {
		private final Uid userRepoKeyId;
		private final UUID serverRepositoryId;
		private final AsymmetricKeyParameter publicKey;
		private final Date validTo;
		private final boolean invitation;

		public PublicKeyImpl(final Uid userRepoKeyId, final UUID serverRepositoryId, final AsymmetricKeyParameter publicKey, final Date validTo, final boolean invitation) {
			this.userRepoKeyId = assertNotNull("userRepoKeyId", userRepoKeyId);
			this.serverRepositoryId = assertNotNull("serverRepositoryId", serverRepositoryId);
			this.publicKey = assertNotNull("publicKey", publicKey);
			this.validTo = validTo;
			this.invitation = invitation;
		}

		@Override
		public Uid getUserRepoKeyId() {
			return userRepoKeyId;
		}

		@Override
		public UUID getServerRepositoryId() {
			return serverRepositoryId;
		}

		@Override
		public AsymmetricKeyParameter getPublicKey() {
			return publicKey;
		}

		@Override
		public Date getValidTo() {
			return validTo;
		}

		@Override
		public boolean isInvitation() {
			return invitation;
		}

		@Override
		public String toString() {
			return String.format("%s[userRepoKeyId=%s, invitation=%s, validTo=%s]", this.getClass().getSimpleName(), userRepoKeyId, invitation, validTo);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;

			if (obj == null)
				return false;

			if (obj.getClass() != this.getClass())
				return false;

			final PublicKeyImpl other = (PublicKeyImpl) obj;
			return this.userRepoKeyId.equals(other.getUserRepoKeyId());
		}

		@Override
		public int hashCode() {
			return userRepoKeyId.hashCode();
		}
	}

	public static class PublicKeyWithSignatureImpl extends PublicKeyImpl implements UserRepoKey.PublicKeyWithSignature {
		private final byte[] signedPublicKeyData; // OpenPGP-signed!!! Has nothing to do with Signable!

		// BEGIN Signable stuff - has nothing to do with OpenPGP-signature!
		private byte[] publicKeyData;
		private Signature signature;
		// END Signable stuff

		protected PublicKeyWithSignatureImpl(Uid userRepoKeyId, UUID serverRepositoryId, AsymmetricKeyParameter publicKey, final byte[] signedPublicKeyData, final Date validTo, final boolean invitation) {
			super(userRepoKeyId, serverRepositoryId, publicKey, validTo, invitation);
			this.signedPublicKeyData = assertNotNull("signedPublicKeyData", signedPublicKeyData);
		}

		public PublicKeyWithSignatureImpl(Uid userRepoKeyId, UUID repositoryId, final byte[] signedPublicKeyData, final Date validTo, final boolean invitation) {
			super(userRepoKeyId, repositoryId, verifyPublicKeyData(signedPublicKeyData), validTo, invitation);
			this.signedPublicKeyData = assertNotNull("signedPublicKeyData", signedPublicKeyData);
		}

		@Override
		public byte[] getSignedPublicKeyData() {
			return signedPublicKeyData;
		}
		@Override
		public byte[] getPublicKeyData() {
			// publicKeyData is of course the same publicKey-data as the one in signedPublicKeyData, but it is not PGP-signed.
			if (publicKeyData == null)
				publicKeyData = CryptoRegistry.getInstance().encodePublicKey(getPublicKey());

			return publicKeyData;
		}

		@Override
		public String getSignedDataType() {
			return UserRepoKeyDto.PUBLIC_KEY_SIGNED_DATA_TYPE;
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

	@Override
	public String toString() {
		return String.format("%s[userRepoKeyId=%s, invitation=%s]", this.getClass().getSimpleName(), userRepoKeyId, invitation);
	}
}
