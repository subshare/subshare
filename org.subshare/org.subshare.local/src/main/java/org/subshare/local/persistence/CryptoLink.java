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
	@Query(name="getCryptoLinksChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision")
})
public class CryptoLink extends Entity implements AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String cryptoLinkId;

	private long localRevision;

	private CryptoKey fromCryptoKey;

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
	 * @return the key used to encrypt {@link #getToCryptoKeyData() toCryptoKeyData}. May be
	 * <code>null</code>, if the key data is not encrypted.
	 */
	public CryptoKey getFromCryptoKey() {
		return fromCryptoKey;
	}
	public void setFromCryptoKey(final CryptoKey fromCryptoKey) {
		this.fromCryptoKey = fromCryptoKey;
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
	 * Gets the actual key data (of {@link #getToCryptoKey() toCryptoKey}) - usually encrypted with
	 * {@link #getFromCryptoKey() fromCryptoKey}. If {@code fromCryptoKey} is <code>null</code>, this
	 * key data is plaintext (usually the case with a {@linkplain CryptoKeyPart#publicKey public key}).
	 * @return the actual key data (of {@link #getToCryptoKey() toCryptoKey}). Never <code>null</code> in
	 * persistence, but maybe <code>null</code> in memory.
	 */
	public byte[] getToCryptoKeyData() {
		return toCryptoKeyData;
	}
	public void setToCryptoKeyData(final byte[] encryptedToCryptoKey) {
		this.toCryptoKeyData = encryptedToCryptoKey;
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
	}
}
