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

import co.codewizards.cloudstore.core.Uid;

@XmlRootElement
public class CurrentHistoCryptoRepoFileDto implements Signable {
	public static final String SIGNED_DATA_TYPE = "CurrentHistoCryptoRepoFile";

	// EITHER the following 2:
	private Uid cryptoRepoFileId;
	private Uid histoCryptoRepoFileId;
	// OR the following 1:
	private HistoCryptoRepoFileDto histoCryptoRepoFileDto;

	@XmlElement
	private SignatureDto signatureDto;

	public CurrentHistoCryptoRepoFileDto() {
	}

	public Uid getCryptoRepoFileId() {
		return cryptoRepoFileId;
	}
	public void setCryptoRepoFileId(Uid cryptoRepoFileId) {
		this.cryptoRepoFileId = cryptoRepoFileId;

		if (cryptoRepoFileId != null)
			this.setHistoCryptoRepoFileDto(null);
	}

	public Uid getHistoCryptoRepoFileId() {
		return histoCryptoRepoFileId;
	}
	public void setHistoCryptoRepoFileId(Uid histoCryptoRepoFileId) {
		this.histoCryptoRepoFileId = histoCryptoRepoFileId;

		if (histoCryptoRepoFileId != null)
			this.setHistoCryptoRepoFileDto(null);
	}

	public HistoCryptoRepoFileDto getHistoCryptoRepoFileDto() {
		return histoCryptoRepoFileDto;
	}
	public void setHistoCryptoRepoFileDto(HistoCryptoRepoFileDto histoCryptoRepoFileDto) {
		this.histoCryptoRepoFileDto = histoCryptoRepoFileDto;

		if (histoCryptoRepoFileDto != null) {
			this.setCryptoRepoFileId(null);
			this.setHistoCryptoRepoFileId(null);
		}
	}

	@Override
	public String getSignedDataType() {
		return CurrentHistoCryptoRepoFileDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code CurrentHistoCryptoRepoFile} must exactly match the one in {@code CurrentHistoCryptoRepoFileDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			final Uid cryptoRepoFileId = (histoCryptoRepoFileDto != null
					? histoCryptoRepoFileDto.getCryptoRepoFileId() : this.cryptoRepoFileId);

			final Uid histoCryptoRepoFileId = (histoCryptoRepoFileDto != null
					? histoCryptoRepoFileDto.getHistoCryptoRepoFileId() : this.histoCryptoRepoFileId);

			assertNotNull("cryptoRepoFileId", cryptoRepoFileId);
			assertNotNull("histoCryptoRepoFileId", histoCryptoRepoFileId);

			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(cryptoRepoFileId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(histoCryptoRepoFileId)
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
