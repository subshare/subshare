package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class UserRepoKeyPublicKeyReplacementRequestDeletionDto {
	public static final String SIGNED_DATA_TYPE = "UserRepoKeyPublicKeyReplacementRequestDeletion";

	private Uid requestId;

	@XmlElement
	private SignatureDto signatureDto;

	public Uid getRequestId() {
		return requestId;
	}
	public void setRequestId(Uid requestId) {
		this.requestId = requestId;
	}

	@XmlTransient
	public Signature getSignature() {
		return signatureDto;
	}
	public void setSignature(final Signature signature) {
		this.signatureDto = SignatureDto.copyIfNeeded(signature);
	}
}
