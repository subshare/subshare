package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class CryptoKeyDto {

	private Uid cryptoKeyId;

	private Uid cryptoRepoFileId;

	private boolean active;

	private CryptoKeyType cryptoKeyType;

	private CryptoKeyRole cryptoKeyRole;

	private long localRevision;

	public Uid getCryptoKeyId() {
		return cryptoKeyId;
	}
	public void setCryptoKeyId(final Uid cryptoKeyId) {
		this.cryptoKeyId = cryptoKeyId;
	}

	public Uid getCryptoRepoFileId() {
		return cryptoRepoFileId;
	}
	public void setCryptoRepoFileId(final Uid cryptoRepoFileId) {
		this.cryptoRepoFileId = cryptoRepoFileId;
	}

	public boolean isActive() {
		return active;
	}
	public void setActive(final boolean active) {
		this.active = active;
	}

	public CryptoKeyType getCryptoKeyType() {
		return cryptoKeyType;
	}
	public void setCryptoKeyType(final CryptoKeyType cryptoKeyType) {
		this.cryptoKeyType = cryptoKeyType;
	}

	public CryptoKeyRole getCryptoKeyRole() {
		return cryptoKeyRole;
	}
	public void setCryptoKeyRole(final CryptoKeyRole cryptoKeyRole) {
		this.cryptoKeyRole = cryptoKeyRole;
	}

	public long getLocalRevision() {
		return localRevision;
	}
	public void setLocalRevision(final long localRevision) {
		this.localRevision = localRevision;
	}

	@Override
	public String toString() {
		return "CryptoKeyDto[cryptoKeyId=" + cryptoKeyId
				+ ", cryptoRepoFileId=" + cryptoRepoFileId + ", active="
				+ active + ", cryptoKeyType=" + cryptoKeyType
				+ ", cryptoKeyRole=" + cryptoKeyRole + ", localRevision="
				+ localRevision + "]";
	}
}
