package org.subshare.core.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;

@SuppressWarnings("serial")
@XmlRootElement
public class PlainHistoCryptoRepoFileDto implements Serializable {

	private Uid cryptoRepoFileId;
	private Uid parentCryptoRepoFileId;

	private HistoCryptoRepoFileDto histoCryptoRepoFileDto;

	private RepoFileDto repoFileDto;

	private List<CollisionDto> collisionDtos;

	private List<CollisionPrivateDto> collisionPrivateDtos;

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

	public List<CollisionDto> getCollisionDtos() {
		if (collisionDtos == null)
			collisionDtos = new ArrayList<>();

		return collisionDtos;
	}
	public void setCollisionDtos(List<CollisionDto> collisionDtos) {
		this.collisionDtos = collisionDtos;
	}

	public List<CollisionPrivateDto> getCollisionPrivateDtos() {
		if (collisionPrivateDtos == null)
			collisionPrivateDtos = new ArrayList<>();

		return collisionPrivateDtos;
	}
	public void setCollisionPrivateDtos(List<CollisionPrivateDto> collisionPrivateDtos) {
		this.collisionPrivateDtos = collisionPrivateDtos;
	}
}
