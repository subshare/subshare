package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class CryptoRepoFileDTO {

	private long repoFileId;

	private Uid cryptoKeyId;

	private byte[] encryptedRepoFileDTO;

	/**
	 * Gets the reference to {@link RepoFileDTO#getId() RepoFileDTO.id} - of the SubShare-server-side.
	 * @return the reference to {@link RepoFileDTO#getId() RepoFileDTO.id} - of the SubShare-server-side.
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

	public byte[] getEncryptedRepoFileDTO() {
		return encryptedRepoFileDTO;
	}
	public void setEncryptedRepoFileDTO(final byte[] encryptedRepoFileDTO) {
		this.encryptedRepoFileDTO = encryptedRepoFileDTO;
	}
}
