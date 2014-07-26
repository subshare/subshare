package org.subshare.local.persistence;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Indices;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;
import javax.jdo.listener.StoreCallback;

import org.subshare.core.dto.CryptoKeyPart;
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
@Unique(name="CryptoLink_cryptoLinkId", members="cryptoLinkId")
@Indices({
	@Index(name="CryptoLink_localRevision", members={"localRevision"})
})
@Queries({
	@Query(name="getCryptoLinksChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision"),
	@Query(
			name="getActiveCryptoLinks_toRepoFile_toCryptoKeyRole_toCryptoKeyPart",
			value="SELECT WHERE "
					+ "this.toCryptoKey.active == true && "
					+ "this.toCryptoKey.repoFile == :toRepoFile && "
					+ "this.toCryptoKey.cryptoKeyRole == :toCryptoKeyRole && "
					+ "this.toCryptoKeyPart == :toCryptoKeyPart")
})
public class CryptoLink extends Entity implements AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String cryptoLinkId;

	private long localRevision;

	private CryptoKey fromCryptoKey;

	private String fromUserRepoKeyId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoKey toCryptoKey;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(jdbcType="INTEGER")
	private CryptoKeyPart toCryptoKeyPart;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] toCryptoKeyData;

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
		this.fromCryptoKey = fromCryptoKey;
	}

	/**
	 * Gets the {@linkplain UserRepoKey#getUserRepoKeyId() user's master key ID} used to encrypt
	 * {@link #getToCryptoKeyData() toCryptoKeyData}.
	 * <p>
	 * <b>Important:</b> This property must be <code>null</code>, if
	 * {@link #getFromCryptoKey() fromCryptoKey} is not <code>null</code>. Only at most one of them can be a
	 * non-<code>null</code> value (both can be <code>null</code>).
	 * @return the universal identifier of the {@link UserRepoKey} used to encrypt
	 * {@link #getToCryptoKeyData() toCryptoKeyData}. Is <code>null</code>, if the key data
	 * is plain-text or if it is encrypted with {@link #getFromCryptoKey() fromCryptoKey}.
	 * @see #getFromCryptoKey()
	 * @see #getToCryptoKeyData()
	 */
	public Uid getFromUserRepoKeyId() {
		return fromUserRepoKeyId == null ? null : new Uid(fromUserRepoKeyId);
	}
	public void setFromUserRepoKeyId(final Uid userRepoKeyId) {
		this.fromUserRepoKeyId = userRepoKeyId == null ? null : userRepoKeyId.toString();
	}

	public CryptoKey getToCryptoKey() {
		return toCryptoKey;
	}
	public void setToCryptoKey(final CryptoKey cryptoKey) {
		this.toCryptoKey = cryptoKey;
	}

	public CryptoKeyPart getToCryptoKeyPart() {
		return toCryptoKeyPart;
	}
	public void setToCryptoKeyPart(final CryptoKeyPart toCryptoKeyPart) {
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
		this.toCryptoKeyData = toCryptoKeyData;
	}

	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(final long localRevision) {
		this.localRevision = localRevision;
	}

	@Override
	public void jdoPreStore() {
		getCryptoLinkId();

		if (fromCryptoKey != null && fromUserRepoKeyId != null)
			throw new IllegalStateException("fromCryptoKey != null && fromUserRepoKeyId != null :: toCryptoKeyData can only be encrypted with one key!");
	}
}
