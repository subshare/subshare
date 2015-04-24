package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class UserIdentityLinkDto {
	private Uid userIdentityLinkId;

	private Uid userIdentityId;

	private Uid forUserRepoKeyId;

	private byte[] encryptedUserIdentityKeyData;

	@XmlElement
	private SignatureDto signature;

	public Uid getUserIdentityLinkId() {
		return userIdentityLinkId;
	}

	public void setUserIdentityLinkId(Uid userIdentityLinkId) {
		this.userIdentityLinkId = userIdentityLinkId;
	}

	public Uid getUserIdentityId() {
		return userIdentityId;
	}
	public void setUserIdentityId(Uid userIdentityId) {
		this.userIdentityId = userIdentityId;
	}

	public Uid getForUserRepoKeyId() {
		return forUserRepoKeyId;
	}

	public void setForUserRepoKeyId(Uid forUserRepoKeyId) {
		this.forUserRepoKeyId = forUserRepoKeyId;
	}

	public byte[] getEncryptedUserIdentityKeyData() {
		return encryptedUserIdentityKeyData;
	}
	public void setEncryptedUserIdentityKeyData(final byte[] encryptedUserIdentityKeyData) {
		this.encryptedUserIdentityKeyData = encryptedUserIdentityKeyData;
	}

	@XmlTransient
	public Signature getSignature() {
		return signature;
	}
	public void setSignature(Signature signature) {
		this.signature = SignatureDto.copyIfNeeded(signature);
	}
}
