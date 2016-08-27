package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;

public class CryptoConfigPropSetDto {
	public static final String SIGNED_DATA_TYPE = "CryptoConfigPropSet";

	private Uid cryptoRepoFileId;

	private Uid cryptoKeyId;

	private byte[] configPropSetDtoData;

	@XmlElement
	private SignatureDto signatureDto;

	public String getSignedDataType() {
		return SIGNED_DATA_TYPE;
	}

	public Uid getCryptoRepoFileId() {
		return cryptoRepoFileId;
	}

	public void setCryptoRepoFileId(Uid cryptoRepoFileId) {
		this.cryptoRepoFileId = cryptoRepoFileId;
	}

	public Uid getCryptoKeyId() {
		return cryptoKeyId;
	}

	public void setCryptoKeyId(Uid cryptoKeyId) {
		this.cryptoKeyId = cryptoKeyId;
	}

	public byte[] getConfigPropSetDtoData() {
		return configPropSetDtoData;
	}

	public void setConfigPropSetDtoData(byte[] configPropSetDtoData) {
		this.configPropSetDtoData = configPropSetDtoData;
	}

	@XmlTransient
	public Signature getSignature() {
		return signatureDto;
	}

	public void setSignature(final Signature signature) {
		this.signatureDto = SignatureDto.copyIfNeeded(signature);
	}
}
