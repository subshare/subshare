package org.subshare.core.dto;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.annotation.XmlRootElement;

import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signable;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class CryptoRepoFileDto implements Signable {

	private Uid cryptoRepoFileId;

	private Uid parentCryptoRepoFileId;

	private Uid cryptoKeyId;

	private boolean directory;

	private byte[] repoFileDtoData;

	private Uid signingUserRepoKeyId;

	private byte[] signatureData;

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

	@Override
	public Uid getSigningUserRepoKeyId() {
		return signingUserRepoKeyId;
	}
	@Override
	public void setSigningUserRepoKeyId(final Uid signingUserRepoKeyId) {
		this.signingUserRepoKeyId = signingUserRepoKeyId;
	}

	@Override
	public byte[] getSignatureData() {
		return signatureData;
	}
	@Override
	public void setSignatureData(final byte[] signatureData) {
		this.signatureData = signatureData;
	}

	@Override
	public String toString() {
		return "CryptoRepoFileDto[cryptoRepoFileId=" + cryptoRepoFileId
				+ ", parentCryptoRepoFileId=" + parentCryptoRepoFileId
				+ ", cryptoKeyId=" + cryptoKeyId + ", directory=" + directory + "]";
	}
}
