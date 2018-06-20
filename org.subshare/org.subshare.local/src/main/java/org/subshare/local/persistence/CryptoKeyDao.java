package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CryptoKeyRole;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.FetchPlanBackup;

public class CryptoKeyDao extends Dao<CryptoKey, CryptoKeyDao> {
	private static final Logger logger = LoggerFactory.getLogger(CryptoKeyDao.class);

	public CryptoKey getCryptoKeyOrFail(final Uid cryptoKeyId) {
		final CryptoKey cryptoKey = getCryptoKey(cryptoKeyId);
		if (cryptoKey == null)
			throw new IllegalArgumentException("There is no CryptoKey with this cryptoKeyId: " + cryptoKeyId);

		return cryptoKey;
	}

	public CryptoKey getCryptoKey(final Uid cryptoKeyId) {
		assertNotNull(cryptoKeyId, "cryptoKeyId");
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoKey_cryptoKeyId");
		try {
			final CryptoKey cryptoKey = (CryptoKey) query.execute(cryptoKeyId.toString());
			return cryptoKey;
		} finally {
			query.closeAll();
		}
	}

	public Collection<CryptoKey> getActiveCryptoKeys(final CryptoRepoFile cryptoRepoFile, final CryptoKeyRole cryptoKeyRole) {
		assertNotNull(cryptoRepoFile, "cryptoRepoFile");
		assertNotNull(cryptoKeyRole, "cryptoKeyRole");
		final Query query = pm().newNamedQuery(getEntityClass(), "getActiveCryptoKeys_cryptoRepoFile_cryptoKeyRole");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoKey> cryptoKeys = (Collection<CryptoKey>) query.execute(cryptoRepoFile, cryptoKeyRole);
			logger.debug("getCryptoKeys: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			cryptoKeys = load(cryptoKeys);
			logger.debug("getCryptoKeys: Loading result-set with {} elements took {} ms.", cryptoKeys.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoKeys;
		} finally {
			query.closeAll();
		}
	}

	/**
	 * Get those {@link CryptoKey}s whose {@link CryptoKey#getLocalRevision() localRevision} is greater
	 * than the given {@code localRevision}.
	 * <p>
	 * TODO We should solve <a href="https://github.com/cloudstore/cloudstore/issues/25">issue 25</a> for this
	 * situation here, too, but taking the new TO-DO-notes there into account as well. Note, that this is only
	 * necessary, if CryptoKey/CryptoKey instances are mutable. Currently, they seem never to be changed again.
	 * @param localRevision the {@link CryptoKey#getLocalRevision() localRevision}, after which the files
	 * to be queried where modified.
	 *
	 * @return those {@link CryptoKey}s which were modified after the given {@code localRevision}. Never
	 * <code>null</code>, but maybe empty.
	 */
	public Collection<CryptoKey> getCryptoKeysChangedAfterExclLastSyncFromRepositoryId(
			final long localRevision, final UUID exclLastSyncFromRepositoryId) {
		assertNotNull(exclLastSyncFromRepositoryId, "exclLastSyncFromRepositoryId");

		final PersistenceManager pm = pm();
		final FetchPlanBackup fetchPlanBackup = FetchPlanBackup.createFrom(pm);
		final Query query = pm.newNamedQuery(getEntityClass(), "getCryptoKeysChangedAfter_localRevision_exclLastSyncFromRepositoryId");
		try {
			clearFetchGroups();
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoKey> cryptoKeys = (Collection<CryptoKey>) query.execute(localRevision, exclLastSyncFromRepositoryId.toString());
			logger.debug("getCryptoKeysChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			fetchPlanBackup.restore(pm);
			startTimestamp = System.currentTimeMillis();
			cryptoKeys = load(cryptoKeys);
			logger.debug("getCryptoKeysChangedAfter: Loading result-set with {} elements took {} ms.", cryptoKeys.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoKeys;
		} finally {
			query.closeAll();
			fetchPlanBackup.restore(pm);
		}
	}

	public Collection<CryptoKey> getCryptoKeys(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull(cryptoRepoFile, "cryptoRepoFile");
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoKeys_cryptoRepoFile");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoKey> cryptoKeys = (Collection<CryptoKey>) query.execute(cryptoRepoFile);
			logger.debug("getCryptoKeys: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			cryptoKeys = load(cryptoKeys);
			logger.debug("getCryptoKeys: Loading result-set with {} elements took {} ms.", cryptoKeys.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoKeys;
		} finally {
			query.closeAll();
		}
	}

	@Override
	public void deletePersistent(CryptoKey entity) {
		deleteDependentObjects(entity);
		pm().flush();
		super.deletePersistent(entity);
	}

	@Override
	public void deletePersistentAll(Collection<? extends CryptoKey> entities) {
		for (final CryptoKey cryptoKey : entities)
			deleteDependentObjects(cryptoKey);

		pm().flush();
		super.deletePersistentAll(entities);
	}

	protected void deleteDependentObjects(final CryptoKey cryptoKey) {
		assertNotNull(cryptoKey, "cryptoKey");
		final CryptoLinkDao cryptoLinkDao = getDao(CryptoLinkDao.class);

		final Collection<CryptoLink> cryptoLinksFrom = cryptoLinkDao.getCryptoLinksFrom(cryptoKey);
		final Collection<CryptoLink> cryptoLinksTo = cryptoLinkDao.getCryptoLinksTo(cryptoKey);
		final Set<CryptoLink> cryptoLinks = new HashSet<CryptoLink>(cryptoLinksFrom.size() + cryptoLinksTo.size());
		cryptoLinks.addAll(cryptoLinksFrom);
		cryptoLinks.addAll(cryptoLinksTo);

		cryptoLinkDao.deletePersistentAll(cryptoLinks);
	}
}
