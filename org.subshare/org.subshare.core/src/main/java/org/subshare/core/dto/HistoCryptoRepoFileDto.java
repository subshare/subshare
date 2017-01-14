package org.subshare.core.dto;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.Signature;

import co.codewizards.cloudstore.core.Uid;

@SuppressWarnings("serial")
@XmlRootElement
public class HistoCryptoRepoFileDto implements Signable, Serializable {
	public static final String SIGNED_DATA_TYPE = "HistoCryptoRepoFile";

	private Uid histoCryptoRepoFileId;

	private Uid previousHistoCryptoRepoFileId;

	private Uid cryptoRepoFileId;

	private Uid histoFrameId;

	private Uid cryptoKeyId;

	private byte[] repoFileDtoData;

	private Date deleted;

	private boolean deletedByIgnoreRule;

//	private List<CollisionDto> collisionDtos;

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

	public Date getDeleted() {
		return deleted;
	}
	public void setDeleted(Date deleted) {
		this.deleted = deleted;
	}

	public boolean isDeletedByIgnoreRule() {
		return deletedByIgnoreRule;
	}
	public void setDeletedByIgnoreRule(boolean deletedByIgnoreRule) {
		this.deletedByIgnoreRule = deletedByIgnoreRule;
	}

//	public List<CollisionDto> getCollisionDtos() {
//		if (collisionDtos == null) {
//			collisionDtos = new ArrayList<>();
//		}
//		return collisionDtos;
//	}
//	public void setCollisionDtos(List<CollisionDto> collisionDtos) {
//		this.collisionDtos = collisionDtos;
//	}

	@Override
	public String getSignedDataType() {
		return HistoCryptoRepoFileDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 1;
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

			final List<InputStreamSource> inputStreamSources = new LinkedList<InputStreamSource>(Arrays.asList(
					InputStreamSource.Helper.createInputStreamSource(assertNotNull(histoCryptoRepoFileId, "histoCryptoRepoFileId")),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(previousHistoCryptoRepoFileId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(assertNotNull(cryptoRepoFileId, "cryptoRepoFileId")),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(assertNotNull(histoFrameId, "histoFrameId")),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(assertNotNull(cryptoKeyId, "cryptoKeyId")),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(repoFileDtoData),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(deleted)
					));

			if (signedDataVersion >= 1) {
				inputStreamSources.add(InputStreamSource.Helper.createInputStreamSource(++separatorIndex));
				inputStreamSources.add(InputStreamSource.Helper.createInputStreamSource(deletedByIgnoreRule));
			}

			// Sanity check for supported signedDataVersions.
			if (signedDataVersion < 0 || signedDataVersion > 1)
				throw new IllegalStateException("signedDataVersion=" + signedDataVersion);

			return new MultiInputStream(inputStreamSources);
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
		return "HistoCryptoRepoFileDto[histoCryptoRepoFileId=" + histoCryptoRepoFileId
				+ ", cryptoRepoFileId=" + cryptoRepoFileId
				+ ", cryptoKeyId=" + cryptoKeyId
				+ "]";
	}
}
