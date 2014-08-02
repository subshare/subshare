package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Collection;
import java.util.UUID;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;
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

	public Collection<CryptoRepoFile> getCryptoRepoFilesWithoutRepoFile() {
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoRepoFilesWithoutRepoFile");
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

}
