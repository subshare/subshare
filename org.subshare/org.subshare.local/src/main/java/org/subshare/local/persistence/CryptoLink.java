package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Indices;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;
import javax.jdo.listener.StoreCallback;

import org.subshare.core.dto.CryptoKeyPart;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.CryptoLinkDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.user.UserRepoKey;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

/**
 * Cryptographic link from the key {@link #getFromCryptoKey() fromCryptoKey} to the key
 * {@link #getToCryptoKey() toCryptoKey}.
 * <p>
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Unique(name="CryptoLink_cryptoLinkId", members="cryptoLinkId")
@Indices({
	@Index(name="CryptoLink_localRevision", members="localRevision")
})
@Queries({
	@Query(name="getCryptoLink_cryptoLinkId", value="SELECT UNIQUE WHERE this.cryptoLinkId == :cryptoLinkId"),
	@Query(name="getCryptoLinksChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision"),
	@Query(
			name="getActiveCryptoLinks_toCryptoRepoFile_toCryptoKeyRole_toCryptoKeyPart",
			value="SELECT WHERE "
					+ "this.toCryptoKey.active == true && "
					+ "this.toCryptoKey.cryptoRepoFile == :toCryptoRepoFile && "
					+ "this.toCryptoKey.cryptoKeyRole == :toCryptoKeyRole && "
					+ "this.toCryptoKeyPart == :toCryptoKeyPart")
})
public class CryptoLink extends Entity implements WriteProtectedEntity, AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String cryptoLinkId;

	private long localRevision;

	private CryptoKey fromCryptoKey;

	private UserRepoKeyPublicKey fromUserRepoKeyPublicKey;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoKey toCryptoKey;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(jdbcType="INTEGER")
	private CryptoKeyPart toCryptoKeyPart;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] toCryptoKeyData;

// TODO BEGIN WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247
//		@Persistent(nullValue=NullValue.EXCEPTION)
//		@Embedded
//		private SignatureImpl signature;

		@Persistent(nullValue=NullValue.EXCEPTION)
		private Date signatureCreated;

		@Persistent(nullValue=NullValue.EXCEPTION)
		@Column(length=22)
		private String signingUserRepoKeyId;

		@Persistent(nullValue=NullValue.EXCEPTION)
		private byte[] signatureData;
// END WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247

	public CryptoLink() { }

	public CryptoLink(final Uid cryptoLinkId) {
		this.cryptoLinkId = cryptoLinkId == null ? null : cryptoLinkId.toString();
	}

	public Uid getCryptoLinkId() {
		if (cryptoLinkId == null)
			cryptoLinkId = new Uid().toString();

		return new Uid(cryptoLinkId);
	}

	/**
	 * Gets the key used to encrypt {@link #getToCryptoKeyData() toCryptoKeyData}.
	 * <p>
	 * <b>Important:</b> This property must be <code>null</code>, if
	 * {@link #getFromUserRepoKeyId() fromUserRepoKeyId} is not <code>null</code>. Only at most one of them
	 * can be a non-<code>null</code> value (both can be <code>null</code>).
	 * @return the key used to encrypt {@link #getToCryptoKeyData() toCryptoKeyData}. Is
	 * <code>null</code>, if the key data is plain-text or if it is encrypted with the {@link UserRepoKey}
	 * referenced by {@link #getFromUserRepoKeyId() fromUserRepoKeyId}.
	 * @see #getFromUserRepoKeyId()
	 * @see #getToCryptoKeyData()
	 */
	public CryptoKey getFromCryptoKey() {
		return fromCryptoKey;
	}
	public void setFromCryptoKey(final CryptoKey fromCryptoKey) {
		if (! equal(this.fromCryptoKey, fromCryptoKey))
			this.fromCryptoKey = fromCryptoKey;
	}

