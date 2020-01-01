package org.subshare.core.dto;

import static co.codewizards.cloudstore.core.util.DateUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.Uid;

@XmlRootElement
public class PermissionSetInheritanceDto implements Signable {
	public static final String SIGNED_DATA_TYPE = "PermissionSetInheritance";

	private Uid permissionSetInheritanceId;

	private Uid cryptoRepoFileId;

	private Date validFrom;

	private Date revoked;

	private Date validTo;

	@XmlElement
	private SignatureDto signatureDto;

	public Uid getPermissionSetInheritanceId() {
		return permissionSetInheritanceId;
	}
	public void setPermissionSetInheritanceId(final Uid permissionSetInheritanceId) {
		this.permissionSetInheritanceId = permissionSetInheritanceId;
	}

	public Uid getCryptoRepoFileId() {
		return cryptoRepoFileId;
	}
	public void setCryptoRepoFileId(final Uid cryptoRepoFileId) {
		this.cryptoRepoFileId = cryptoRepoFileId;
	}

	public Date getValidFrom() {
		return validFrom;
	}
	public void setValidFrom(final Date validFrom) {
		this.validFrom = copyDate(validFrom);
	}

	public Date getRevoked() {
		return revoked;
	}
	public void setRevoked(final Date revoked) {
		this.revoked = copyDate(revoked);
	}

	public Date getValidTo() {
		return validTo;
	}
	public void setValidTo(final Date validTo) {
		this.validTo = copyDate(validTo);
	}

	@Override
	public String getSignedDataType() {
		return PermissionSetInheritanceDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}
	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code PermissionSetInheritance} must exactly match the one in {code PermissionSetInheritanceDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(permissionSetInheritanceId),
//					localRevision
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(cryptoRepoFileId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(validFrom),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(revoked),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(validTo)
					);
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}
	@XmlTransient
	@Override
	public Signature getSignature() {
		return signatureDto;
	}

	@Override
	public void setSignature(final Signature signature) {
		this.signatureDto = SignatureDto.copyIfNeeded(signature);
	}
}
