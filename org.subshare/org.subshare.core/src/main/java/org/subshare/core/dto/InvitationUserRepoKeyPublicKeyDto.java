package org.subshare.core.dto;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.sign.Signature;

@XmlRootElement
public class InvitationUserRepoKeyPublicKeyDto extends UserRepoKeyPublicKeyDto {

	private Date validTo;

	@XmlElement
	private SignatureDto signature;

	public Date getValidTo() {
		return validTo;
	}
	public void setValidTo(Date validTo) {
		this.validTo = validTo;
	}

	@XmlTransient
	public Signature getSignature() {
		return signature;
	}
	public void setSignature(Signature signature) {
		this.signature = SignatureDto.copyIfNeeded(signature);
	}
}
