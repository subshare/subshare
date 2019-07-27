package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Uniques({
	@Unique(name="UK_CurrentHistoCryptoRepoFile_cryptoRepoFile", members="cryptoRepoFile")
})
@Indices({
	@Index(name="CurrentHistoCryptoRepoFile_localRevision", members="localRevision"),
	@Index(name="CurrentHistoCryptoRepoFile_cryptoRepoFile", members="cryptoRepoFile")
})
@Queries({
	@Query(name="getCurrentHistoCryptoRepoFile_cryptoRepoFile", value="SELECT UNIQUE WHERE this.cryptoRepoFile == :cryptoRepoFile"),
	@Query(
			name="getCurrentHistoCryptoRepoFilesChangedAfter_localRevision_exclLastSyncFromRepositoryId",
			value="SELECT WHERE this.localRevision > :localRevision && (this.lastSyncFromRepositoryId == null || this.lastSyncFromRepositoryId != :lastSyncFromRepositoryId)") // TODO this necessary == null is IMHO a DN bug!
})
@FetchGroups({
	@FetchGroup(name = FetchGroupConst.CURRENT_HISTO_CRYPTO_REPO_FILE_DTO, members = {
			@Persistent(name = "histoCryptoRepoFile"),
			@Persistent(name = "cryptoRepoFile"),
			@Persistent(name = "signature")
	}),
	@FetchGroup(name = FetchGroupConst.SIGNATURE, members = {
			@Persistent(name = "signature")
	})
})
public class CurrentHistoCryptoRepoFile extends Entity implements WriteProtected, AutoTrackLocalRevision, StoreCallback {

	private static final Logger logger = LoggerFactory.getLogger(CurrentHistoCryptoRepoFile.class);

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoRepoFile cryptoRepoFile;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private HistoCryptoRepoFile histoCryptoRepoFile;

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
	@Embedded(nullIndicatorColumn="signatureCreated")
	private SignatureImpl signature;

	public CurrentHistoCryptoRepoFile() {
	}

	public CryptoRepoFile getCryptoRepoFile() {
		return cryptoRepoFile;
	}
	public void setCryptoRepoFile(CryptoRepoFile cryptoRepoFile) {
		if (equal(this.cryptoRepoFile, cryptoRepoFile))
			return;

		if (this.cryptoRepoFile != null)
			throw new IllegalStateException("this.cryptoRepoFile already assigned! Cannot re-assign!");

		this.cryptoRepoFile = cryptoRepoFile;
	}

	public HistoCryptoRepoFile getHistoCryptoRepoFile() {
		return histoCryptoRepoFile;
	}
	public void setHistoCryptoRepoFile(HistoCryptoRepoFile histoCryptoRepoFile) {
		this.histoCryptoRepoFile = histoCryptoRepoFile;
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
		logger.debug("jdoPreStore: {} {}",
				cryptoRepoFile, histoCryptoRepoFile);

		final Uid cryptoRepoFileId = requireNonNull(cryptoRepoFile, "cryptoRepoFile").getCryptoRepoFileId();
		final CryptoRepoFile crf = requireNonNull(histoCryptoRepoFile, "histoCryptoRepoFile").getCryptoRepoFile();
		requireNonNull(crf, "histoCryptoRepoFile.cryptoRepoFile");

		if (! cryptoRepoFileId.equals(crf.getCryptoRepoFileId()))
			throw new IllegalStateException(String.format("cryptoRepoFile.cryptoRepoFileId != histoCryptoRepoFile.cryptoRepoFile.cryptoRepoFileId :: %s != %s",
					cryptoRepoFileId, crf.getCryptoRepoFileId()));
	}

	@Override
	public String getSignedDataType() {
		return CurrentHistoCryptoRepoFileDto.SIGNED_DATA_TYPE;
	}

	@Override
	public int getSignedDataVersion() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code CurrentHistoCryptoRepoFile} must exactly match the one in {@code CurrentHistoCryptoRepoFileDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			final Uid cryptoRepoFileId = requireNonNull(cryptoRepoFile, "cryptoRepoFile").getCryptoRepoFileId();
			final Uid histoCryptoRepoFileId = requireNonNull(histoCryptoRepoFile, "histoCryptoRepoFile").getHistoCryptoRepoFileId();

			requireNonNull(cryptoRepoFileId, "cryptoRepoFileId");
			requireNonNull(histoCryptoRepoFileId, "histoCryptoRepoFileId");

			byte separatorIndex = 0;
			return new MultiInputStream(
					InputStreamSource.Helper.createInputStreamSource(cryptoRepoFileId),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(histoCryptoRepoFileId)
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

	public UUID getLastSyncFromRepositoryId() {
		return lastSyncFromRepositoryId == null ? null : UUID.fromString(lastSyncFromRepositoryId);
	}
	public void setLastSyncFromRepositoryId(final UUID repositoryId) {
		if (! equal(this.getLastSyncFromRepositoryId(), repositoryId))
			this.lastSyncFromRepositoryId = repositoryId == null ? null : repositoryId.toString();
	}

	@Override
	public Uid getCryptoRepoFileIdControllingPermissions() {
		return requireNonNull(requireNonNull(cryptoRepoFile, "cryptoRepoFile").getCryptoRepoFileId(), "cryptoRepoFileId");
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.write;
	}
}
