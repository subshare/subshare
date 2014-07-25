package org.subshare.local.persistence;

import java.util.Collection;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.local.persistence.DAO;

public class CryptoLinkDAO extends DAO<CryptoLink, CryptoLinkDAO> {
	private static final Logger logger = LoggerFactory.getLogger(CryptoLinkDAO.class);

	/**
	 * Get those {@link CryptoLink}s whose {@link CryptoLink#getLocalRevision() localRevision} is greater
	 * than the given {@code localRevision}.
	 * <p>
	 * TODO We should solve <a href="https://github.com/cloudstore/cloudstore/issues/25">issue 25</a> for this
	 * situation here, too, but taking the new TO-DO-notes there into account as well. Note, that this is only
	 * necessary, if CryptoKey/CryptoLink instances are mutable. Currently, they seem never to be changed again.
	 * @param localRevision the {@link CryptoLink#getLocalRevision() localRevision}, after which the files
	 * to be queried where modified.
	 *
	 * @return those {@link CryptoLink}s which were modified after the given {@code localRevision}. Never
	 * <code>null</code>, but maybe empty.
	 */
	public Collection<CryptoLink> getCryptoLinksChangedAfter(final long localRevision) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoLinksChangedAfter_localRevision");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<CryptoLink> cryptoLinks = (Collection<CryptoLink>) query.execute(localRevision);
			logger.debug("getCryptoLinksChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			cryptoLinks = load(cryptoLinks);
			logger.debug("getCryptoLinksChangedAfter: Loading result-set with {} elements took {} ms.", cryptoLinks.size(), System.currentTimeMillis() - startTimestamp);

			return cryptoLinks;
		} finally {
			query.closeAll();
		}
	}

}
