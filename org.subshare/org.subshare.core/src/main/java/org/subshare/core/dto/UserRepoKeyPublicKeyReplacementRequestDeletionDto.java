package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.Uid;

@XmlRootElement
public class UserRepoKeyPublicKeyReplacementRequestDeletionDto {
	public static final String SIGNED_DATA_TYPE = "UserRepoKeyPublicKeyReplacementRequestDeletion";

	private Uid requestId;

	private Uid oldUserRepoKeyId;

	@XmlElement
	private SignatureDto signatureDto;

	public Uid getRequestId() {
		return requestId;
	}
	public void setRequestId(Uid requestId) {
		this.requestId = requestId;
	}

	public Uid getOldUserRepoKeyId() {
		return oldUserRepoKeyId;
	}
	public void setOldUserRepoKeyId(Uid oldUserRepoKeyId) {
		this.oldUserRepoKeyId = oldUserRepoKeyId;
	}

	@XmlTransient
	public Signature getSignature() {
		return signatureDto;
	}
	public void setSignature(final Signature signature) {
		this.signatureDto = SignatureDto.copyIfNeeded(signature);
	}
}
