package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.RepoFileDto;

@XmlRootElement
public class RepoFileDtoWithCryptoRepoFileOnServerDto {

	private RepoFileDto repoFileDto;

	private CryptoRepoFileOnServerDto cryptoRepoFileOnServerDto;

	public RepoFileDto getRepoFileDto() {
		return repoFileDto;
	}
	public void setRepoFileDto(RepoFileDto repoFileDto) {
		this.repoFileDto = repoFileDto;
	}

	public CryptoRepoFileOnServerDto getCryptoRepoFileOnServerDto() {
		return cryptoRepoFileOnServerDto;
	}
	public void setCryptoRepoFileOnServerDto(CryptoRepoFileOnServerDto cryptoRepoFileOnServerDto) {
		this.cryptoRepoFileOnServerDto = cryptoRepoFileOnServerDto;
	}
}
