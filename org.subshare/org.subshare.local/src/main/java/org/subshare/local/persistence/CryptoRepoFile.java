package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.annotations.Column;
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

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.dto.jaxb.RepoFileDtoIo;
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
@Uniques({
	@Unique(name="CryptoRepoFile_cryptoRepoFileId", members="cryptoRepoFileId")
})
@Indices({
	// CryptoRepoFile_repoFile is actually UNIQUE, but only, if repoFile is not null. There can be multiple
	// CryptoRepoFile instances with repoFile == null!
	@Index(name="CryptoRepoFile_repoFile", members="repoFile"),
	@Index(name="CryptoRepoFile_localRevision", members={"localRevision"})
})
@Queries({
	@Query(name="getCryptoRepoFile_repoFile", value="SELECT UNIQUE WHERE this.repoFile == :repoFile"),
	@Query(name="getCryptoRepoFilesWithoutRepoFile", value="SELECT WHERE this.repoFile == null"),
	@Query(name="getCryptoRepoFile_cryptoRepoFileId", value="SELECT UNIQUE WHERE this.cryptoRepoFileId == :cryptoRepoFileId"),
	@Query(
			name="getCryptoRepoFileChangedAfter_localRevision_exclLastSyncFromRepositoryId",
			value="SELECT WHERE this.localRevision > :localRevision && (this.lastSyncFromRepositoryId == null || this.lastSyncFromRepositoryId != :lastSyncFromRepositoryId)") // TODO this necessary == null is IMHO a DN bug!
})
public class CryptoRepoFile extends Entity implements AutoTrackLocalRevision, StoreCallback {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(length=22)
	private String cryptoRepoFileId;

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

//	@Persistent(nullValue=NullValue.EXCEPTION) // is temporarily null
	private CryptoKey cryptoKey;

//	@Persistent(nullValue=NullValue.EXCEPTION) // is temporarily null
	private byte[] repoFileDtoData;

	public CryptoRepoFile() { }

	public CryptoRepoFile(final Uid cryptoRepoFileId) {
		this.cryptoRepoFileId = cryptoRepoFileId == null ? null : cryptoRepoFileId.toString();
	}

	public Uid getCryptoRepoFileId() {
		if (cryptoRepoFileId == null)
			cryptoRepoFileId = new Uid().toString();

		return new Uid(cryptoRepoFileId);
	}

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
	 * @return the corresponding {@link RepoFile}. May be <code>null</code>.
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
	 * the file contents. Might be temporarily <code>null</code> during creation.
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
	 * Before encryption, it is serialised using {@link RepoFileDtoIo} and compressed by a {@link GZIPOutputStream}.
	 * @return the encrypted real meta-data (an instance of a sub-class of {@link RepoFileDto}). Might be temporarily <code>null</code>
	 * during creation.
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

	public UUID getLastSyncFromRepositoryId() {
		return lastSyncFromRepositoryId == null ? null : UUID.fromString(lastSyncFromRepositoryId);
	}
	public void setLastSyncFromRepositoryId(final UUID repositoryId) {
		this.lastSyncFromRepositoryId = repositoryId == null ? null : repositoryId.toString();
	}

	@Override
	public void jdoPreStore() {
		getCryptoRepoFileId();
		assertUniqueRepoFile();
	}

	/**
	 * Asserts that there is only one single {@code CryptoRepoFile} referencing a certain {@link #getRepoFile() RepoFile}.
	 * <p>
	 * A unique constraint does not work, because multiple {@code CryptoRepoFile}s can have a <code>null</code> reference.
	 * Thus we must move this consistency check one layer up - here.
	 */
	private void assertUniqueRepoFile() {
		if (repoFile == null)
			return;

		final PersistenceManager pm = JDOHelper.getPersistenceManager(this);
		assertNotNull("JDOHelper.getPersistenceManager(this)", pm);
		final javax.jdo.Query q = pm.newNamedQuery(CryptoRepoFile.class, "getCryptoRepoFile_repoFile");
		final CryptoRepoFile other = (CryptoRepoFile) q.execute(repoFile);
		if (other != null && !this.equals(other))
			throw new IllegalStateException(String.format(
					"Constraint violation: There is already another CryptoRepoFile (id=%s) referencing the same RepoFile (id=%s)!",
					other.getId(), repoFile.getId()));
	}

	/**
	 * Gets the path within the repository from the root (including) to <code>this</code> (including).
	 * <p>
	 * The first element in the list is the root. The last element is <code>this</code>.
	 * <p>
	 * If this method is called on the root itself, the result will be a list with one single element (the root itself).
	 * @return the path within the repository from the root (including) to <code>this</code> (including). Never <code>null</code>.
	 */
	public List<CryptoRepoFile> getPathList() {
		final LinkedList<CryptoRepoFile> path = new LinkedList<CryptoRepoFile>();
		CryptoRepoFile crf = this;
		while (crf != null) {
			path.addFirst(crf);
			crf = crf.getParent();
		}
		return Collections.unmodifiableList(path);
	}

	/**
	 * Gets the path from the root to <code>this</code>.
	 * <p>
	 * The path's elements are separated by a slash ("/"). The path starts with a slash (like an absolute path), but
	 * is relative to the server(!) repository's local root.
	 * @return the path from the root to <code>this</code>. Never <code>null</code>. The repository's root itself has the path "/".
	 */
	public String getPath() {
		final StringBuilder sb = new StringBuilder();
		for (final CryptoRepoFile crf : getPathList()) {
			if (crf.getParent() == null)
				continue; // the root is just "/" without a name everywhere - on the server, too!

			if (sb.length() == 0 || sb.charAt(sb.length() - 1) != '/')
				sb.append('/');

			sb.append(crf.getCryptoRepoFileId().toString());
		}
		return sb.toString();
	}
}
