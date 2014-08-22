package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.HashSet;
import java.util.Set;

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
import org.subshare.core.dto.CryptoKeyType;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

/**
 * Key used for encryption.
 * <p>
 * An instance of {@code CryptoKey} is mostly immutable. The only changes allowed after initial persistence
 * are:
 * <ul>
 * <li>Switching {@link #isActive() active} from <code>true</code> to <code>false</code>. Switching back is
 * not possible.
 * <li>Adding new {@link CryptoLink} relations to {@link #getInCryptoLinks() inCryptoLinks} or
 * {@link #getOutCryptoLinks() outCryptoLinks}.
 * </ul>
 * <p>
 * Most importantly, the actual key which is contained in the
 * {@linkplain #getInCryptoLinks() incoming crypto-links}, must never change and all incoming crypto-links
 * must contain the same key. If it's an asymmetric key, then all parts of the key indicated by
 * {@link CryptoLink#getToCryptoKeyPart() toCryptoKeyPart} must fit together - and be the same, if the
 * {@link CryptoKeyPart} is the same.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Unique(name="CryptoKey_cryptoKeyId", members="cryptoKeyId")
@Indices({
	@Index(name="CryptoKey_localRevision", members="localRevision"),
	@Index(name="CryptoKey_cryptoRepoFile", members="cryptoRepoFile")
})
@Queries({
	@Query(name="getCryptoKey_cryptoKeyId", value="SELECT UNIQUE WHERE this.cryptoKeyId == :cryptoKeyId"),
	@Query(name="getCryptoKeysChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision")
})
public class CryptoKey extends Entity implements AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String cryptoKeyId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoRepoFile cryptoRepoFile;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(jdbcType="INTEGER")
	private CryptoKeyType cryptoKeyType;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(jdbcType="INTEGER")
	private CryptoKeyRole cryptoKeyRole;

	private long localRevision;

	@Persistent(mappedBy="toCryptoKey", dependentElement="true")
	private Set<CryptoLink> inCryptoLinks;

	@Persistent(mappedBy="fromCryptoKey")
	private Set<CryptoLink> outCryptoLinks;

	private boolean active = true;

	public CryptoKey() { }

	public CryptoKey(final Uid cryptoKeyId) {
		this.cryptoKeyId = cryptoKeyId == null ? null : cryptoKeyId.toString();
	}

	/**
	 * Gets the universal identifier of this key.
	 * <p>
	 * This universal identifier is unique across repositories.
	 * @return the universal identifier of this key. Never <code>null</code>.
	 */
	public Uid getCryptoKeyId() {
		if (cryptoKeyId == null)
			cryptoKeyId = new Uid().toString();

		return new Uid(cryptoKeyId);
	}

	public boolean isActive() {
		return active;
	}
	public void setActive(final boolean active) {
		if (! equal(this.active, active))
			this.active = active;
	}

	/**
	 * Gets the directory or file this key belongs to.
	 * @return the directory or file this key belongs to. Never <code>null</code> in persistence
	 * (maybe <code>null</code> temporarily in memory).
	 */
	public CryptoRepoFile getCryptoRepoFile() {
		return cryptoRepoFile;
	}
	public void setCryptoRepoFile(final CryptoRepoFile cryptoRepoFile) {
		if (! equal(this.cryptoRepoFile, cryptoRepoFile))
			this.cryptoRepoFile = cryptoRepoFile;
	}

	public CryptoKeyRole getCryptoKeyRole() {
		return cryptoKeyRole;
	}
	public void setCryptoKeyRole(final CryptoKeyRole cryptoKeyRole) {
		if (! equal(this.cryptoKeyRole, cryptoKeyRole))
			this.cryptoKeyRole = cryptoKeyRole;
	}

	public CryptoKeyType getCryptoKeyType() {
		return cryptoKeyType;
	}
	public void setCryptoKeyType(final CryptoKeyType cryptoKeyType) {
		if (! equal(this.cryptoKeyType, cryptoKeyType))
			this.cryptoKeyType = cryptoKeyType;
	}

	/**
	 * Gets the incoming crypto-links.
	 * <p>
	 * The actual key data of {@code this} key (i.e. the shared secret or the public/private key) are
	 * contained inside these {@link CryptoLink} instances.
	 * <p>
	 * Every incoming crypto-link references {@code this} via its
	 * {@link CryptoLink#getToCryptoKey() toCryptoKey} property.
	 * @return the incoming crypto-links. Never <code>null</code> and normally never empty, either.
	 */
	public Set<CryptoLink> getInCryptoLinks() {
		if (inCryptoLinks == null)
			inCryptoLinks = new HashSet<CryptoLink>();

		return inCryptoLinks;
	}

	/**
	 * Gets the outgoing crypto-links.
	 * <p>
	 * The actual key data contained in these {@link CryptoLink} instances is encrypted with {@code this}
	 * key.
	 * <p>
	 * Every outgoing crypto-link references {@code this} via its
	 * {@link CryptoLink#getFromCryptoKey() fromCryptoKey} property.
	 * @return the outgoing crypto-links. Never <code>null</code>, but maybe empty.
	 */
	public Set<CryptoLink> getOutCryptoLinks() {
		if (outCryptoLinks == null)
			outCryptoLinks = new HashSet<CryptoLink>();

		return outCryptoLinks;
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
		getCryptoKeyId();
	}

	@Override
	public String toString() {
		return String.format("%s{cryptoKeyId=%s, cryptoRepoFile=%s, cryptoKeyType=%s, cryptoKeyRole=%s}",
				super.toString(),
				cryptoKeyId,
				cryptoRepoFile,
				cryptoKeyType,
				cryptoKeyRole);
	}
}
