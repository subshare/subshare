package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
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
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.io.InputStreamSource;
import org.subshare.core.io.MultiInputStream;
import org.subshare.core.sign.Signature;
import org.subshare.core.sign.WriteProtected;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
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
	@Index(name="CryptoRepoFile_parent", members="parent"),
	@Index(name="CryptoRepoFile_deleted", members="deleted"),
	@Index(name="CryptoRepoFile_localRevision", members={"localRevision"})
})
@Queries({
	@Query(name="getChildCryptoRepoFiles_parent", value="SELECT WHERE this.parent == :parent"),
//	@Query(name="getChildCryptoRepoFile_parent_localName", value="SELECT UNIQUE WHERE this.parent == :parent && this.localName == :localName"),
	@Query(name="getChildCryptoRepoFiles_parent_localName", value="SELECT WHERE this.parent == :parent && this.localName == :localName"),
	@Query(name="getCryptoRepoFile_repoFile", value="SELECT UNIQUE WHERE this.repoFile == :repoFile"),
	@Query(name="getCryptoRepoFilesWithoutRepoFileAndNotDeleted", value="SELECT WHERE this.repoFile == null && this.deleted == null"),
	@Query(name="getCryptoRepoFile_cryptoRepoFileId", value="SELECT UNIQUE WHERE this.cryptoRepoFileId == :cryptoRepoFileId"),
	@Query(
			name="getCryptoRepoFilesChangedAfter_localRevision_exclLastSyncFromRepositoryId",
			value="SELECT WHERE this.localRevision > :localRevision && (this.lastSyncFromRepositoryId == null || this.lastSyncFromRepositoryId != :lastSyncFromRepositoryId)") // TODO this necessary == null is IMHO a DN bug!
})
@FetchGroups({
	@FetchGroup(name = FetchGroupConst.CRYPTO_REPO_FILE_DTO, members = {
			@Persistent(name = "parent"),
			@Persistent(name = "cryptoKey"),
			@Persistent(name = "repoFileDtoData"),
			@Persistent(name = "signature")
	}),
	@FetchGroup(name = FetchGroupConst.SIGNATURE, members = {
			@Persistent(name = "signature")
	}),
	@FetchGroup(name = FetchGroupConst.CRYPTO_REPO_FILE_PARENT_AND_REPO_FILE, members = {
			@Persistent(name = "parent"),
			@Persistent(name = "repoFile")
	})
})
public class CryptoRepoFile extends Entity implements WriteProtected, AutoTrackLocalRevision, StoreCallback {

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
	@Column(jdbcType="BLOB")
	private byte[] repoFileDtoData;

//	@Column(jdbcType="CLOB")
	private String localName;

	private boolean directory;

	private Date cryptoRepoFileCreated = new Date();

	private Date deleted;

	@Column(defaultValue = "N")
	private boolean deletedByIgnoreRule;

