package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.local.persistence.Dao;

public class HistoFileChunkDao extends Dao<HistoFileChunk, HistoFileChunkDao> {

	private static final Logger logger = LoggerFactory.getLogger(HistoFileChunkDao.class);

	public Collection<HistoFileChunk> getHistoFileChunks(final FileChunkPayload fileChunkPayload) {
		assertNotNull(fileChunkPayload, "fileChunkPayload");
		final Query query = pm().newNamedQuery(getEntityClass(), "getHistoFileChunks_fileChunkPayload");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<HistoFileChunk> result = (Collection<HistoFileChunk>) query.execute(fileChunkPayload);
			logger.debug("getHistoFileChunks: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getHistoFileChunks: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<HistoFileChunk> getHistoFileChunks(final HistoCryptoRepoFile histoCryptoRepoFile) {
		assertNotNull(histoCryptoRepoFile, "histoCryptoRepoFile");
		final Query query = pm().newNamedQuery(getEntityClass(), "getHistoFileChunks_histoCryptoRepoFile");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<HistoFileChunk> result = (Collection<HistoFileChunk>) query.execute(histoCryptoRepoFile);
			logger.debug("getHistoFileChunks2: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getHistoFileChunks2: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public long getHistoFileChunkCount(final FileChunkPayload fileChunkPayload) {
		assertNotNull(fileChunkPayload, "fileChunkPayload");
		final Query query = pm().newNamedQuery(getEntityClass(), "getHistoFileChunkCount_fileChunkPayload");
		try {
			long startTimestamp = System.currentTimeMillis();
			Long result = (Long) query.execute(fileChunkPayload);
			logger.debug("getHistoFileChunkCount: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);
			return result;
		} finally {
			query.closeAll();
		}
	}
}
