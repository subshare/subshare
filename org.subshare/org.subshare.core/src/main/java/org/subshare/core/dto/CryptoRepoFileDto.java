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

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class CryptoRepoFileDto implements Signable {

	private Uid cryptoRepoFileId;

	private Uid parentCryptoRepoFileId;

	private Uid cryptoKeyId;

	private boolean directory;

	private byte[] repoFileDtoData;

	@XmlElement
	private SignatureDto signatureDto;

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

	@Override
	public int getSignedDataVersion() {
		return 0;
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
			return new MultiInputStream(
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
					InputStreamSource.Helper.createInputStreamSource(directory)
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
		return "CryptoRepoFileDto[cryptoRepoFileId=" + cryptoRepoFileId
				+ ", parentCryptoRepoFileId=" + parentCryptoRepoFileId
				+ ", cryptoKeyId=" + cryptoKeyId + ", directory=" + directory + "]";
	}
}
