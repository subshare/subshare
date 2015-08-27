package org.subshare.local.dbrepo.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class TempFileChunkDao extends Dao<TempFileChunk, TempFileChunkDao> {

	private static final Logger logger = LoggerFactory.getLogger(TempFileChunkDao.class);

	public TempFileChunk getTempFileChunk(RepoFile repoFile, UUID remoteRepositoryId, long offset) {
		assertNotNull("repoFile", repoFile);
		assertNotNull("remoteRepositoryId", remoteRepositoryId);

		final Query query = pm().newNamedQuery(getEntityClass(), "getTempFileChunk_repoFile_remoteRepositoryId_offset");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(3);
			params.put("repoFile", repoFile);
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

	public Collection<TempFileChunk> getTempFileChunks(RepoFile repoFile) {
		assertNotNull("repoFile", repoFile);

		final Query query = pm().newNamedQuery(getEntityClass(), "getTempFileChunks_repoFile");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(1);
			params.put("repoFile", repoFile);

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

	public Collection<TempFileChunk> getTempFileChunks(RepoFile repoFile, UUID remoteRepositoryId) {
		assertNotNull("repoFile", repoFile);
		assertNotNull("remoteRepositoryId", remoteRepositoryId);

		final Query query = pm().newNamedQuery(getEntityClass(), "getTempFileChunks_repoFile_remoteRepositoryId");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(2);
			params.put("repoFile", repoFile);
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

	@Override
	public void deletePersistent(TempFileChunk entity) {
		deleteDependencies(Collections.singleton(entity));
		super.deletePersistent(entity);
	}

	@Override
	public void deletePersistentAll(Collection<? extends TempFileChunk> entities) {
		deleteDependencies(entities);
		super.deletePersistentAll(entities);
	}

	private void deleteDependencies(Collection<? extends TempFileChunk> tempFileChunks) {
		final FileChunkPayloadDao fileChunkPayloadDao = getDao(FileChunkPayloadDao.class);
		for (final TempFileChunk tempFileChunk : tempFileChunks) {
			final FileChunkPayload fileChunkPayload = fileChunkPayloadDao.getFileChunkPayload(tempFileChunk);
			if (fileChunkPayload != null)
				fileChunkPayloadDao.deletePersistent(fileChunkPayload);
		}
		getPersistenceManager().flush();
	}

//	public List<TempFileChunk> getFileChunksSortedByOffset(final RepoFile repoFile, UUID remoteRepositoryId, FileChunkType fileChunkType) {
//		final List<TempFileChunk> result = new ArrayList<TempFileChunk>(getFileChunks(repoFile, remoteRepositoryId, fileChunkType));
//		Collections.sort(result, new Comparator<TempFileChunk>() {
//			@Override
//			public int compare(TempFileChunk o1, TempFileChunk o2) {
//				return Long.compare(o1.getOffset(), o2.getOffset());
//			}
//		});
//		return result;
//	}
}