//	/**
//	 * Gets the {@linkplain UserRepoKey#getUserRepoKeyId() user's master key ID} used to encrypt
//	 * {@link #getToCryptoKeyData() toCryptoKeyData}.
//	 * <p>
//	 * <b>Important:</b> This property must be <code>null</code>, if
//	 * {@link #getFromCryptoKey() fromCryptoKey} is not <code>null</code>. Only at most one of them can be a
//	 * non-<code>null</code> value (both can be <code>null</code>).
//	 * @return the universal identifier of the {@link UserRepoKey} used to encrypt
//	 * {@link #getToCryptoKeyData() toCryptoKeyData}. Is <code>null</code>, if the key data
//	 * is plain-text or if it is encrypted with {@link #getFromCryptoKey() fromCryptoKey}.
//	 * @see #getFromCryptoKey()
//	 * @see #getToCryptoKeyData()
//	 */
//	public Uid getFromUserRepoKeyId() {
//		return fromUserRepoKeyId == null ? null : new Uid(fromUserRepoKeyId);
//	}
//	public void setFromUserRepoKeyId(final Uid userRepoKeyId) {
//		this.fromUserRepoKeyId = userRepoKeyId == null ? null : userRepoKeyId.toString();
//	}
	public UserRepoKeyPublicKey getFromUserRepoKeyPublicKey() {
		return fromUserRepoKeyPublicKey;
	}
	public void setFromUserRepoKeyPublicKey(final UserRepoKeyPublicKey fromUserRepoKeyPublicKey) {
		if (! equal(this.fromUserRepoKeyPublicKey, fromUserRepoKeyPublicKey))
			this.fromUserRepoKeyPublicKey = fromUserRepoKeyPublicKey;
	}

	public CryptoKey getToCryptoKey() {
		return toCryptoKey;
	}
	public void setToCryptoKey(final CryptoKey cryptoKey) {
		if (! equal(this.toCryptoKey, cryptoKey))
			this.toCryptoKey = cryptoKey;
	}

	public CryptoKeyPart getToCryptoKeyPart() {
		return toCryptoKeyPart;
	}
	public void setToCryptoKeyPart(final CryptoKeyPart toCryptoKeyPart) {
		if (! equal(this.toCryptoKeyPart, toCryptoKeyPart))
			this.toCryptoKeyPart = toCryptoKeyPart;
	}

	/**
	 * Gets the actual key data (of {@link #getToCryptoKey() toCryptoKey}) - usually encrypted, but
	 * sometimes plain-text.
	 * <p>
	 * If it is encrypted, the key used for encryption is either {@link #getFromCryptoKey() fromCryptoKey}
	 * or the {@link UserRepoKey} referenced by {@link #getFromUserRepoKeyId() fromUserRepoKeyId}.
	 * <p>
	 * If {@code fromCryptoKey} and {@code fromUserRepoKeyId} are both <code>null</code>, this
	 * key data is plain-text (usually the case with a {@linkplain CryptoKeyPart#publicKey public key}).
	 * @return the actual key data (of {@link #getToCryptoKey() toCryptoKey}). Never <code>null</code> in
	 * persistence, but maybe <code>null</code> in memory.
	 * @see #getFromCryptoKey()
	 * @see #getFromUserRepoKeyId()
	 */
	public byte[] getToCryptoKeyData() {
		return toCryptoKeyData;
	}
	public void setToCryptoKeyData(final byte[] toCryptoKeyData) {
		if (! equal(this.toCryptoKeyData, toCryptoKeyData))
			this.toCryptoKeyData = toCryptoKeyData;
	}

	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(final long localRevision) {
		if (! equal(this.localRevision, localRevision))
			this.localRevision = localRevision;
	}

	@Override
	public void jdoPreStore() {
		getCryptoLinkId();

		if (fromCryptoKey != null && fromUserRepoKeyPublicKey != null)
			throw new IllegalStateException("fromCryptoKey != null && fromUserRepoKeyPublicKey != null :: toCryptoKeyData can only be encrypted with one key!");
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code CryptoLink} must exactly match the one in {@link CryptoLinkDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getCryptoLinkId()),
//					localRevision
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(fromCryptoKey == null ? null : fromCryptoKey.getCryptoKeyId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(fromUserRepoKeyPublicKey == null ? null : fromUserRepoKeyPublicKey.getUserRepoKeyId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(toCryptoKey.getCryptoKeyId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(toCryptoKeyPart.ordinal()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(toCryptoKeyData)
					);
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

// TODO BEGIN WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247
//		@Override
//		public Signature getSignature() {
//			return signature;
//		}
//		@Override
//		public void setSignature(final Signature signature) {
//			if (!equal(this.signature, signature))
//				this.signature = SignatureImpl.copy(signature);
//		}
	@Override
	public Signature getSignature() {
		String.valueOf(signatureCreated);
		String.valueOf(signingUserRepoKeyId);
		String.valueOf(signatureData);
		return SignableEmbeddedWorkaround.getSignature(this);
	}
	@Override
	public void setSignature(final Signature signature) {
		SignableEmbeddedWorkaround.setSignature(this, signature);
	}
// END WORKAROUND for http://www.datanucleus.org/servlet/jira/browse/NUCCORE-1247

	@Override
	public CryptoRepoFile getCryptoRepoFileControllingPermissions() {
		assertNotNull("toCryptoKey", toCryptoKey);
		return toCryptoKey.getCryptoRepoFile();
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		if (toCryptoKey == null)
			throw new IllegalStateException("toCryptoKey == null");

		final CryptoKeyRole cryptoKeyRole = toCryptoKey.getCryptoKeyRole();
		if (cryptoKeyRole == null)
			throw new IllegalStateException("toCryptoKey.cryptoKeyRole == null");

		return cryptoKeyRole == CryptoKeyRole.clearanceKey ? PermissionType.grant : PermissionType.write;
	}

	@Override
	public String toString() {
		return String.format("%s{cryptoLinkId=%s, fromCryptoKey=%s, fromUserRepoKeyPublicKey=%s, toCryptoKey=%s, toCryptoKeyPart=%s}",
				super.toString(),
				cryptoLinkId,
				fromCryptoKey,
				fromUserRepoKeyPublicKey,
				toCryptoKey,
				toCryptoKeyPart);
	}
}
