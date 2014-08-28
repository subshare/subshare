package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserRepoKeyDto {

	private UserRepoKeyPrivateKeyDto privateKeyDto;

	private UserRepoKeyPublicKeyDto publicKeyDto;

	public UserRepoKeyPrivateKeyDto getPrivateKeyDto() {
		return privateKeyDto;
	}
	public void setPrivateKeyDto(final UserRepoKeyPrivateKeyDto privateKeyDto) {
		this.privateKeyDto = privateKeyDto;
	}

	public UserRepoKeyPublicKeyDto getPublicKeyDto() {
		return publicKeyDto;
	}
	public void setPublicKeyDto(final UserRepoKeyPublicKeyDto publicKeyDto) {
		this.publicKeyDto = publicKeyDto;
	}

}
