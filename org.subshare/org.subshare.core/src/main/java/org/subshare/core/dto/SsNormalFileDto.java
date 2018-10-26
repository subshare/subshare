package org.subshare.core.dto;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.dto.NormalFileDto;

@SuppressWarnings("serial")
@XmlRootElement
public class SsNormalFileDto extends NormalFileDto implements SsRepoFileDto {
	public static final String SIGNED_DATA_TYPE = "NormalFile";

	private String parentName;

	@XmlElement
	private SignatureDto signatureDto;

	private long lengthWithPadding = -1;

	@Override
	public String getParentName() {
		return parentName;
	}
	@Override
	public void setParentName(final String parentName) {
		this.parentName = parentName;
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
	 * <b>Important:</b> The implementation in {@code SsNormalFile} must exactly match the one in {@code SsNormalFileDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getName()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(parentName),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getLength()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getLengthWithPadding()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getLastModified())
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

	public long getLengthWithPadding() {
		return lengthWithPadding;
	}
	public void setLengthWithPadding(long paddingLength) {
		this.lengthWithPadding = paddingLength;
	}

	@Override
	protected String toString_getProperties() {
		return super.toString_getProperties()
				+ ", lengthWithPadding=" + lengthWithPadding
				+ ", parentName=" + parentName
				+ ", signatureDto=" + signatureDto;
	}
}