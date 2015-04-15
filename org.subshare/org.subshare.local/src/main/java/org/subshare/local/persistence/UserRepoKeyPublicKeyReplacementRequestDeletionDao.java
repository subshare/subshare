package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.util.Collection;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;

public class UserRepoKeyPublicKeyReplacementRequestDeletionDao extends Dao<UserRepoKeyPublicKeyReplacementRequestDeletion, UserRepoKeyPublicKeyReplacementRequestDeletionDao> {

	private static final Logger logger = LoggerFactory.getLogger(UserRepoKeyPublicKeyReplacementRequestDeletionDao.class);

	public UserRepoKeyPublicKeyReplacementRequestDeletion getUserRepoKeyPublicKeyReplacementRequestOrFail(final Uid requestId) {
		final UserRepoKeyPublicKeyReplacementRequestDeletion requestDeletion = getUserRepoKeyPublicKeyReplacementRequestDeletion(requestId);
		if (requestDeletion == null)
			throw new IllegalArgumentException("There is no UserRepoKeyPublicKeyReplacementRequestDeletion with this requestId: " + requestId);

		return requestDeletion;
	}

	public UserRepoKeyPublicKeyReplacementRequestDeletion getUserRepoKeyPublicKeyReplacementRequestDeletion(final Uid requestId) {
		assertNotNull("requestId", requestId);
		final Query query = pm().newNamedQuery(getEntityClass(), "getUserRepoKeyPublicKeyReplacementRequestDeletion_requestId");
		try {
			final UserRepoKeyPublicKeyReplacementRequestDeletion requestDeletion = (UserRepoKeyPublicKeyReplacementRequestDeletion) query.execute(requestId.toString());
			return requestDeletion;
		} finally {
			query.closeAll();
		}
	}

	/**
	 * Get those {@link UserRepoKeyPublicKeyReplacementRequestDeletion}s whose {@link UserRepoKeyPublicKeyReplacementRequestDeletion#getLocalRevision() localRevision} is greater
	 * than the given {@code localRevision}.
	 * <p>
	 * TODO We should solve <a href="https://github.com/cloudstore/cloudstore/issues/25">issue 25</a> for this
	 * situation here, too, but taking the new TO-DO-notes there into account as well. Note, that this is only
	 * necessary, if UserRepoKeyPublicKeyReplacementRequestDeletion instances are mutable. Currently, they seem never to be changed again.
	 * @param localRevision the {@link UserRepoKeyPublicKeyReplacementRequestDeletion#getLocalRevision() localRevision}, after which the data
	 * to be queried were modified.
	 * @return those {@link UserRepoKeyPublicKeyReplacementRequestDeletion}s which were modified after the given {@code localRevision}. Never
	 * <code>null</code>, but maybe empty.
	 */
	public Collection<UserRepoKeyPublicKeyReplacementRequestDeletion> getUserRepoKeyPublicKeyReplacementRequestDeletionsChangedAfter(final long localRevision) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getUserRepoKeyPublicKeyReplacementRequestDeletionsChangedAfter_localRevision");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<UserRepoKeyPublicKeyReplacementRequestDeletion> result = (Collection<UserRepoKeyPublicKeyReplacementRequestDeletion>) query.execute(localRevision);
			logger.debug("getUserRepoKeyPublicKeyReplacementRequestDeletionsChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getUserRepoKeyPublicKeyReplacementRequestDeletionsChangedAfter: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}
}
