package org.subshare.core.dto;

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

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class PermissionDto implements Signable {
	public static final String SIGNED_DATA_TYPE = "Permission";

	private Uid permissionId;

	private Uid cryptoRepoFileId;

	private Uid userRepoKeyId;

	private PermissionType permissionType;

	private Date validFrom;

	private Date revoked;

	private Date validTo;

	@XmlElement
	private SignatureDto signatureDto;

	public Uid getPermissionId() {
		return permissionId;
	}

	public void setPermissionId(final Uid permissionId) {
		this.permissionId = permissionId;
	}

	public Uid getCryptoRepoFileId() {
		return cryptoRepoFileId;
	}

	public void setCryptoRepoFileId(final Uid cryptoRepoFileId) {
		this.cryptoRepoFileId = cryptoRepoFileId;
	}

	public Uid getUserRepoKeyId() {
		return userRepoKeyId;
	}

	public void setUserRepoKeyId(final Uid userRepoKeyId) {
		this.userRepoKeyId = userRepoKeyId;
	}

	public PermissionType getPermissionType() {
		return permissionType;
	}

	public void setPermissionType(final PermissionType permissionType) {
		this.permissionType = permissionType;
	}

	public Date getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(final Date validFrom) {
		this.validFrom = validFrom;
	}

	public Date getRevoked() {
		return revoked;
	}

	public void setRevoked(final Date revoked) {
		this.revoked = revoked;
	}

	public Date getValidTo() {
		return validTo;
	}

	public void setValidTo(final Date validTo) {
		this.validTo = validTo;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code Permission} must exactly match the one in {@code PermissionDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(PermissionDto.SIGNED_DATA_TYPE),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getPermissionId()),
//					localRevision
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(cryptoRepoFileId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(userRepoKeyId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(permissionType.ordinal()),

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
