package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.RepoFileDto;

@XmlRootElement
public class RepoFileDtoWithCryptoRepoFileOnServerDto {

	private RepoFileDto repoFileDto;

	private HistoCryptoRepoFileDto histoCryptoRepoFileDto;

	public RepoFileDto getRepoFileDto() {
		return repoFileDto;
	}
	public void setRepoFileDto(RepoFileDto repoFileDto) {
		this.repoFileDto = repoFileDto;
	}

	public HistoCryptoRepoFileDto getCryptoRepoFileOnServerDto() {
		return histoCryptoRepoFileDto;
	}
	public void setCryptoRepoFileOnServerDto(HistoCryptoRepoFileDto histoCryptoRepoFileDto) {
		this.histoCryptoRepoFileDto = histoCryptoRepoFileDto;
	}
}
