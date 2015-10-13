package org.subshare.core.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class HistoCryptoRepoFileDto implements Signable {
	public static final String SIGNED_DATA_TYPE = "HistoCryptoRepoFile";

	private Uid histoCryptoRepoFileId;

	private Uid previousHistoCryptoRepoFileId;

	private Uid cryptoRepoFileId;

	private Uid histoFrameId;

	private Uid cryptoKeyId;

	private byte[] repoFileDtoData;

	@XmlElement
	private SignatureDto signatureDto;

	public Uid getHistoCryptoRepoFileId() {
		return histoCryptoRepoFileId;
	}
	public void setHistoCryptoRepoFileId(final Uid id) {
		this.histoCryptoRepoFileId = id;
	}

	public Uid getPreviousHistoCryptoRepoFileId() {
		return previousHistoCryptoRepoFileId;
	}
	public void setPreviousHistoCryptoRepoFileId(final Uid id) {
		this.previousHistoCryptoRepoFileId = id;
	}

	public Uid getCryptoRepoFileId() {
		return cryptoRepoFileId;
	}
	public void setCryptoRepoFileId(final Uid cryptoRepoFileId) {
		this.cryptoRepoFileId = cryptoRepoFileId;
	}

	public Uid getHistoFrameId() {
		return histoFrameId;
	}
	public void setHistoFrameId(Uid histoFrameId) {
		this.histoFrameId = histoFrameId;
	}

	public Uid getCryptoKeyId() {
		return cryptoKeyId;
	}
	public void setCryptoKeyId(final Uid cryptoKeyId) {
		this.cryptoKeyId = cryptoKeyId;
	}

	public byte[] getRepoFileDtoData() {
		return repoFileDtoData;
	}
	public void setRepoFileDtoData(final byte[] repoFileDtoData) {
		this.repoFileDtoData = repoFileDtoData;
	}

	@Override
	public String getSignedDataType() {
		return HistoCryptoRepoFileDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code CryptoRepoFileOnServer} must exactly match the one in {@code HistoCryptoRepoFileDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(assertNotNull("histoCryptoRepoFileId", histoCryptoRepoFileId)),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(previousHistoCryptoRepoFileId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(assertNotNull("cryptoRepoFileId", cryptoRepoFileId)),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(assertNotNull("histoFrameId", histoFrameId)),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(assertNotNull("cryptoKeyId", cryptoKeyId)),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(repoFileDtoData)
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
		return "HistoCryptoRepoFileDto[cryptoRepoFileId=" + cryptoRepoFileId
				+ ", cryptoKeyId=" + cryptoKeyId + "]";
	}
}
