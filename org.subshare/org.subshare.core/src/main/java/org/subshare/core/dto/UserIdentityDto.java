package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.Uid;

@XmlRootElement
public class UserIdentityDto {
	public static final String SIGNED_DATA_TYPE = "UserIdentity";

	private Uid userIdentityId;

	private Uid ofUserRepoKeyId;

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

	public byte[] getEncryptedUserIdentityPayloadDto() {
		return encryptedUserIdentityPayloadDto;
	}

	public void setEncryptedUserIdentityPayloadDto(byte[] encryptedUserIdentityPayloadDto) {
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
