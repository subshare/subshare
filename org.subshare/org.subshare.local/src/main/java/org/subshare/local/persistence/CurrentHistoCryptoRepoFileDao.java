package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CurrentHistoCryptoRepoFileDto;

import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.FetchPlanBackup;

public class CurrentHistoCryptoRepoFileDao extends Dao<CurrentHistoCryptoRepoFile, CurrentHistoCryptoRepoFileDao> {

	private static final Logger logger = LoggerFactory.getLogger(CurrentHistoCryptoRepoFileDao.class);

	public Collection<CurrentHistoCryptoRepoFile> getCurrentHistoCryptoRepoFilesChangedAfterExclLastSyncFromRepositoryId(
			final long localRevision, final UUID exclLastSyncFromRepositoryId) {
		assertNotNull(exclLastSyncFromRepositoryId, "exclLastSyncFromRepositoryId");

		final PersistenceManager pm = pm();
		final FetchPlanBackup fetchPlanBackup = FetchPlanBackup.createFrom(pm);
		final Query query = pm.newNamedQuery(getEntityClass(), "getCurrentHistoCryptoRepoFilesChangedAfter_localRevision_exclLastSyncFromRepositoryId");
		try {
			clearFetchGroups();
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CurrentHistoCryptoRepoFile> result = (Collection<CurrentHistoCryptoRepoFile>) query.execute(localRevision, exclLastSyncFromRepositoryId.toString());
			logger.debug("getCurrentHistoCryptoRepoFilesChangedAfterExclLastSyncFromRepositoryId: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			fetchPlanBackup.restore(pm);
			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getCurrentHistoCryptoRepoFilesChangedAfterExclLastSyncFromRepositoryId: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
			fetchPlanBackup.restore(pm);
		}
	}

	public List<CurrentHistoCryptoRepoFileDto> getCurrentHistoCryptoRepoFileDtosChangedAfterExclLastSyncFromRepositoryId(
			final long localRevision, final UUID exclLastSyncFromRepositoryId) {
		assertNotNull(exclLastSyncFromRepositoryId, "exclLastSyncFromRepositoryId");

		final PersistenceManager pm = pm();
		final FetchPlanBackup fetchPlanBackup = FetchPlanBackup.createFrom(pm);
		final Query query = pm.newNamedQuery(getEntityClass(), "getCurrentHistoCryptoRepoFilesChangedAfter_localRevision_exclLastSyncFromRepositoryId");
		try {
			clearFetchGroups();
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CurrentHistoCryptoRepoFile> result = (Collection<CurrentHistoCryptoRepoFile>) query.execute(localRevision, exclLastSyncFromRepositoryId.toString());
			logger.debug("getCurrentHistoCryptoRepoFileDtosChangedAfterExclLastSyncFromRepositoryId: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			fetchPlanBackup.restore(pm);
			startTimestamp = System.currentTimeMillis();
			List<CurrentHistoCryptoRepoFileDto> resultDtos = loadDtos(result);
			logger.debug("getCurrentHistoCryptoRepoFileDtosChangedAfterExclLastSyncFromRepositoryId: Loading result-set with {} elements took {} ms.", resultDtos.size(), System.currentTimeMillis() - startTimestamp);

			return resultDtos;
		} finally {
			query.closeAll();
			fetchPlanBackup.restore(pm);
		}
	}

	protected List<CurrentHistoCryptoRepoFileDto> loadDtos(Collection<CurrentHistoCryptoRepoFile> entities) {
		return super.loadDtos(entities, CurrentHistoCryptoRepoFileDto.class,
				"this.cryptoRepoFile.cryptoRepoFileId, this.histoCryptoRepoFile.histoCryptoRepoFileId, this.signature");
	}

	public CurrentHistoCryptoRepoFile getCurrentHistoCryptoRepoFile(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull(cryptoRepoFile, "cryptoRepoFile");
		final Query query = pm().newNamedQuery(getEntityClass(), "getCurrentHistoCryptoRepoFile_cryptoRepoFile");
		try {
			final CurrentHistoCryptoRepoFile result = (CurrentHistoCryptoRepoFile) query.execute(cryptoRepoFile);
			return result;
		} finally {
			query.closeAll();
		}
	}

	@Override
	public void deletePersistent(CurrentHistoCryptoRepoFile entity) {
		super.deletePersistent(entity);
	}

	@Override
	public void deletePersistentAll(Collection<? extends CurrentHistoCryptoRepoFile> entities) {
		super.deletePersistentAll(entities);
	}
}
