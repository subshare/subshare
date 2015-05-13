package org.subshare.core.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.PgpSignable;

@XmlRootElement
public class CreateRepositoryRequestDto implements PgpSignable {
	private static final String SIGNED_DATA_TYPE = "CreateRepositoryRequest";

	private UUID serverRepositoryId;

	private byte[] pgpSignatureData;

	public UUID getServerRepositoryId() {
		return serverRepositoryId;
	}
	public void setServerRepositoryId(UUID serverRepositoryId) {
		this.serverRepositoryId = serverRepositoryId;
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
		assertNotNull("serverRepositoryId", serverRepositoryId);
		try {
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(serverRepositoryId)
					);
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
