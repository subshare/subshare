package org.subshare.core.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.annotation.XmlRootElement;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.sign.PgpSignable;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class CreateRepositoryRequestDto implements PgpSignable {
	private static final String SIGNED_DATA_TYPE = "CreateRepositoryRequest";

	private Uid requestId;

	private byte[] pgpSignatureData;

	public Uid getRequestId() {
		return requestId;
	}
	public void setRequestId(Uid requestId) {
		this.requestId = requestId;
	}

	@Override
	public String getSignedDataType() {
		return SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	@Override
	public InputStream getSignedData(int signedDataVersion) {
		assertNotNull("requestId", requestId);
		try {
			return InputStreamSource.Helper.createInputStreamSource(requestId).createInputStream();
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	@Override
	public byte[] getPgpSignatureData() {
		return pgpSignatureData;
	}

	@Override
	public void setPgpSignatureData(byte[] pgpSignatureData) {
		this.pgpSignatureData = pgpSignatureData;
	}
}
