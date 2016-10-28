package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.Uid;

@XmlRootElement
public class UserRepoKeyPublicKeyReplacementRequestDto {
	public static final String SIGNED_DATA_TYPE = "UserRepoKeyPublicKeyReplacementRequest";

	private Uid requestId;

	private Uid oldKeyId;

	private Uid newKeyId;

	private long localRevision;

	@XmlElement
	private SignatureDto signatureDto;

	public Uid getRequestId() {
		return requestId;
	}

	public void setRequestId(Uid requestId) {
		this.requestId = requestId;
	}

	public Uid getOldKeyId() {
		return oldKeyId;
	}
	public void setOldKeyId(Uid oldKeyId) {
		this.oldKeyId = oldKeyId;
	}

	public Uid getNewKeyId() {
		return newKeyId;
	}
	public void setNewKeyId(Uid newKeyId) {
		this.newKeyId = newKeyId;
	}

	public long getLocalRevision() {
		return localRevision;
	}
	public void setLocalRevision(long localRevision) {
		this.localRevision = localRevision;
	}

	@XmlTransient
	public Signature getSignature() {
		return signatureDto;
	}
	public void setSignature(final Signature signature) {
		this.signatureDto = SignatureDto.copyIfNeeded(signature);
	}

}
