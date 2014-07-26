package org.subshare.local.persistence;

import java.util.HashSet;
import java.util.Set;

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

import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.CryptoKeyType;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;
import co.codewizards.cloudstore.local.persistence.RepoFile;

@PersistenceCapable
@Unique(name="CryptoKey_cryptoKeyId", members="cryptoKeyId")
@Indices({
	@Index(name="CryptoKey_localRevision", members={"localRevision"}),
	@Index(name="CryptoKey_repoFile", members={"repoFile"})
})
@Queries({
	@Query(name="getCryptoKeysChangedAfter_localRevision", value="SELECT WHERE this.localRevision > :localRevision")
})
public class CryptoKey extends Entity implements AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String cryptoKeyId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private RepoFile repoFile;

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
		this.active = active;
	}

	/**
	 * Gets the directory or file this key belongs to.
	 * @return the directory or file this key belongs to. Never <code>null</code> in persistence, but maybe
	 * <code>null</code> in memory.
	 */
	public RepoFile getRepoFile() {
		return repoFile;
	}
	public void setRepoFile(final RepoFile repoFile) {
		this.repoFile = repoFile;
	}

	public CryptoKeyRole getCryptoKeyRole() {
		return cryptoKeyRole;
	}
	public void setCryptoKeyRole(final CryptoKeyRole cryptoKeyRole) {
		this.cryptoKeyRole = cryptoKeyRole;
	}

	public CryptoKeyType getCryptoKeyType() {
		return cryptoKeyType;
	}
	public void setCryptoKeyType(final CryptoKeyType cryptoKeyType) {
		this.cryptoKeyType = cryptoKeyType;
	}

	public Set<CryptoLink> getInCryptoLinks() {
		if (inCryptoLinks == null)
			inCryptoLinks = new HashSet<CryptoLink>();

		return inCryptoLinks;
	}

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
		this.localRevision = localRevision;
	}

	@Override
	public void jdoPreStore() {
		getCryptoKeyId();
	}
}
