package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.Indices;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;
import javax.jdo.annotations.Uniques;
import javax.jdo.listener.StoreCallback;

import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.HistoCryptoRepoFileDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.dto.jaxb.RepoFileDtoIo;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

/**
 * Container holding the encrypted meta-data for a file (or directory) after the data was uploaded to the server.
 * <p>
 * There is a 1-1-relation to a {@link CryptoRepoFile} within the scope of a {@link HistoFrame}.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name="UK_HistoCryptoRepoFile_histoCryptoRepoFileId", members="histoCryptoRepoFileId"),
	@Unique(name="UK_HistoCryptoRepoFile_cryptoRepoFile_histoFrame", members={"cryptoRepoFile", "histoFrame"})
})
@Indices({
	@Index(name="HistoCryptoRepoFile_histoCryptoRepoFileId", members="histoCryptoRepoFileId"),
	@Index(name="HistoCryptoRepoFile_localRevision", members="localRevision"),
	@Index(name="HistoCryptoRepoFile_cryptoRepoFile_histoFrame", members={"cryptoRepoFile", "histoFrame"}),
	@Index(name="HistoCryptoRepoFile_histoFrame", members="histoFrame")
})
@Queries({
	@Query(name="getHistoCryptoRepoFile_histoCryptoRepoFileId", value="SELECT UNIQUE WHERE this.histoCryptoRepoFileId == :histoCryptoRepoFileId"),
	@Query(name="getHistoCryptoRepoFiles_cryptoRepoFile", value="SELECT WHERE this.cryptoRepoFile == :cryptoRepoFile"),
	@Query(name="getHistoCryptoRepoFiles_histoFrame", value="SELECT WHERE this.histoFrame == :histoFrame"),
	@Query(
			name="getHistoCryptoRepoFilesChangedAfter_localRevision",
			value="SELECT WHERE this.localRevision > :localRevision")
})
public class HistoCryptoRepoFile extends Entity implements WriteProtected, AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private String histoCryptoRepoFileId;

	private HistoCryptoRepoFile previousHistoCryptoRepoFile;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoRepoFile cryptoRepoFile;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private HistoFrame histoFrame;

	private long localRevision;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoKey cryptoKey; // might be different than the one of cryptoRepoFile, when it was just changed => keep separately.

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(jdbcType="BLOB")
	private byte[] repoFileDtoData;

	private Date deleted;

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

	public HistoCryptoRepoFile() { }

	public HistoCryptoRepoFile(final Uid histoCryptoRepoFileId) {
		this.histoCryptoRepoFileId = histoCryptoRepoFileId == null ? null : histoCryptoRepoFileId.toString();
	}

	public Uid getHistoCryptoRepoFileId() {
		if (histoCryptoRepoFileId == null)
			histoCryptoRepoFileId = new Uid().toString();

		return new Uid(histoCryptoRepoFileId);
	}

	public CryptoRepoFile getCryptoRepoFile() {
		return cryptoRepoFile;
	}
	public void setCryptoRepoFile(CryptoRepoFile cryptoRepoFile) {
		if (equal(this.cryptoRepoFile, cryptoRepoFile))
			return;

//		if (this.cryptoRepoFile != null) // We must allow re-assignment because of collisions on the server and the CryptoRepoFileMerger!
//			throw new IllegalStateException("this.cryptoRepoFile already assigned! Cannot re-assign!");

		this.cryptoRepoFile = cryptoRepoFile;
	}

	public HistoFrame getHistoFrame() {
		return histoFrame;
	}
	public void setHistoFrame(HistoFrame histoFrame) {
		if (equal(this.histoFrame, histoFrame))
			return;

		if (this.histoFrame != null)
			throw new IllegalStateException("this.histoFrame already assigned! Cannot re-assign!");

		this.histoFrame = histoFrame;
	}

	public HistoCryptoRepoFile getPreviousHistoCryptoRepoFile() {
		return previousHistoCryptoRepoFile;
	}
	public void setPreviousHistoCryptoRepoFile(final HistoCryptoRepoFile previousHistoCryptoRepoFile) {
		if (! equal(this.previousHistoCryptoRepoFile, previousHistoCryptoRepoFile))
			this.previousHistoCryptoRepoFile = previousHistoCryptoRepoFile;
	}

	/**
	 * Gets the key used to encrypt {@link #getRepoFileDtoData() repoFileDtoData} as well as the
	 * actual content of the file (if it is a normal file - no directory).
	 * @return the key used to encrypt {@link #getRepoFileDtoData() repoFileDtoData} and - if applicable -
	 * the file contents. Might be temporarily <code>null</code> during creation.
	 */
	public CryptoKey getCryptoKey() {
		return cryptoKey;
	}
	public void setCryptoKey(final CryptoKey cryptoKey) {
		if (! equal(this.cryptoKey, cryptoKey)) {
			if (cryptoKey != null) {
				final CryptoKeyRole cryptoKeyRole = assertNotNull("cryptoKey.cryptoKeyRole", cryptoKey.getCryptoKeyRole());
				if (CryptoKeyRole.dataKey != cryptoKeyRole)
					throw new IllegalArgumentException("cryptoKey.cryptoKeyRole != dataKey");
			}
			this.cryptoKey = cryptoKey;
		}
	}

	/**
	 * Gets the encrypted real meta-data (an instance of a sub-class of {@link RepoFileDto}).
	 * <p>
	 * This meta-data is encrypted on the client-side using the referenced {@link #getCryptoKey() cryptoKey}.
	 * Before encryption, it is serialised using {@link RepoFileDtoIo} and compressed by a {@link GZIPOutputStream}.
	 * @return the encrypted real meta-data (an instance of a sub-class of {@link RepoFileDto}). Might be temporarily <code>null</code>
	 * during creation.
	 */
	public byte[] getRepoFileDtoData() {
		return repoFileDtoData;
	}
	public void setRepoFileDtoData(final byte[] repoFileDtoData) {
		if (! equal(this.repoFileDtoData, repoFileDtoData))
			this.repoFileDtoData = repoFileDtoData;
	}

	public Date getDeleted() {
		return deleted;
	}
	public void setDeleted(Date deleted) {
		if (! equal(this.deleted, deleted))
			this.deleted = deleted;
	}

	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(final long localRevision) {
		if (! equal(this.localRevision, localRevision))
			this.localRevision = localRevision;
	}

	@Override
	public void jdoPreStore() {
		getHistoCryptoRepoFileId();
	}

	@Override
	public String getSignedDataType() {
		return HistoCryptoRepoFileDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

//	public String getLastSyncFromRepositoryId() {
//		return lastSyncFromRepositoryId;
//	}
//	public void setLastSyncFromRepositoryId(String lastSyncFromRepositoryId) {
//		this.lastSyncFromRepositoryId = lastSyncFromRepositoryId;
//	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code HistoCryptoRepoFile} must exactly match the one in {@link HistoCryptoRepoFileDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(getHistoCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(
							previousHistoCryptoRepoFile == null ? null : previousHistoCryptoRepoFile.getHistoCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(assertNotNull("cryptoRepoFile", cryptoRepoFile).getCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(assertNotNull("histoFrame", histoFrame).getHistoFrameId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(assertNotNull("cryptoKey", cryptoKey).getCryptoKeyId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getRepoFileDtoData()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(deleted)
					);
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
	}

	@Override
	public Signature getSignature() {
		return signature;
	}
	@Override
	public void setSignature(final Signature signature) {
		if (!equal(this.signature, signature))
			this.signature = SignatureImpl.copy(signature);
	}

	@Override
	public Uid getCryptoRepoFileIdControllingPermissions() {
		return assertNotNull("cryptoRepoFileId", assertNotNull("cryptoRepoFile", cryptoRepoFile).getCryptoRepoFileId());
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.write;
	}

	@Override
	public String toString() {
		return String.format("%s{cryptoRepoFileId=%s}",
				super.toString(),
				cryptoRepoFile == null ? null : cryptoRepoFile.getCryptoRepoFileId());
	}
}
