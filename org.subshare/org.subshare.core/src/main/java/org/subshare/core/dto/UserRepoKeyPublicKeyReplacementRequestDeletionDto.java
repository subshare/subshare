package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class UserRepoKeyPublicKeyReplacementRequestDeletionDto {

	private Uid requestId;

	private long localRevision;

	@XmlElement
	private SignatureDto signatureDto;

	public UserRepoKeyPublicKeyReplacementRequestDeletionDto() {
	}

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

	public long getLocalRevision() {
		return localRevision;
	}
	public void setLocalRevision(long localRevision) {
		this.localRevision = localRevision;
	}
}
