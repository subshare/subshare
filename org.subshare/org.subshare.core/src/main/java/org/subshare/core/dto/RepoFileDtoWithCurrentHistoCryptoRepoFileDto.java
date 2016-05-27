package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.RepoFileDto;

@XmlRootElement
public class RepoFileDtoWithCurrentHistoCryptoRepoFileDto {

	private RepoFileDto repoFileDto;

	private CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto;

	public RepoFileDto getRepoFileDto() {
		return repoFileDto;
	}
	public void setRepoFileDto(RepoFileDto repoFileDto) {
		this.repoFileDto = repoFileDto;
	}

	public CurrentHistoCryptoRepoFileDto getCurrentHistoCryptoRepoFileDto() {
		return currentHistoCryptoRepoFileDto;
	}
	public void setCurrentHistoCryptoRepoFileDto(CurrentHistoCryptoRepoFileDto currentHistoCryptoRepoFileDto) {
		this.currentHistoCryptoRepoFileDto = currentHistoCryptoRepoFileDto;
	}
}
