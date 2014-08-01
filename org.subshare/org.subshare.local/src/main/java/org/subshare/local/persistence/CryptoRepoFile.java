package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

import org.subshare.core.dto.CryptoKeyRole;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;
import co.codewizards.cloudstore.local.persistence.RepoFile;

/**
 * Container holding the encrypted meta-data for a file (or directory).
 * <p>
 * There is a 1-1-relation to a {@link RepoFile} (via the {@link #getRepoFile() repoFile} property).
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.NEW_TABLE)
@Unique(name="CryptoRepoFile_repoFile", members="repoFile")
@Index(name="CryptoRepoFile_localRevision", members={"localRevision"})
@Queries({
	@Query(name="getCryptoRepoFile_repoFile", value="SELECT UNIQUE WHERE this.repoFile == :repoFile"),
	@Query(
			name="getCryptoRepoFileChangedAfter_localRevision_exclLastSyncFromRepositoryId",
			value="SELECT WHERE this.localRevision > :localRevision && (this.lastSyncFromRepositoryId == null || this.lastSyncFromRepositoryId != :lastSyncFromRepositoryId)") // TODO this necessary == null is IMHO a DN bug!
})
public class CryptoRepoFile extends Entity implements AutoTrackLocalRevision {

	private CryptoRepoFile parent;

	private RepoFile repoFile;

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
	private CryptoKey cryptoKey;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] repoFileDtoData;

	public CryptoRepoFile getParent() {
		return parent;
	}
	public void setParent(final CryptoRepoFile parent) {
		this.parent = parent;
	}

	/**
	 * Gets the corresponding {@link RepoFile}.
	 * <p>
	 * If this is the client side, the file referenced here is plain-text.
	 * <p>
	 * If this is the server side, the file referenced here is encrypted and has no useful meta-data, anymore:
	 * <ul>
	 * <li>Its {@link RepoFile#getName() name} is a hash code (unique per server-repository and real name).
	 * <li>Its {@link RepoFile#getLastModified() lastModified} is always 0 (1970-01-01 00:00:00 UTC).
	 * </ul>
	 * <p>
	 * The real meta-data is encoded as Dto and then encrypted on the client-side.
	 * <p>
	 * Please note, that this referenced {@code RepoFile} might be <code>null</code>, if it does not exist
	 * locally. This is e.g. possible, when checking out a sub-tree only. In this case, the client still fetches
	 * all the {@code CryptoRepoFile}s up to the root (=> backlinks) in order to know the plain-text path,
	 * but there are no corresponding directories in the client's repository.
	 * @return the {@link RepoFile} on the SubShare-server-side. Never <code>null</code> in persistent data
	 * (but maybe <code>null</code> temporarily in memory).
	 */
	public RepoFile getRepoFile() {
		return repoFile;
	}
	public void setRepoFile(final RepoFile repoFile) {
		this.repoFile = repoFile;
	}

	/**
	 * Gets the key used to encrypt {@link #getRepoFileDtoData() repoFileDtoData} as well as the
	 * actual content of the file (if it is a normal file - no directory).
	 * @return the key used to encrypt {@link #getRepoFileDtoData() repoFileDtoData} and - if applicable -
	 * the file contents.
	 */
	public CryptoKey getCryptoKey() {
		return cryptoKey;
	}
	public void setCryptoKey(final CryptoKey cryptoKey) {
		if (cryptoKey != null) {
			final CryptoKeyRole cryptoKeyRole = assertNotNull("cryptoKey.cryptoKeyRole", cryptoKey.getCryptoKeyRole());
			if (CryptoKeyRole.dataKey != cryptoKeyRole)
				throw new IllegalArgumentException("cryptoKey.cryptoKeyRole != dataKey");
		}
		this.cryptoKey = cryptoKey;
	}

	/**
	 * Gets the encrypted real meta-data (an instance of a sub-class of {@link RepoFileDto}).
	 * <p>
	 * This meta-data is encrypted on the client-side using the referenced {@link #getCryptoKey() cryptoKey}.
	 * @return the encrypted real meta-data (an instance of a sub-class of {@link RepoFileDto}). Never <code>null</code> in persistent data
	 * (but maybe <code>null</code> temporarily in memory).
	 */
	public byte[] getRepoFileDtoData() {
		return repoFileDtoData;
	}
	public void setRepoFileDtoData(final byte[] repoFileDtoData) {
		this.repoFileDtoData = repoFileDtoData;
	}

	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(final long localRevision) {
		this.localRevision = localRevision;
	}

	public String getLastSyncFromRepositoryId() {
		return lastSyncFromRepositoryId;
	}
	public void setLastSyncFromRepositoryId(final String lastSyncFromRepositoryId) {
		this.lastSyncFromRepositoryId = lastSyncFromRepositoryId;
	}

}
