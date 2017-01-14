package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.UUID;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.local.persistence.Dao;

public class CryptoConfigPropSetDao extends Dao<CryptoConfigPropSet, CryptoConfigPropSetDao> {

	private static final Logger logger = LoggerFactory.getLogger(CryptoConfigPropSetDao.class);

	public CryptoConfigPropSet getCryptoConfigPropSetOrFail(final CryptoRepoFile cryptoRepoFile) {
		final CryptoConfigPropSet result = getCryptoConfigPropSet(cryptoRepoFile);
		if (result == null)
			throw new IllegalArgumentException("There is no CryptoConfigPropSet for: " + cryptoRepoFile);

		return result;
	}

	public CryptoConfigPropSet getCryptoConfigPropSet(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull(cryptoRepoFile, "cryptoRepoFile");
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoConfigPropSet_cryptoRepoFile");
		try {
			final CryptoConfigPropSet result = (CryptoConfigPropSet) query.execute(cryptoRepoFile);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<CryptoConfigPropSet> getCryptoConfigPropSetsChangedAfterExclLastSyncFromRepositoryId(
			final long localRevision, final UUID exclLastSyncFromRepositoryId) {

		assertNotNull(exclLastSyncFromRepositoryId, "exclLastSyncFromRepositoryId");
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoConfigPropSetsChangedAfter_localRevision_exclLastSyncFromRepositoryId");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoConfigPropSet> result = (Collection<CryptoConfigPropSet>) query.execute(localRevision, exclLastSyncFromRepositoryId.toString());
			logger.debug("getCryptoConfigPropSetsChangedAfterExclLastSyncFromRepositoryId: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getCryptoConfigPropSetsChangedAfterExclLastSyncFromRepositoryId: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}
}
