package org.subshare.core.dto;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Arrays;
import java.util.Date;

import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;

public class SignatureDto implements Signature {

	private Date signatureCreated;

	private Uid signingUserRepoKeyId;

	private byte[] signatureData;

	public SignatureDto() { }

	public SignatureDto(final Date signatureCreated, final Uid signingUserRepoKeyId, final byte[] signatureData) {
		setSignatureCreated(signatureCreated);
		setSigningUserRepoKeyId(signingUserRepoKeyId);
		setSignatureData(signatureData);
	}

	@Override
	public Date getSignatureCreated() {
		return signatureCreated;
	}
	public void setSignatureCreated(final Date signatureCreated) {
		this.signatureCreated = signatureCreated;
	}

	@Override
	public Uid getSigningUserRepoKeyId() {
		return signingUserRepoKeyId;
	}
	public void setSigningUserRepoKeyId(final Uid signingUserRepoKeyId) {
		this.signingUserRepoKeyId = signingUserRepoKeyId;
	}

	@Override
	public byte[] getSignatureData() {
		return signatureData;
	}
	public void setSignatureData(final byte[] signatureData) {
		this.signatureData = signatureData;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((signatureCreated == null) ? 0 : signatureCreated.hashCode());
		result = prime * result + ((signingUserRepoKeyId == null) ? 0 : signingUserRepoKeyId.hashCode());
		result = prime * result + Arrays.hashCode(signatureData);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (! (obj instanceof Signature))
			return false;
		final Signature other = (Signature) obj;

		return equal(this.signatureCreated, other.getSignatureCreated())
				&& equal(this.signingUserRepoKeyId, other.getSigningUserRepoKeyId())
				&& equal(this.signatureData, other.getSignatureData());
	}

	public static SignatureDto copyIfNeeded(final Signature signature) {
		if (signature == null)
			return null;

		if (signature instanceof SignatureDto)
			return (SignatureDto) signature;

		return new SignatureDto(signature.getSignatureCreated(), signature.getSigningUserRepoKeyId(), signature.getSignatureData());
	}
}
