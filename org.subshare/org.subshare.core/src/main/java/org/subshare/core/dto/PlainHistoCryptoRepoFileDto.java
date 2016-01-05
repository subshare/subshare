package org.subshare.core.dto;

import java.io.Serializable;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;

@SuppressWarnings("serial")
public class PlainHistoCryptoRepoFileDto implements Serializable {

	private Uid cryptoRepoFileId;
	private Uid parentCryptoRepoFileId;

	private HistoCryptoRepoFileDto histoCryptoRepoFileDto;

	private RepoFileDto repoFileDto;

	public PlainHistoCryptoRepoFileDto() {
	}

	public Uid getCryptoRepoFileId() {
		return cryptoRepoFileId;
	}
	public void setCryptoRepoFileId(Uid cryptoRepoFileId) {
		this.cryptoRepoFileId = cryptoRepoFileId;
	}

	public Uid getParentCryptoRepoFileId() {
		return parentCryptoRepoFileId;
	}
	public void setParentCryptoRepoFileId(Uid parentCryptoRepoFileId) {
		this.parentCryptoRepoFileId = parentCryptoRepoFileId;
	}

	public HistoCryptoRepoFileDto getHistoCryptoRepoFileDto() {
		return histoCryptoRepoFileDto;
	}
	public void setHistoCryptoRepoFileDto(HistoCryptoRepoFileDto histoCryptoRepoFileDto) {
		this.histoCryptoRepoFileDto = histoCryptoRepoFileDto;
	}

	public RepoFileDto getRepoFileDto() {
		return repoFileDto;
	}
	public void setRepoFileDto(RepoFileDto repoFileDto) {
		this.repoFileDto = repoFileDto;
	}
}
