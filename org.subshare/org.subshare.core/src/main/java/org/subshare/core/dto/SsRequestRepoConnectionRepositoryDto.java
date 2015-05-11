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

import co.codewizards.cloudstore.core.dto.RepositoryDto;

@XmlRootElement
public class SsRequestRepoConnectionRepositoryDto extends RepositoryDto implements Signable {

	public static final String SIGNED_DATA_TYPE = "RequestRepoConnectionRepository";

	@XmlElement
	private SignatureDto signatureDto;

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	@Override
	public InputStream getSignedData(int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(SsRequestRepoConnectionRepositoryDto.SIGNED_DATA_TYPE),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getRepositoryId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getPublicKey())
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
