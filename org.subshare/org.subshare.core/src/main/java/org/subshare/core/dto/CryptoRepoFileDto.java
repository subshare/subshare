package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class CryptoRepoFileDto {

	private long repoFileId = -1;

	private Uid cryptoKeyId;

	private byte[] repoFileDtoData;

	/**
	 * Gets the reference to {@link RepoFileDto#getId() RepoFileDto.id} - of the SubShare-server-side.
	 * @return the reference to {@link RepoFileDto#getId() RepoFileDto.id} - of the SubShare-server-side.
	 */
	public long getRepoFileId() {
		return repoFileId;
	}
	public void setRepoFileId(final long repoFileId) {
		this.repoFileId = repoFileId;
	}

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
}
