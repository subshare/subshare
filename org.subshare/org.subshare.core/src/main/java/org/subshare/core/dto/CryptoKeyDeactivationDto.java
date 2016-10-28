package org.subshare.core.dto;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.Uid;

@XmlRootElement
public class CryptoKeyDeactivationDto implements Signable {
	public static final String SIGNED_DATA_TYPE = "CryptoKeyDeactivation";

	private Uid cryptoKeyId;

	@XmlElement
	private SignatureDto signatureDto;

	public Uid getCryptoKeyId() {
		return cryptoKeyId;
	}
	public void setCryptoKeyId(final Uid cryptoKeyId) {
		this.cryptoKeyId = cryptoKeyId;
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
	 * <b>Important:</b> The implementation in {@code CryptoKeyDeactivation} must exactly match the one in {@code CryptoKeyDeactivationDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			return InputStreamSource.Helper.createInputStreamSource(cryptoKeyId).createInputStream();
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
		return "CryptoKeyDeactivationDto[cryptoKeyId=" + cryptoKeyId + "]";
	}

}
