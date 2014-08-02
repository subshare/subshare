package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class CryptoLinkDto {

	private Uid cryptoLinkId;

	private long localRevision;

	private Uid fromCryptoKeyId;

	private Uid fromUserRepoKeyId;

	private Uid toCryptoKeyId;

	private CryptoKeyPart toCryptoKeyPart;

	private byte[] toCryptoKeyData;

	public Uid getCryptoLinkId() {
		return cryptoLinkId;
	}
	public void setCryptoLinkId(final Uid cryptoLinkId) {
		this.cryptoLinkId = cryptoLinkId;
	}

	public long getLocalRevision() {
		return localRevision;
	}
	public void setLocalRevision(final long localRevision) {
		this.localRevision = localRevision;
	}

	public Uid getFromCryptoKeyId() {
		return fromCryptoKeyId;
	}
	public void setFromCryptoKeyId(final Uid fromCryptoKeyId) {
		this.fromCryptoKeyId = fromCryptoKeyId;
	}

	public Uid getFromUserRepoKeyId() {
		return fromUserRepoKeyId;
	}
	public void setFromUserRepoKeyId(final Uid fromUserRepoKeyId) {
		this.fromUserRepoKeyId = fromUserRepoKeyId;
	}

	public Uid getToCryptoKeyId() {
		return toCryptoKeyId;
	}
	public void setToCryptoKeyId(final Uid toCryptoKeyId) {
		this.toCryptoKeyId = toCryptoKeyId;
	}

	public CryptoKeyPart getToCryptoKeyPart() {
		return toCryptoKeyPart;
	}
	public void setToCryptoKeyPart(final CryptoKeyPart toCryptoKeyPart) {
		this.toCryptoKeyPart = toCryptoKeyPart;
	}

	public byte[] getToCryptoKeyData() {
		return toCryptoKeyData;
	}
	public void setToCryptoKeyData(final byte[] toCryptoKeyData) {
		this.toCryptoKeyData = toCryptoKeyData;
	}
	@Override
	public String toString() {
		return "CryptoLinkDto [cryptoLinkId=" + cryptoLinkId
				+ ", localRevision=" + localRevision + ", fromCryptoKeyId="
				+ fromCryptoKeyId + ", fromUserRepoKeyId=" + fromUserRepoKeyId
				+ ", toCryptoKeyId=" + toCryptoKeyId + ", toCryptoKeyPart="
				+ toCryptoKeyPart + "]";
	}
}
