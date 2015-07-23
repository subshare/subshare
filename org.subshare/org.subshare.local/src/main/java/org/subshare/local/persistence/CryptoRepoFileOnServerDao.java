package org.subshare.local.persistence;

import java.util.Collection;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.local.persistence.Dao;

public class CryptoRepoFileOnServerDao extends Dao<CryptoRepoFileOnServer, CryptoRepoFileOnServerDao> {

	private static final Logger logger = LoggerFactory.getLogger(CryptoRepoFileOnServerDao.class);

	public Collection<CryptoRepoFileOnServer> getCryptoRepoFileOnServersChangedAfter(
			final long localRevision) {

		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoRepoFileOnServerChangedAfter_localRevision");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoRepoFileOnServer> result = (Collection<CryptoRepoFileOnServer>) query.execute(localRevision);
			logger.debug("getCryptoRepoFileOnServersChangedAfterExclLastSyncFromRepositoryId: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getCryptoRepoFileOnServersChangedAfterExclLastSyncFromRepositoryId: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	@Override
	public void deletePersistent(CryptoRepoFileOnServer entity) {
		super.deletePersistent(entity);
	}

	@Override
	public void deletePersistentAll(final Collection<? extends CryptoRepoFileOnServer> entities) {
		super.deletePersistentAll(entities);
	}
}
