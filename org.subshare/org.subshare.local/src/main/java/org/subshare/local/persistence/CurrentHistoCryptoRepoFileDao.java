package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.UUID;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.local.persistence.Dao;

public class CurrentHistoCryptoRepoFileDao extends Dao<CurrentHistoCryptoRepoFile, CurrentHistoCryptoRepoFileDao> {

	private static final Logger logger = LoggerFactory.getLogger(CurrentHistoCryptoRepoFileDao.class);

	public Collection<CurrentHistoCryptoRepoFile> getCurrentHistoCryptoRepoFilesChangedAfterExclLastSyncFromRepositoryId(
			final long localRevision, final UUID exclLastSyncFromRepositoryId) {
		assertNotNull("exclLastSyncFromRepositoryId", exclLastSyncFromRepositoryId);

		final Query query = pm().newNamedQuery(getEntityClass(), "getCurrentHistoCryptoRepoFilesChangedAfter_localRevision_exclLastSyncFromRepositoryId");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CurrentHistoCryptoRepoFile> result = (Collection<CurrentHistoCryptoRepoFile>) query.execute(localRevision, exclLastSyncFromRepositoryId.toString());
			logger.debug("getCurrentHistoCryptoRepoFilesChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getCurrentHistoCryptoRepoFilesChangedAfter: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public CurrentHistoCryptoRepoFile getCurrentHistoCryptoRepoFile(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull("cryptoRepoFile", cryptoRepoFile);
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
