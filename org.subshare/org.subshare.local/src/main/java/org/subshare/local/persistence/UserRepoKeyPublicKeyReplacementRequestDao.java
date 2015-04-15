package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.util.Collection;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;

public class UserRepoKeyPublicKeyReplacementRequestDao extends Dao<UserRepoKeyPublicKeyReplacementRequest, UserRepoKeyPublicKeyReplacementRequestDao> {

	private static final Logger logger = LoggerFactory.getLogger(UserRepoKeyPublicKeyReplacementRequestDao.class);

	public UserRepoKeyPublicKeyReplacementRequest getUserRepoKeyPublicKeyReplacementRequestOrFail(final Uid requestId) {
		final UserRepoKeyPublicKeyReplacementRequest request = getUserRepoKeyPublicKeyReplacementRequest(requestId);
		if (request == null)
			throw new IllegalArgumentException("There is no UserRepoKeyPublicKeyReplacementRequest with this requestId: " + requestId);

		return request;
	}

	public UserRepoKeyPublicKeyReplacementRequest getUserRepoKeyPublicKeyReplacementRequest(final Uid requestId) {
		assertNotNull("requestId", requestId);
		final Query query = pm().newNamedQuery(getEntityClass(), "getUserRepoKeyPublicKeyReplacementRequest_requestId");
		try {
			final UserRepoKeyPublicKeyReplacementRequest request = (UserRepoKeyPublicKeyReplacementRequest) query.execute(requestId.toString());
			return request;
		} finally {
			query.closeAll();
		}
	}

	/**
	 * Get those {@link UserRepoKeyPublicKeyReplacementRequest}s whose {@link UserRepoKeyPublicKeyReplacementRequest#getLocalRevision() localRevision} is greater
	 * than the given {@code localRevision}.
	 * <p>
	 * TODO We should solve <a href="https://github.com/cloudstore/cloudstore/issues/25">issue 25</a> for this
	 * situation here, too, but taking the new TO-DO-notes there into account as well. Note, that this is only
	 * necessary, if UserRepoKeyPublicKeyReplacementRequest instances are mutable. Currently, they seem never to be changed again.
	 * @param localRevision the {@link UserRepoKeyPublicKeyReplacementRequest#getLocalRevision() localRevision}, after which the data
	 * to be queried were modified.
	 * @return those {@link UserRepoKeyPublicKeyReplacementRequest}s which were modified after the given {@code localRevision}. Never
	 * <code>null</code>, but maybe empty.
	 */
	public Collection<UserRepoKeyPublicKeyReplacementRequest> getUserRepoKeyPublicKeyReplacementRequestsChangedAfter(final long localRevision) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getUserRepoKeyPublicKeyReplacementRequestsChangedAfter_localRevision");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<UserRepoKeyPublicKeyReplacementRequest> keys = (Collection<UserRepoKeyPublicKeyReplacementRequest>) query.execute(localRevision);
			logger.debug("getUserRepoKeyPublicKeyReplacementRequestsChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			keys = load(keys);
			logger.debug("getUserRepoKeyPublicKeyReplacementRequestsChangedAfter: Loading result-set with {} elements took {} ms.", keys.size(), System.currentTimeMillis() - startTimestamp);

			return keys;
		} finally {
			query.closeAll();
		}
	}
}
