package org.subshare.local.persistence;

import static java.util.Objects.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.NormalFile;

public class TempFileChunkDao extends Dao<TempFileChunk, TempFileChunkDao> {

	private static final Logger logger = LoggerFactory.getLogger(TempFileChunkDao.class);

	public TempFileChunk getTempFileChunk(NormalFile normalFile, UUID remoteRepositoryId, long offset) {
		requireNonNull(normalFile, "repoFile");
		requireNonNull(remoteRepositoryId, "remoteRepositoryId");

		final Query query = pm().newNamedQuery(getEntityClass(), "getTempFileChunk_normalFile_remoteRepositoryId_offset");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(3);
			params.put("normalFile", normalFile);
			params.put("remoteRepositoryId", remoteRepositoryId.toString());
			params.put("offset", offset);

			long startTimestamp = System.currentTimeMillis();
			final TempFileChunk result = (TempFileChunk) query.executeWithMap(params);
			logger.debug("getFileChunk: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<TempFileChunk> getTempFileChunks(NormalFile normalFile) {
		requireNonNull(normalFile, "repoFile");

		final Query query = pm().newNamedQuery(getEntityClass(), "getTempFileChunks_normalFile");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(1);
			params.put("normalFile", normalFile);

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<TempFileChunk> result = (Collection<TempFileChunk>) query.executeWithMap(params);
			logger.debug("getTempFileChunks: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getTempFileChunks: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<TempFileChunk> getTempFileChunks(NormalFile normalFile, UUID remoteRepositoryId) {
		requireNonNull(normalFile, "repoFile");
		requireNonNull(remoteRepositoryId, "remoteRepositoryId");

		final Query query = pm().newNamedQuery(getEntityClass(), "getTempFileChunks_normalFile_remoteRepositoryId");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(2);
			params.put("normalFile", normalFile);
			params.put("remoteRepositoryId", remoteRepositoryId.toString());

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<TempFileChunk> result = (Collection<TempFileChunk>) query.executeWithMap(params);
			logger.debug("getTempFileChunks: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getTempFileChunks: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

// done by listener, now...
//	@Override
//	public void deletePersistent(TempFileChunk entity) {
//		deleteDependencies(Collections.singleton(entity));
//		super.deletePersistent(entity);
//	}
//
//	@Override
//	public void deletePersistentAll(Collection<? extends TempFileChunk> entities) {
//		deleteDependencies(entities);
//		super.deletePersistentAll(entities);
//	}
//
//	private void deleteDependencies(Collection<? extends TempFileChunk> tempFileChunks) {
//		final FileChunkPayloadDao fileChunkPayloadDao = getDao(FileChunkPayloadDao.class);
//		for (final TempFileChunk tempFileChunk : tempFileChunks) {
//			final FileChunkPayload fileChunkPayload = fileChunkPayloadDao.getFileChunkPayload(tempFileChunk);
//			if (fileChunkPayload != null)
//				fileChunkPayloadDao.deletePersistent(fileChunkPayload);
//		}
//		getPersistenceManager().flush();
//	}
}
