package org.subshare.core.dto;

import java.io.IOException;
import java.io.InputStream;
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

@XmlRootElement
public class CryptoRepoFileDto implements Signable {
	public static final String SIGNED_DATA_TYPE = "CryptoRepoFile";

	private Uid cryptoRepoFileId;

	private Uid parentCryptoRepoFileId;

	private Uid cryptoKeyId;

	private boolean directory;

	private byte[] repoFileDtoData;

	private Date cryptoRepoFileCreated;

	private Date deleted;

	private boolean deletedByIgnoreRule;

	@XmlElement
	private SignatureDto signatureDto;

	public CryptoRepoFileDto() {
	}

	/**
	 * JDO-direct-loading constructor.
	 * <p>
	 * <b>Important:</b> Do not use this constructor in Java code!
	 * <p>
	 * <b>Important:</b> When modifying this constructor, make sure all DAO-JDO-query-result-code matche it.
	 */
	public CryptoRepoFileDto(
			String cryptoRepoFileId,
			String parentCryptoRepoFileId,
			String cryptoKeyId,
			boolean directory,
			byte[] repoFileDtoData,
			Date cryptoRepoFileCreated,
			Date deleted,
			boolean deletedByIgnoreRule,
			Signature signature)
	{
		this.cryptoRepoFileId = Uid.valueOf(cryptoRepoFileId);
		this.parentCryptoRepoFileId = Uid.valueOf(parentCryptoRepoFileId);
		this.cryptoKeyId = Uid.valueOf(cryptoKeyId);
		this.directory = directory;
		this.repoFileDtoData = repoFileDtoData;
		this.cryptoRepoFileCreated = cryptoRepoFileCreated;
		this.deleted = deleted;
		this.deletedByIgnoreRule = deletedByIgnoreRule;
		this.setSignature(signature);
	}

	public Uid getCryptoRepoFileId() {
		return cryptoRepoFileId;
	}
	public void setCryptoRepoFileId(final Uid cryptoRepoFileId) {
		this.cryptoRepoFileId = cryptoRepoFileId;
	}

	public Uid getParentCryptoRepoFileId() {
		return parentCryptoRepoFileId;
	}
	public void setParentCryptoRepoFileId(final Uid parentCryptoRepoFileId) {
		this.parentCryptoRepoFileId = parentCryptoRepoFileId;
	}

	public Uid getCryptoKeyId() {
		return cryptoKeyId;
	}
	public void setCryptoKeyId(final Uid cryptoKeyId) {
		this.cryptoKeyId = cryptoKeyId;
	}

	public boolean isDirectory() {
		return directory;
	}
	public void setDirectory(final boolean directory) {
		this.directory = directory;
	}

	public byte[] getRepoFileDtoData() {
		return repoFileDtoData;
	}
	public void setRepoFileDtoData(final byte[] repoFileDtoData) {
		this.repoFileDtoData = repoFileDtoData;
	}

	public Date getCryptoRepoFileCreated() {
		return cryptoRepoFileCreated;
	}
	public void setCryptoRepoFileCreated(Date cryptoRepoFileCreated) {
		this.cryptoRepoFileCreated = cryptoRepoFileCreated;
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

	@Override
	public String getSignedDataType() {
		return CryptoRepoFileDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 2;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code CryptoRepoFile} must exactly match the one in {@code CryptoRepoFileDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;

			final List<InputStreamSource> inputStreamSources = new LinkedList<InputStreamSource>(Arrays.asList(
					InputStreamSource.Helper.createInputStreamSource(cryptoRepoFileId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(parentCryptoRepoFileId),
//			getRepoFile();
//			getLocalRevision();
//			getLastSyncFromRepositoryId(),
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(cryptoKeyId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(repoFileDtoData),
//			localName;
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(directory),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(deleted)
					));

			if (signedDataVersion >= 1) {
				inputStreamSources.add(InputStreamSource.Helper.createInputStreamSource(++separatorIndex));
				inputStreamSources.add(InputStreamSource.Helper.createInputStreamSource(deletedByIgnoreRule));
			}

			if (signedDataVersion >= 2) {
				inputStreamSources.add(InputStreamSource.Helper.createInputStreamSource(++separatorIndex));
				inputStreamSources.add(InputStreamSource.Helper.createInputStreamSource(cryptoRepoFileCreated));
			}

			// Sanity check for supported signedDataVersions.
			if (signedDataVersion < 0 || signedDataVersion > 2)
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
		return "CryptoRepoFileDto[cryptoRepoFileId=" + cryptoRepoFileId
				+ ", parentCryptoRepoFileId=" + parentCryptoRepoFileId
				+ ", cryptoKeyId=" + cryptoKeyId + ", directory=" + directory + "]";
	}
}