	@Persistent(nullValue = NullValue.EXCEPTION)
	@Embedded(nullIndicatorColumn = "signatureCreated")
	private SignatureImpl signature;

//	@Persistent(mappedBy = "cryptoRepoFile")
//	private HistoCryptoRepoFile cryptoRepoFileOnServer;

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
		if (! equal(this.parent, parent))
			this.parent = parent;
	}

	/**
	 * Gets the local, plain-text name (as opposed to the encrypted name on the server).
	 * <p>
	 * This property is always <code>null</code> on the server. On the client, it may be <code>null</code>, too,
	 * which indicates that the user does not have the necessary access rights to decrypt it.
	 * <p>
	 * If it is non-<code>null</code>, it usually equals the associated {@link #getRepoFile() RepoFile}'s
	 * {@link RepoFile#getName() name}. There might be differences, though, if the file was renamed and
	 * not yet all objects were updated in the database.
	 * @return the local, plain-text name or <code>null</code>.
	 */
	public String getLocalName() {
		return localName;
	}
	public void setLocalName(final String localName) {
		if (! equal(this.localName, localName))
			this.localName = localName;
	}

	/**
	 * Gets the corresponding {@link RepoFile}.
	 * <p>
	 * If this is the client side, the file referenced here is plain-text.
	 * <p>
	 * If this is the server side, the file referenced here is encrypted and has no useful meta-data, anymore:
	 * <ul>
	 * <li>Its {@link RepoFile#getName() name} is a random name.
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
		if (! equal(this.repoFile, repoFile))
			this.repoFile = repoFile;
	}

	/**
	 * Gets the timestamp when this {@code CryptoRepoFile} was globally created.
	 * <p>
	 * This is different from {@link #getCreated() created} which returns the timestamp of the creation in the local
	 * database. In contrast to {@code created}, {@code cryptoRepoFileCreated} is synchronized across all repositories
	 * (i.e. all databases).
	 * @return the timestamp when this was globally created. Should never be <code>null</code> for new
	 * entities, but may be <code>null</code> for old entities, because this property was newly introduced.
	 */
	public Date getCryptoRepoFileCreated() {
		return cryptoRepoFileCreated;
	}
	public void setCryptoRepoFileCreated(final Date cryptoRepoFileCreated) {
		if (! equal(this.cryptoRepoFileCreated, cryptoRepoFileCreated))
			this.cryptoRepoFileCreated = cryptoRepoFileCreated;
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

	@Override
	public long getLocalRevision() {
		return localRevision;
	}
	@Override
	public void setLocalRevision(final long localRevision) {
		if (! equal(this.localRevision, localRevision))
			this.localRevision = localRevision;
	}

	public UUID getLastSyncFromRepositoryId() {
		return lastSyncFromRepositoryId == null ? null : UUID.fromString(lastSyncFromRepositoryId);
	}
	public void setLastSyncFromRepositoryId(final UUID repositoryId) {
		if (! equal(this.getLastSyncFromRepositoryId(), repositoryId))
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
		requireNonNull(pm, "JDOHelper.getPersistenceManager(this)");
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
	 * Gets the path from the root to <code>this</code> as visible on the server.
	 * <p>
	 * In contrast to the local path, containing the plain-text-names of the files, this path contains random
	 * names as the real names are only in the encrypted meta-data.
	 * <p>
	 * The path's elements are separated by a slash ("/"). The path starts with a slash (like an absolute path), but
	 * is relative to the server(!) repository's local root.
	 * @return the path from the root to <code>this</code>. Never <code>null</code>. The repository's root itself has the path "/".
	 */
	public String getServerPath() {
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

	public String getLocalPathOrFail() {
		final StringBuilder sb = new StringBuilder();
		for (final CryptoRepoFile crf : getPathList()) {
			if (crf.getParent() == null)
				continue; // the root is just "/" without a name everywhere - locally, too!

			if (sb.length() == 0 || sb.charAt(sb.length() - 1) != '/')
				sb.append('/');

			sb.append(requireNonNull(crf.getLocalName(), "cryptoRepoFile.localName")); // if this is null, we are not on the client-side (or we have no access at all). if we are on the client and do have access, we should be able to decrypt all parents due to the backlink-keys!
		}
		return sb.toString();
	}

	public boolean isDirectory() {
		return directory;
	}
	public void setDirectory(final boolean directory) {
		if (! equal(this.directory, directory))
			this.directory = directory;
	}

	@Override
	public String getSignedDataType() {
		return CryptoRepoFileDto.SIGNED_DATA_TYPE;
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
	public int getSignedDataVersion() {
		return 2;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>Important:</b> The implementation in {@code CryptoRepoFile} must exactly match the one in {@link CryptoRepoFileDto}!
	 */
	@Override
	public InputStream getSignedData(final int signedDataVersion) {
		try {
			byte separatorIndex = 0;

			final List<InputStreamSource> inputStreamSources = new LinkedList<InputStreamSource>(Arrays.asList(
					InputStreamSource.Helper.createInputStreamSource(getCryptoRepoFileId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(parent == null ? null : parent.getCryptoRepoFileId()),
//			getRepoFile();
//			getLocalRevision();
//			getLastSyncFromRepositoryId(),
					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(cryptoKey == null ? null : cryptoKey.getCryptoKeyId()),

					InputStreamSource.Helper.createInputStreamSource(++separatorIndex),
					InputStreamSource.Helper.createInputStreamSource(getRepoFileDtoData()),
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

	@Override
	public Signature getSignature() {
		return signature;
	}
	@Override
	public void setSignature(final Signature signature) {
		if (! equal(this.signature, signature))
			this.signature = SignatureImpl.copy(signature);
	}

	@Override
	public Uid getCryptoRepoFileIdControllingPermissions() {
		return requireNonNull(this.getCryptoRepoFileId(), "cryptoRepoFileId");
	}

	@Override
	public PermissionType getPermissionTypeRequiredForWrite() {
		return PermissionType.write;
	}

	@Override
	protected String toString_getProperties() {
		return super.toString_getProperties()
				+ ", cryptoRepoFileId=" + cryptoRepoFileId
				+ ", localName=" + localName
				+ ", deleted=" + deleted;
	}
}
