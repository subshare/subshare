package org.subshare.core.dto;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.Uid;

@SuppressWarnings("serial")
@XmlRootElement
public class HistoFrameDto implements Signable, Serializable {
	public static final String SIGNED_DATA_TYPE = "HistoFrame";

	private Uid histoFrameId;
	private UUID fromRepositoryId;
	private Date sealed;

	@XmlElement
	private SignatureDto signatureDto;

	public Uid getHistoFrameId() {
		return histoFrameId;
	}
	public void setHistoFrameId(Uid histoFrameId) {
		this.histoFrameId = histoFrameId;
	}

	public UUID getFromRepositoryId() {
		return fromRepositoryId;
	}
	public void setFromRepositoryId(UUID fromRepositoryId) {
		this.fromRepositoryId = fromRepositoryId;
	}

	public Date getSealed() {
		return sealed;
	}
	public void setSealed(Date sealed) {
		this.sealed = sealed;
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
	 * <b>Important:</b> The implementation in {@code HistoFrame} must exactly match the one in {@code HistoFrameDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(histoFrameId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(fromRepositoryId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(sealed)
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
