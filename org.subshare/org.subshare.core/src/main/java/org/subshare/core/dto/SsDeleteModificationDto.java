package org.subshare.core.dto;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.dto.DeleteModificationDto;
import co.codewizards.cloudstore.core.dto.Uid;

public class SsDeleteModificationDto extends DeleteModificationDto implements WriteProtected {
	public static final String SIGNED_DATA_TYPE = "DeleteModification";

	private String serverPath;

	private Uid cryptoRepoFileIdControllingPermissions;

	@XmlElement
	private SignatureDto signatureDto;

	public String getServerPath() {
		return serverPath;
	}
	public void setServerPath(final String serverPath) {
		this.serverPath = serverPath;
	}

	@Override
	public String getSignedDataType() {
		return SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code SsDeleteModification} must exactly match the one in {@link SsDeleteModificationDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getServerPath()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getCryptoRepoFileIdControllingPermissions())
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

	@Override
	public Uid getCryptoRepoFileIdControllingPermissions() {
		return cryptoRepoFileIdControllingPermissions;
	}

	public void setCryptoRepoFileIdControllingPermissions(Uid cryptoRepoFileIdControllingPermissions) {
		this.cryptoRepoFileIdControllingPermissions = cryptoRepoFileIdControllingPermissions;
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.write;
	}
}
