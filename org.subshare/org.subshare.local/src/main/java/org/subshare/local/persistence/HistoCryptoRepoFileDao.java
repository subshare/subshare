package org.subshare.local.persistence;

import static java.util.Objects.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.HistoCryptoRepoFileDto;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.FetchPlanBackup;

public class HistoCryptoRepoFileDao extends Dao<HistoCryptoRepoFile, HistoCryptoRepoFileDao> {

	private static final Logger logger = LoggerFactory.getLogger(HistoCryptoRepoFileDao.class);

	public Collection<HistoCryptoRepoFile> getHistoCryptoRepoFilesChangedAfterExclLastSyncFromRepositoryId(
			final long localRevision, final UUID exclLastSyncFromRepositoryId) {
		requireNonNull(exclLastSyncFromRepositoryId, "exclLastSyncFromRepositoryId");

		final PersistenceManager pm = pm();
		final FetchPlanBackup fetchPlanBackup = FetchPlanBackup.createFrom(pm);
		final Query query = pm.newNamedQuery(getEntityClass(), "getHistoCryptoRepoFilesChangedAfter_localRevision_exclLastSyncFromRepositoryId");
		try {
			clearFetchGroups();
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<HistoCryptoRepoFile> result = (Collection<HistoCryptoRepoFile>) query.execute(localRevision, exclLastSyncFromRepositoryId.toString());
			logger.debug("getHistoCryptoRepoFilesChangedAfterExclLastSyncFromRepositoryId: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			fetchPlanBackup.restore(pm);
			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getHistoCryptoRepoFilesChangedAfterExclLastSyncFromRepositoryId: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
			fetchPlanBackup.restore(pm);
		}
	}

	public List<HistoCryptoRepoFileDto> getHistoCryptoRepoFileDtosChangedAfterExclLastSyncFromRepositoryId(
			final long localRevision, final UUID exclLastSyncFromRepositoryId) {
		requireNonNull(exclLastSyncFromRepositoryId, "exclLastSyncFromRepositoryId");

		final PersistenceManager pm = pm();
		final FetchPlanBackup fetchPlanBackup = FetchPlanBackup.createFrom(pm);
		final Query query = pm.newNamedQuery(getEntityClass(), "getHistoCryptoRepoFilesChangedAfter_localRevision_exclLastSyncFromRepositoryId");
		try {
			clearFetchGroups();
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<HistoCryptoRepoFile> result = (Collection<HistoCryptoRepoFile>) query.execute(localRevision, exclLastSyncFromRepositoryId.toString());
			logger.debug("getHistoCryptoRepoFileDtosChangedAfterExclLastSyncFromRepositoryId: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			List<HistoCryptoRepoFileDto> resultDtos = loadDtos(result);
			logger.debug("getHistoCryptoRepoFileDtosChangedAfterExclLastSyncFromRepositoryId: Loading result-set with {} elements took {} ms.", resultDtos.size(), System.currentTimeMillis() - startTimestamp);

			return resultDtos;
		} finally {
			query.closeAll();
			fetchPlanBackup.restore(pm);
		}
	}

	protected List<HistoCryptoRepoFileDto> loadDtos(Collection<HistoCryptoRepoFile> entities) {
		return super.loadDtos(entities, HistoCryptoRepoFileDto.class,
				"this.histoCryptoRepoFileId, this.previousHistoCryptoRepoFile.histoCryptoRepoFileId, "
				+ "this.cryptoRepoFile.cryptoRepoFileId, this.histoFrame.histoFrameId, "
				+ "this.cryptoKey.cryptoKeyId, this.repoFileDtoData, this.deleted, "
				+ "this.deletedByIgnoreRule, this.signature");
	}

	public Collection<HistoCryptoRepoFile> getHistoCryptoRepoFiles(final CryptoRepoFile cryptoRepoFile) {
		requireNonNull(cryptoRepoFile, "cryptoRepoFile");
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
		requireNonNull(histoFrame, "histoFrame");

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
		requireNonNull(histoCryptoRepoFile, "getHistoCryptoRepoFile(" + histoCryptoRepoFileId + ")");
		return histoCryptoRepoFile;
	}

	public HistoCryptoRepoFile getHistoCryptoRepoFile(final Uid histoCryptoRepoFileId) {
		requireNonNull(histoCryptoRepoFileId, "histoCryptoRepoFileId");
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

	public Collection<HistoCryptoRepoFile> getHistoCryptoRepoFilesByCollisions(final Set<Uid> collisionIds) {
		requireNonNull(collisionIds, "collisionIds");
		final Set<String> collisionIdsAsString = new HashSet<>(collisionIds.size());
		for (final Uid collisionId : collisionIds)
			collisionIdsAsString.add(collisionId.toString());

		final Query query = pm().newNamedQuery(getEntityClass(), "getHistoCryptoRepoFilesByCollisions");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<HistoCryptoRepoFile> result = (Collection<HistoCryptoRepoFile>) query.execute(collisionIdsAsString);
			logger.debug("getHistoCryptoRepoFilesByCollisions: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getHistoCryptoRepoFilesByCollisions: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}
}
