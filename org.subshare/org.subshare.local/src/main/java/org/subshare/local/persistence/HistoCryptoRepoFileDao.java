package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;

public class HistoCryptoRepoFileDao extends Dao<HistoCryptoRepoFile, HistoCryptoRepoFileDao> {

	private static final Logger logger = LoggerFactory.getLogger(HistoCryptoRepoFileDao.class);

	public Collection<HistoCryptoRepoFile> getHistoCryptoRepoFilesChangedAfter(final long localRevision) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getHistoCryptoRepoFilesChangedAfter_localRevision");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<HistoCryptoRepoFile> result = (Collection<HistoCryptoRepoFile>) query.execute(localRevision);
			logger.debug("getHistoCryptoRepoFilesChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getHistoCryptoRepoFilesChangedAfter: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<HistoCryptoRepoFile> getHistoCryptoRepoFiles(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull("cryptoRepoFile", cryptoRepoFile);
		final Query query = pm().newNamedQuery(getEntityClass(), "getHistoCryptoRepoFiles_cryptoRepoFile");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<HistoCryptoRepoFile> result = (Collection<HistoCryptoRepoFile>) query.execute(cryptoRepoFile);
			logger.debug("getHistoCryptoRepoFiles: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getHistoCryptoRepoFiles: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<HistoCryptoRepoFile> getHistoCryptoRepoFiles(HistoFrame histoFrame) {
		assertNotNull("histoFrame", histoFrame);

		final Query query = pm().newNamedQuery(getEntityClass(), "getHistoCryptoRepoFiles_histoFrame");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<HistoCryptoRepoFile> result = (Collection<HistoCryptoRepoFile>) query.execute(histoFrame);
			logger.debug("getHistoCryptoRepoFiles: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getHistoCryptoRepoFiles: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public HistoCryptoRepoFile getHistoCryptoRepoFileOrFail(final Uid histoCryptoRepoFileId) {
		final HistoCryptoRepoFile histoCryptoRepoFile = getHistoCryptoRepoFile(histoCryptoRepoFileId);
		assertNotNull("getHistoCryptoRepoFile(" + histoCryptoRepoFileId + ")", histoCryptoRepoFile);
		return histoCryptoRepoFile;
	}

	public HistoCryptoRepoFile getHistoCryptoRepoFile(final Uid histoCryptoRepoFileId) {
		assertNotNull("histoCryptoRepoFileId", histoCryptoRepoFileId);
		final Query query = pm().newNamedQuery(getEntityClass(), "getHistoCryptoRepoFile_histoCryptoRepoFileId");
		try {
			final HistoCryptoRepoFile result = (HistoCryptoRepoFile) query.execute(histoCryptoRepoFileId.toString());
			return result;
		} finally {
			query.closeAll();
		}
	}

	@Override
	public void deletePersistent(final HistoCryptoRepoFile entity) {
		deletePlainHistoCryptoRepoFile(entity);
		deleteHistoFileChunks(entity);
		getPersistenceManager().flush();
		super.deletePersistent(entity);
	}

	@Override
	public void deletePersistentAll(final Collection<? extends HistoCryptoRepoFile> entities) {
		for (HistoCryptoRepoFile entity : entities) {
			deletePlainHistoCryptoRepoFile(entity);
			deleteHistoFileChunks(entity);
		}
		getPersistenceManager().flush();
		super.deletePersistentAll(entities);
	}

	private void deleteHistoFileChunks(final HistoCryptoRepoFile histoCryptoRepoFile) {
		final HistoFileChunkDao hfcDao = getDao(HistoFileChunkDao.class);
		hfcDao.deletePersistentAll(hfcDao.getHistoFileChunks(histoCryptoRepoFile));
	}

	private void deletePlainHistoCryptoRepoFile(final HistoCryptoRepoFile histoCryptoRepoFile) {
		final PlainHistoCryptoRepoFileDao phcrfDao = getDao(PlainHistoCryptoRepoFileDao.class);
		final PlainHistoCryptoRepoFile phcrf = phcrfDao.getPlainHistoCryptoRepoFile(histoCryptoRepoFile);
		if (phcrf != null)
			phcrfDao.deletePersistent(phcrf);
	}

	public Collection<HistoCryptoRepoFile> getHistoCryptoRepoFilesWithoutPlainHistoCryptoRepoFile() {
		final Query query = pm().newNamedQuery(getEntityClass(), "getHistoCryptoRepoFilesWithoutPlainHistoCryptoRepoFile");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<HistoCryptoRepoFile> result = (Collection<HistoCryptoRepoFile>) query.execute();
			logger.debug("getHistoCryptoRepoFilesWithoutPlainHistoCryptoRepoFile: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getHistoCryptoRepoFilesWithoutPlainHistoCryptoRepoFile: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}
}
