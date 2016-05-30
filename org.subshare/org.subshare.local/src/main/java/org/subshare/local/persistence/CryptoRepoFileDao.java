package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class CryptoRepoFileDao extends Dao<CryptoRepoFile, CryptoRepoFileDao> {

	private static final Logger logger = LoggerFactory.getLogger(CryptoRepoFileDao.class);

	public CryptoRepoFile getCryptoRepoFileOrFail(final Uid cryptoRepoFileId) {
		final CryptoRepoFile result = getCryptoRepoFile(cryptoRepoFileId);
		if (result == null)
			throw new IllegalArgumentException("There is no CryptoRepoFile for this cryptoRepoFileId: " + cryptoRepoFileId);

		return result;
	}

	public CryptoRepoFile getCryptoRepoFile(final Uid cryptoRepoFileId) {
		assertNotNull("cryptoRepoFileId", cryptoRepoFileId);
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoRepoFile_cryptoRepoFileId");
		try {
			final CryptoRepoFile cryptoRepoFile = (CryptoRepoFile) query.execute(cryptoRepoFileId.toString());
			return cryptoRepoFile;
		} finally {
			query.closeAll(); // probably not needed for a UNIQUE query, but it shouldn't harm ;-)
		}
	}

	public CryptoRepoFile getCryptoRepoFileOrFail(final RepoFile repoFile) {
		final CryptoRepoFile result = getCryptoRepoFile(repoFile);
		if (result == null)
			throw new IllegalArgumentException("There is no CryptoRepoFile for this RepoFile: " + repoFile);

		return result;
	}

	public CryptoRepoFile getCryptoRepoFile(final RepoFile repoFile) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoRepoFile_repoFile");
		try {
			final CryptoRepoFile cryptoRepoFile = (CryptoRepoFile) query.execute(repoFile);
			return cryptoRepoFile;
		} finally {
			query.closeAll(); // probably not needed for a UNIQUE query, but it shouldn't harm ;-)
		}
	}

	public Collection<CryptoRepoFile> getCryptoRepoFilesWithoutRepoFileAndNotDeleted() {
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoRepoFilesWithoutRepoFileAndNotDeleted");
		try {
			long startTimestamp = System.currentTimeMillis();

			@SuppressWarnings("unchecked")
			Collection<CryptoRepoFile> cryptoRepoFiles = (Collection<CryptoRepoFile>) query.execute();
			logger.debug("getCryptoRepoFilesWithoutRepoFile: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			cryptoRepoFiles = load(cryptoRepoFiles);
			logger.debug("getCryptoRepoFilesWithoutRepoFile: Loading result-set with {} elements took {} ms.", cryptoRepoFiles.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoRepoFiles;
		} finally {
			query.closeAll();
		}
	}

	public Collection<CryptoRepoFile> getCryptoRepoFilesChangedAfterExclLastSyncFromRepositoryId(
			final long localRevision, final UUID exclLastSyncFromRepositoryId) {

		assertNotNull("exclLastSyncFromRepositoryId", exclLastSyncFromRepositoryId);
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoRepoFileChangedAfter_localRevision_exclLastSyncFromRepositoryId");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoRepoFile> cryptoRepoFiles = (Collection<CryptoRepoFile>) query.execute(localRevision, exclLastSyncFromRepositoryId.toString());
			logger.debug("getCryptoRepoFileChangedAfterExclLastSyncFromRepositoryId: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			cryptoRepoFiles = load(cryptoRepoFiles);
			logger.debug("getCryptoRepoFileChangedAfterExclLastSyncFromRepositoryId: Loading result-set with {} elements took {} ms.", cryptoRepoFiles.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoRepoFiles;
		} finally {
			query.closeAll();
		}
	}

	public Collection<CryptoRepoFile> getChildCryptoRepoFiles(final CryptoRepoFile parent) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getChildCryptoRepoFiles_parent");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoRepoFile> cryptoRepoFiles = (Collection<CryptoRepoFile>) query.execute(parent);
			logger.debug("getChildCryptoRepoFiles: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			cryptoRepoFiles = load(cryptoRepoFiles);
			logger.debug("getChildCryptoRepoFiles: Loading result-set with {} elements took {} ms.", cryptoRepoFiles.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoRepoFiles;
		} finally {
			query.closeAll();
		}
	}

	public CryptoRepoFile getChildCryptoRepoFile(final CryptoRepoFile parent, final String localName) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getChildCryptoRepoFile_parent_localName");
		try {
			final CryptoRepoFile cryptoRepoFile = (CryptoRepoFile) query.execute(parent, localName);
			return cryptoRepoFile;
		} finally {
			query.closeAll();
		}
	}

	public CryptoRepoFile getRootCryptoRepoFile() {
		final Collection<CryptoRepoFile> childCryptoRepoFiles = getChildCryptoRepoFiles(null);
		final Iterator<CryptoRepoFile> iterator = childCryptoRepoFiles.iterator();

		if (!iterator.hasNext())
			return null;

		final CryptoRepoFile rootCryptoRepoFile = iterator.next();

		if (iterator.hasNext())
			throw new IllegalStateException("There are multiple root-CryptoRepoFiles!");

		return rootCryptoRepoFile;
	}

	public CryptoRepoFile getCryptoRepoFile(final RemoteRepository remoteRepository, final String localPath) {
		assertNotNull("remoteRepository", remoteRepository);
		assertNotNull("localPath", localPath);
		final String localPathPrefix = remoteRepository.getLocalPathPrefix();
		if ("/".equals(localPathPrefix))
			throw new IllegalStateException("This should never be slash! For the root, it should be empty!");

		final StringBuilder prefixedLocalPath = new StringBuilder();
		prefixedLocalPath.append(localPathPrefix);
		if (prefixedLocalPath.length() > 0
				&& prefixedLocalPath.charAt(prefixedLocalPath.length() - 1) != '/'
				&& localPath.length() > 0
				&& localPath.charAt(0) != '/')
			prefixedLocalPath.append('/');

		prefixedLocalPath.append(localPath);

		return getCryptoRepoFile(prefixedLocalPath.toString());
	}

	/**
	 * Gets the {@link CryptoRepoFile} corresponding to the given {@code localPath}.
	 * <p>
	 * <b>Important:</b> This {@code localPath} is global and independent from the current working copy's
	 * check-out-root. This is, because {@code CryptoRepoFile}s are global.
	 * @param localPath local path, independent from where the current working-copy is checked out. Thus it's always
	 * relative to the <code>server</code>'s repository-root. Must not be <code>null</code>.
	 * @return the {@link CryptoRepoFile} corresponding to the given {@code localPath}. May be <code>null</code>, if
	 * there is no such {@link CryptoRepoFile}.
	 */
	public CryptoRepoFile getCryptoRepoFile(final String localPath) {
		return _getCryptoRepoFile(assertNotNull("localPath", localPath), localPath);
	}

	private CryptoRepoFile _getCryptoRepoFile(final String localPath, final String originallySearchedLocalPath) {
		if ("/".equals(localPath) || localPath.isEmpty())
			return getRootCryptoRepoFile();

		final String parentLocalPath = getParentPath(localPath);
		if (parentLocalPath == null)
			throw new IllegalArgumentException(String.format("Repository does not contain CryptoRepoFile for local path '%s'!", originallySearchedLocalPath));

		final CryptoRepoFile parentRepoFile = _getCryptoRepoFile(parentLocalPath, originallySearchedLocalPath);
		final CryptoRepoFile result = getChildCryptoRepoFile(parentRepoFile, getName(localPath));
		return result;
	}

	private String getName(final String path) {
		if ("/".equals(path) || path.isEmpty())
			return "";

		final int lastSlashIndex = assertNotNull("path", path).lastIndexOf('/');
		return path.substring(lastSlashIndex + 1);
	}

	private String getParentPath(final String path) {
		if ("/".equals(path) || path.isEmpty())
			return null;

		final int lastSlashIndex = assertNotNull("path", path).lastIndexOf('/');
		final String parentPath = path.substring(0, lastSlashIndex);
		if (parentPath.isEmpty())
			return "/";
		else
			return parentPath;
	}

	@Override
	public void deletePersistent(CryptoRepoFile entity) {
		nullCryptoRepoKey(entity);
		deleteCurrentHistoCryptoRepoFile(entity);
		pm().flush();

		deleteHistoCryptoRepoFiles(entity);
		pm().flush();

		deleteCryptoKeys(entity);
		pm().flush();
		super.deletePersistent(entity);
	}

	@Override
	public void deletePersistentAll(final Collection<? extends CryptoRepoFile> entities) {
		for (CryptoRepoFile cryptoRepoFile : entities) {
			nullCryptoRepoKey(cryptoRepoFile);
			deleteCurrentHistoCryptoRepoFile(cryptoRepoFile);
		}
		pm().flush();

		for (CryptoRepoFile cryptoRepoFile : entities)
			deleteHistoCryptoRepoFiles(cryptoRepoFile);

		pm().flush();

		for (CryptoRepoFile cryptoRepoFile : entities)
			deleteCryptoKeys(cryptoRepoFile);

		pm().flush();
		super.deletePersistentAll(entities);
	}

	protected void nullCryptoRepoKey(final CryptoRepoFile cryptoRepoFile) {
		cryptoRepoFile.setCryptoKey(null); // must null this first! otherwise there's a circular dependency
	}

	protected void deleteCurrentHistoCryptoRepoFile(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull("cryptoRepoFile", cryptoRepoFile);
		final CurrentHistoCryptoRepoFileDao chcrfDao = getDao(CurrentHistoCryptoRepoFileDao.class);
		final CurrentHistoCryptoRepoFile chcrf = chcrfDao.getCurrentHistoCryptoRepoFile(cryptoRepoFile);
		if (chcrf != null)
			chcrfDao.deletePersistent(chcrf);
	}

	protected void deleteHistoCryptoRepoFiles(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull("cryptoRepoFile", cryptoRepoFile);
		final HistoCryptoRepoFileDao hcrfDao = getDao(HistoCryptoRepoFileDao.class);
		final Collection<HistoCryptoRepoFile> histoCryptoRepoFiles = hcrfDao.getHistoCryptoRepoFiles(cryptoRepoFile);
		hcrfDao.deletePersistentAll(histoCryptoRepoFiles);
	}

	protected void deleteCryptoKeys(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull("cryptoRepoFile", cryptoRepoFile);
		final CryptoKeyDao cryptoKeyDao = getDao(CryptoKeyDao.class);
		cryptoKeyDao.deletePersistentAll(cryptoKeyDao.getCryptoKeys(cryptoRepoFile));
	}

	public Collection<CryptoRepoFile> getDeletedCryptoRepoFilesWithoutDeletedHistoCryptoRepoFiles() {
		final Query query = pm().newQuery("SELECT FROM " + CryptoRepoFile.class.getName()
				+ " WHERE this.deleted != null"
				+ " && (SELECT count(h) FROM " + HistoCryptoRepoFile.class.getName() + " h WHERE h.cryptoRepoFile == this && h.deleted != null) == 0");
		try {
			long startTimestamp = System.currentTimeMillis();

			@SuppressWarnings("unchecked")
			Collection<CryptoRepoFile> cryptoRepoFiles = (Collection<CryptoRepoFile>) query.execute();
			logger.debug("getDeletedCryptoRepoFilesWithoutDeletedHistoCryptoRepoFiles: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			cryptoRepoFiles = load(cryptoRepoFiles);
			logger.debug("getDeletedCryptoRepoFilesWithoutDeletedHistoCryptoRepoFiles: Loading result-set with {} elements took {} ms.", cryptoRepoFiles.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoRepoFiles;
		} finally {
			query.closeAll();
		}
	}

	public Collection<CryptoRepoFile> getCryptoRepoFilesWithRepoFileAndDeleted() {
		final Query query = pm().newQuery("SELECT FROM " + CryptoRepoFile.class.getName()
				+ " WHERE this.repoFile != null && this.deleted != null");
		try {
			long startTimestamp = System.currentTimeMillis();

			@SuppressWarnings("unchecked")
			Collection<CryptoRepoFile> cryptoRepoFiles = (Collection<CryptoRepoFile>) query.execute();
			logger.debug("getCryptoRepoFilesWithRepoFileAndDeleted: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			cryptoRepoFiles = load(cryptoRepoFiles);
			logger.debug("getCryptoRepoFilesWithRepoFileAndDeleted: Loading result-set with {} elements took {} ms.", cryptoRepoFiles.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoRepoFiles;
		} finally {
			query.closeAll();
		}
	}
}
