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
public class CryptoLinkDto implements Signable {
	public static final String SIGNED_DATA_TYPE = "CryptoLink";

	private Uid cryptoLinkId;

	private long localRevision;

	private Uid fromCryptoKeyId;

	private Uid fromUserRepoKeyId;

	private Uid toCryptoKeyId;

	private CryptoKeyPart toCryptoKeyPart;

	private byte[] toCryptoKeyData;

	@XmlElement
	private SignatureDto signatureDto;

	public Uid getCryptoLinkId() {
		return cryptoLinkId;
	}
	public void setCryptoLinkId(final Uid cryptoLinkId) {
		this.cryptoLinkId = cryptoLinkId;
	}

	public long getLocalRevision() {
		return localRevision;
	}
	public void setLocalRevision(final long localRevision) {
		this.localRevision = localRevision;
	}

	public Uid getFromCryptoKeyId() {
		return fromCryptoKeyId;
	}
	public void setFromCryptoKeyId(final Uid fromCryptoKeyId) {
		this.fromCryptoKeyId = fromCryptoKeyId;
	}

	public Uid getFromUserRepoKeyId() {
		return fromUserRepoKeyId;
	}
	public void setFromUserRepoKeyId(final Uid fromUserRepoKeyId) {
		this.fromUserRepoKeyId = fromUserRepoKeyId;
	}

	public Uid getToCryptoKeyId() {
		return toCryptoKeyId;
	}
	public void setToCryptoKeyId(final Uid toCryptoKeyId) {
		this.toCryptoKeyId = toCryptoKeyId;
	}

	public CryptoKeyPart getToCryptoKeyPart() {
		return toCryptoKeyPart;
	}
	public void setToCryptoKeyPart(final CryptoKeyPart toCryptoKeyPart) {
		this.toCryptoKeyPart = toCryptoKeyPart;
	}

	public byte[] getToCryptoKeyData() {
		return toCryptoKeyData;
	}
	public void setToCryptoKeyData(final byte[] toCryptoKeyData) {
		this.toCryptoKeyData = toCryptoKeyData;
	}

	@Override
	public String getSignedDataType() {
		return CryptoLinkDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code CryptoLink} must exactly match the one in {@code CryptoLinkDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(cryptoLinkId),
//					localRevision
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(fromCryptoKeyId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(fromUserRepoKeyId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(toCryptoKeyId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(toCryptoKeyPart.ordinal()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(toCryptoKeyData)
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
		return "CryptoLinkDto[cryptoLinkId=" + cryptoLinkId
				+ ", localRevision=" + localRevision + ", fromCryptoKeyId="
				+ fromCryptoKeyId + ", fromUserRepoKeyId=" + fromUserRepoKeyId
				+ ", toCryptoKeyId=" + toCryptoKeyId + ", toCryptoKeyPart="
				+ toCryptoKeyPart + "]";
	}
}
