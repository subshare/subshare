package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Arrays;
import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.EmbeddedOnly;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;

@PersistenceCapable
@EmbeddedOnly
public class SignatureImpl implements Signature {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(name="signatureCreated")
	private Date signatureCreated;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(name="signingUserRepoKeyId", length=22)
	private String signingUserRepoKeyId;

	@Persistent(nullValue=NullValue.EXCEPTION, defaultFetchGroup="true")
	@Column(name="signatureData", jdbcType="BLOB")
	private byte[] signatureData;

	protected SignatureImpl() { }

	@Override
	public Date getSignatureCreated() {
		return signatureCreated;
	}
	private void setSignatureCreated(final Date signatureCreated) {
		if (this.signatureCreated != null && !this.signatureCreated.equals(signatureCreated))
			throw new IllegalStateException("this.signatureCreated already assigned to a different value! Cannot modify!");

		this.signatureCreated = signatureCreated;
	}

	@Override
	public Uid getSigningUserRepoKeyId() {
		return signingUserRepoKeyId == null ? null : new Uid(signingUserRepoKeyId);
	}
	private void setSigningUserRepoKeyId(final Uid signingUserRepoKeyId) {
		if (this.signingUserRepoKeyId != null && !this.signingUserRepoKeyId.equals(signingUserRepoKeyId))
			throw new IllegalStateException("this.signingUserRepoKeyId already assigned to a different value! Cannot modify!");

		this.signingUserRepoKeyId = signingUserRepoKeyId == null ? null : signingUserRepoKeyId.toString();
	}

	@Override
	public byte[] getSignatureData() {
		return signatureData;
	}
	private void setSignatureData(final byte[] signatureData) {
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
				&& equal(this.signingUserRepoKeyId, other.getSigningUserRepoKeyId().toString())
				&& equal(this.signatureData, other.getSignatureData());
	}

	public static SignatureImpl copy(final Signature signature) {
		if (signature == null)
			return null;

		assertNotNull("signature.signatureCreated", signature.getSignatureCreated());
		assertNotNull("signature.signingUserRepoKeyId", signature.getSigningUserRepoKeyId());
		assertNotNull("signature.signatureData", signature.getSignatureData());

		final SignatureImpl copy = new SignatureImpl();
		copy.setSignatureCreated(signature.getSignatureCreated());
		copy.setSigningUserRepoKeyId(signature.getSigningUserRepoKeyId());
		copy.setSignatureData(signature.getSignatureData());
		return copy;
	}
}
