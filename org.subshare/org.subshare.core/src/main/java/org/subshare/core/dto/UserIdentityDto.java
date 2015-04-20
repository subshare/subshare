package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class UserIdentityDto {
	private Uid userIdentityId;

	private Uid ofUserRepoKeyId;

	private Uid forUserRepoKeyId;

	private byte[] encryptedUserIdentityPayloadDto;

	@XmlElement
	private SignatureDto signature;

	public Uid getUserIdentityId() {
		return userIdentityId;
	}

	public void setUserIdentityId(Uid userIdentityId) {
		this.userIdentityId = userIdentityId;
	}

	public Uid getOfUserRepoKeyId() {
		return ofUserRepoKeyId;
	}

	public void setOfUserRepoKeyId(Uid ofUserRepoKeyId) {
		this.ofUserRepoKeyId = ofUserRepoKeyId;
	}

	public Uid getForUserRepoKeyId() {
		return forUserRepoKeyId;
	}

	public void setForUserRepoKeyId(Uid forUserRepoKeyId) {
		this.forUserRepoKeyId = forUserRepoKeyId;
	}

	public byte[] getEncryptedUserIdentityPayloadDto() {
		return encryptedUserIdentityPayloadDto;
	}

	public void setEncryptedUserIdentityPayloadDto(
			byte[] encryptedUserIdentityPayloadDto) {
		this.encryptedUserIdentityPayloadDto = encryptedUserIdentityPayloadDto;
	}

	@XmlTransient
	public Signature getSignature() {
		return signature;
	}
	public void setSignature(Signature signature) {
		this.signature = SignatureDto.copyIfNeeded(signature);
	}
}
