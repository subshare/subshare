package org.subshare.local.persistence;

import java.util.Collection;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.local.persistence.DAO;

public class CryptoKeyDAO extends DAO<CryptoKey, CryptoKeyDAO> {
	private static final Logger logger = LoggerFactory.getLogger(CryptoKeyDAO.class);

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
	public Collection<CryptoKey> getCryptoKeysChangedAfter(final long localRevision) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoKeysChangedAfter_localRevision");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoKey> cryptoKeys = (Collection<CryptoKey>) query.execute(localRevision);
			logger.debug("getCryptoKeysChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			cryptoKeys = load(cryptoKeys);
			logger.debug("getCryptoKeysChangedAfter: Loading result-set with {} elements took {} ms.", cryptoKeys.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoKeys;
		} finally {
			query.closeAll();
		}
	}

}
