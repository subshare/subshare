package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.FetchGroup;
import javax.jdo.annotations.FetchGroups;
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

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.jaxb.RepoFileDtoIo;
import co.codewizards.cloudstore.local.db.IgnoreDatabaseMigraterComparison;
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

	@Query(name="getHistoCryptoRepoFilesByCollisions",
			value="SELECT WHERE"
					+ "  collisionIds.contains(collision.collisionId)"
					+ "  && (this == collision.histoCryptoRepoFile1 || this == collision.histoCryptoRepoFile2) "
					+ "VARIABLES org.subshare.local.persistence.Collision collision "
					+ "PARAMETERS java.util.Set collisionIds"),

	@Query(
			name="getHistoCryptoRepoFilesChangedAfter_localRevision_exclLastSyncFromRepositoryId",
			value="SELECT WHERE this.localRevision > :localRevision && (this.lastSyncFromRepositoryId == null || this.lastSyncFromRepositoryId != :lastSyncFromRepositoryId)"), // TODO this necessary == null is IMHO a DN bug!
	@Query(
			name="getHistoCryptoRepoFilesWithoutPlainHistoCryptoRepoFile",
			value="SELECT WHERE 0 == (SELECT count(p) FROM org.subshare.local.persistence.PlainHistoCryptoRepoFile p WHERE p.histoCryptoRepoFile == this)")
})
@FetchGroups({
	@FetchGroup(name = FetchGroupConst.HISTO_CRYPTO_REPO_FILE_DTO, members = {
			@Persistent(name = "histoFrame"),
			@Persistent(name = "cryptoRepoFile"),
			@Persistent(name = "previousHistoCryptoRepoFile"),
			@Persistent(name = "cryptoKey"),
			@Persistent(name = "repoFileDtoData"),
			@Persistent(name = "signature")
	}),
	@FetchGroup(name = FetchGroupConst.SIGNATURE, members = {
			@Persistent(name = "signature")
	})
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

	// TODO 1: The direct partner-repository from which this was synced, should be a real relation to the RemoteRepository,
	// because this is more efficient (not a String, but a long id).
	// TODO 2: We should additionally store (and forward) the origin repositoryId (UUID/String) to use this feature during
	// circular syncs over multiple repos - e.g. repoA ---> repoB ---> repoC ---> repoA (again) - this circle would currently
	// cause https://github.com/cloudstore/cloudstore/issues/25 again (because issue 25 is only solved for direct partners - not indirect).
	// TODO 3: We should switch from UUID to Uid everywhere (most importantly the repositoryId).
	// Careful, though: Uid's String-representation is case-sensitive! Due to Windows, it must thus not be used for file names!
	private String lastSyncFromRepositoryId;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoKey cryptoKey; // might be different than the one of cryptoRepoFile, when it was just changed => keep separately.

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(jdbcType="BLOB")
	private byte[] repoFileDtoData;

	private Date deleted;

//	@Column(defaultValue = "N") // does not work with PostgreSQL!
	private boolean deletedByIgnoreRule;

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

		if (this.cryptoRepoFile != null) // We must allow re-assignment because of collisions on the server and the DuplicateCryptoRepoFileHandler! --- no we don't! we do not re-assign!
			throw new IllegalStateException("this.cryptoRepoFile already assigned! Cannot re-assign!");

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
				final CryptoKeyRole cryptoKeyRole = requireNonNull(cryptoKey.getCryptoKeyRole(), "cryptoKey.cryptoKeyRole");
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

	public boolean isDeletedByIgnoreRule() {
		return deletedByIgnoreRule;
	}
	public void setDeletedByIgnoreRule(boolean deletedByIgnoreRule) {
		if (! equal(this.deletedByIgnoreRule, deletedByIgnoreRule))
			this.deletedByIgnoreRule = deletedByIgnoreRule;
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

	public UUID getLastSyncFromRepositoryId() {
		return lastSyncFromRepositoryId == null ? null : UUID.fromString(lastSyncFromRepositoryId);
	}
	public void setLastSyncFromRepositoryId(final UUID repositoryId) {
		if (! equal(this.getLastSyncFromRepositoryId(), repositoryId))
			this.lastSyncFromRepositoryId = repositoryId == null ? null : repositoryId.toString();
	}

	@Override
	public int getSignedDataVersion() {
		return 1;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code HistoCryptoRepoFile} must exactly match the one in {@link HistoCryptoRepoFileDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;

			final List<InputStreamSource> inputStreamSources = new LinkedList<InputStreamSource>(Arrays.asList(
					InputStreamSource.Helper.createInputStreamSource(getHistoCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(
							previousHistoCryptoRepoFile == null ? null : previousHistoCryptoRepoFile.getHistoCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(requireNonNull(cryptoRepoFile, "cryptoRepoFile").getCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(requireNonNull(histoFrame, "histoFrame").getHistoFrameId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(requireNonNull(cryptoKey, "cryptoKey").getCryptoKeyId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getRepoFileDtoData()),

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

	@Override
	public Signature getSignature() {
		return signature;
	}
	@Override
	public void setSignature(final Signature signature) {
		if (!equal(this.signature, signature))
			this.signature = SignatureImpl.copy(signature);
	}

	@IgnoreDatabaseMigraterComparison
	@Override
	public Uid getCryptoRepoFileIdControllingPermissions() {
		return requireNonNull(requireNonNull(cryptoRepoFile, "cryptoRepoFile").getCryptoRepoFileId(), "cryptoRepoFileId");
	}

	@IgnoreDatabaseMigraterComparison
	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.write;
	}

	@Override
	protected String toString_getProperties() {
		return super.toString_getProperties()
				+ ", histoCryptoRepoFileId=" + histoCryptoRepoFileId
				+ ", cryptoRepoFileId=" + (cryptoRepoFile == null ? null : cryptoRepoFile.getCryptoRepoFileId())
				+ ", deleted=" + deleted;
	}
}
