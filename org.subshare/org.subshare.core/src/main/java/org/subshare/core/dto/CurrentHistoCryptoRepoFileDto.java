package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class CurrentHistoCryptoRepoFileDto {

	private Uid cryptoRepoFileId;
	private Uid histoCryptoRepoFileId;

	public CurrentHistoCryptoRepoFileDto() {
	}

	public Uid getCryptoRepoFileId() {
		return cryptoRepoFileId;
	}
	public void setCryptoRepoFileId(Uid cryptoRepoFileId) {
		this.cryptoRepoFileId = cryptoRepoFileId;
	}

	public Uid getHistoCryptoRepoFileId() {
		return histoCryptoRepoFileId;
	}
	public void setHistoCryptoRepoFileId(Uid histoCryptoRepoFileId) {
		this.histoCryptoRepoFileId = histoCryptoRepoFileId;
	}
}
