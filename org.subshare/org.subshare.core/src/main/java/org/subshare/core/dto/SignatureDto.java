package org.subshare.core.dto;

import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.Uid;

@SuppressWarnings("serial")
public class SignatureDto implements Signature, Serializable {

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
		if (this.signatureCreated != null && !this.signatureCreated.equals(signatureCreated))
			throw new IllegalStateException("this.signatureCreated already assigned to a different value! Cannot modify!");

		this.signatureCreated = signatureCreated;
	}

	@Override
	public Uid getSigningUserRepoKeyId() {
		return signingUserRepoKeyId;
	}
	public void setSigningUserRepoKeyId(final Uid signingUserRepoKeyId) {
		if (this.signingUserRepoKeyId != null && !this.signingUserRepoKeyId.equals(signingUserRepoKeyId))
			throw new IllegalStateException("this.signingUserRepoKeyId already assigned to a different value! Cannot modify!");

		this.signingUserRepoKeyId = signingUserRepoKeyId;
	}

	@Override
	public byte[] getSignatureData() {
		return signatureData;
	}
	public void setSignatureData(final byte[] signatureData) {
		if (this.signatureData != null && !Arrays.equals(this.signatureData, signatureData))
			throw new IllegalStateException("this.signatureData already assigned to a different value! Cannot modify!");

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

		requireNonNull(signature.getSignatureCreated(), "signature.signatureCreated");
		requireNonNull(signature.getSigningUserRepoKeyId(), "signature.signingUserRepoKeyId");
		requireNonNull(signature.getSignatureData(), "signature.signatureData");

		if (signature instanceof SignatureDto)
			return (SignatureDto) signature;

		return new SignatureDto(signature.getSignatureCreated(), signature.getSigningUserRepoKeyId(), signature.getSignatureData());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "signatureCreated=" + signatureCreated
				+ ", signingUserRepoKeyId=" + signingUserRepoKeyId;
	}
}
