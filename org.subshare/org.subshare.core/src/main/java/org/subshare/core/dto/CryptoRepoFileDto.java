package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class CryptoRepoFileDto {

	private Uid cryptoRepoFileId;

	private Uid parentCryptoRepoFileId;

//	private long repoFileId = -1;

	private Uid cryptoKeyId;

	private byte[] repoFileDtoData;

	public Uid getCryptoRepoFileId() {
		return cryptoRepoFileId;
	}
	public void setCryptoRepoFileId(final Uid cryptoRepoFileId) {
		this.cryptoRepoFileId = cryptoRepoFileId;
	}

	public Uid getParentCryptoRepoFileId() {
		return parentCryptoRepoFileId;
	}
	public void setParentCryptoRepoFileId(final Uid parentCryptoRepoFileId) {
		this.parentCryptoRepoFileId = parentCryptoRepoFileId;
	}

//	/**
//	 * Gets the reference to {@link RepoFileDto#getId() RepoFileDto.id} - of the SubShare-server-side.
//	 * @return the reference to {@link RepoFileDto#getId() RepoFileDto.id} - of the SubShare-server-side.
//	 */
//	public long getRepoFileId() {
//		return repoFileId;
//	}
//	public void setRepoFileId(final long repoFileId) {
//		this.repoFileId = repoFileId;
//	}

	public Uid getCryptoKeyId() {
		return cryptoKeyId;
	}
	public void setCryptoKeyId(final Uid cryptoKeyId) {
		this.cryptoKeyId = cryptoKeyId;
	}

	public byte[] getRepoFileDtoData() {
		return repoFileDtoData;
	}
	public void setRepoFileDtoData(final byte[] repoFileDtoData) {
		this.repoFileDtoData = repoFileDtoData;
	}

	@Override
	public String toString() {
		return "CryptoRepoFileDto [cryptoRepoFileId=" + cryptoRepoFileId
				+ ", parentCryptoRepoFileId=" + parentCryptoRepoFileId
				+ ", cryptoKeyId=" + cryptoKeyId + "]";
	}
}
