package org.subshare.local.persistence;

import static java.util.Objects.*;

import java.util.HashMap;
import java.util.Map;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.FileChunk;
import co.codewizards.cloudstore.local.persistence.NormalFile;

public class FileChunkPayloadDao extends Dao<FileChunkPayload, FileChunkPayloadDao> {

	private static final Logger logger = LoggerFactory.getLogger(FileChunkPayloadDao.class);

	public FileChunkPayload getFileChunkPayload(TempFileChunk tempFileChunk) {
		requireNonNull(tempFileChunk, "tempFileChunk");

		final Query query = pm().newNamedQuery(getEntityClass(), "getFileChunkPayload_tempFileChunk");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(1);
			params.put("tempFileChunk", tempFileChunk);

			long startTimestamp = System.currentTimeMillis();
			final FileChunkPayload result = (FileChunkPayload) query.executeWithMap(params);
			logger.debug("getFileChunkPayload(TempFileChunk): query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public FileChunkPayload getFileChunkPayload(FileChunk fileChunk) {
		requireNonNull(fileChunk, "fileChunk");

		final Query query = pm().newNamedQuery(getEntityClass(), "getFileChunkPayload_fileChunk");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(1);
			params.put("fileChunk", fileChunk);

			long startTimestamp = System.currentTimeMillis();
			final FileChunkPayload result = (FileChunkPayload) query.executeWithMap(params);
			logger.debug("getFileChunkPayload(FileChunk): query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public FileChunkPayload getFileChunkPayloadOfFileChunk(NormalFile normalFile, long offset) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getFileChunkPayloadOfFileChunk_normalFile_offset");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(2);
			params.put("normalFile", normalFile);
			params.put("offset", offset);

			long startTimestamp = System.currentTimeMillis();
			final FileChunkPayload result = (FileChunkPayload) query.executeWithMap(params);
			logger.debug("getFileChunkPayloadOfFileChunk: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public FileChunkPayload getFileChunkPayloadOfHistoFileChunk(HistoCryptoRepoFile histoCryptoRepoFile, long offset) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getFileChunkPayloadOfHistoFileChunk_histoCryptoRepoFile_offset");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(2);
			params.put("histoCryptoRepoFile", histoCryptoRepoFile);
			params.put("offset", offset);

			long startTimestamp = System.currentTimeMillis();
			final FileChunkPayload result = (FileChunkPayload) query.executeWithMap(params);
			logger.debug("getFileChunkPayloadOfFileChunk: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);
			return result;
		} finally {
			query.closeAll();
		}
	}

}
