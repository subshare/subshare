package org.subshare.core.dto;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.Uid;

@XmlRootElement
public class CryptoKeyDto implements Signable {
	public static final String SIGNED_DATA_TYPE = "CryptoKey";

	private Uid cryptoKeyId;

	private Uid cryptoRepoFileId;

	private CryptoKeyType cryptoKeyType;

	private CryptoKeyRole cryptoKeyRole;

	private long localRevision;

	@XmlElement
	private SignatureDto signatureDto;

	private CryptoKeyDeactivationDto cryptoKeyDeactivationDto;

	public Uid getCryptoKeyId() {
		return cryptoKeyId;
	}
	public void setCryptoKeyId(final Uid cryptoKeyId) {
		this.cryptoKeyId = cryptoKeyId;
	}

	public Uid getCryptoRepoFileId() {
		return cryptoRepoFileId;
	}
	public void setCryptoRepoFileId(final Uid cryptoRepoFileId) {
		this.cryptoRepoFileId = cryptoRepoFileId;
	}

	public CryptoKeyType getCryptoKeyType() {
		return cryptoKeyType;
	}
	public void setCryptoKeyType(final CryptoKeyType cryptoKeyType) {
		this.cryptoKeyType = cryptoKeyType;
	}

	public CryptoKeyRole getCryptoKeyRole() {
		return cryptoKeyRole;
	}
	public void setCryptoKeyRole(final CryptoKeyRole cryptoKeyRole) {
		this.cryptoKeyRole = cryptoKeyRole;
	}

	public long getLocalRevision() {
		return localRevision;
	}
	public void setLocalRevision(final long localRevision) {
		this.localRevision = localRevision;
	}

	public CryptoKeyDeactivationDto getCryptoKeyDeactivationDto() {
		return cryptoKeyDeactivationDto;
	}
	public void setCryptoKeyDeactivationDto(final CryptoKeyDeactivationDto cryptoKeyDeactivationDto) {
		this.cryptoKeyDeactivationDto = cryptoKeyDeactivationDto;
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
	 * <b>Important:</b> The implementation in {@code CryptoKey} must exactly match the one in {@code CryptoKeyDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(cryptoKeyId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(cryptoRepoFileId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(cryptoKeyRole.ordinal()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(cryptoKeyType.ordinal())
//					localRevision
//					inCryptoLinks
//					outCryptoLinks
//					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
//					InputStreamSource.Helper.createInputStreamSource(active)
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
	public String toString() {
		return "CryptoKeyDto[cryptoKeyId=" + cryptoKeyId
				+ ", cryptoRepoFileId=" + cryptoRepoFileId + ", cryptoKeyType=" + cryptoKeyType
				+ ", cryptoKeyRole=" + cryptoKeyRole + ", localRevision="
				+ localRevision + ", cryptoKeyDeactivationDto=" + cryptoKeyDeactivationDto + "]";
	}
}
