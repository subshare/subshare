package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

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
			Collection<UserRepoKeyPublicKeyReplacementRequest> requests = (Collection<UserRepoKeyPublicKeyReplacementRequest>) query.execute(localRevision);
			logger.debug("getUserRepoKeyPublicKeyReplacementRequestsChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			requests = load(requests);
			logger.debug("getUserRepoKeyPublicKeyReplacementRequestsChangedAfter: Loading result-set with {} elements took {} ms.", requests.size(), System.currentTimeMillis() - startTimestamp);

			return requests;
		} finally {
			query.closeAll();
		}
	}

	/**
	 * Gets those {@link UserRepoKeyPublicKeyReplacementRequest}s whose {@link UserRepoKeyPublicKeyReplacementRequest#getOldKey() oldKey}
	 * matches the given {@code oldKey}.
	 * <p>
	 * There is usually exactly 0 or 1. Only in case of collisions (invitation key is imported by invited user on 2 of his machines),
	 * there might be more than 1 - and we might even disallow this, later.
	 * @param oldKey {@link UserRepoKeyPublicKeyReplacementRequest#getOldKey() oldKey} for which to search related replacement-requests.
	 * Must not be <code>null</code>.
	 * @return the replacement-requests found matching the given criteria - never <code>null</code>, but maybe empty.
	 */
	public Collection<UserRepoKeyPublicKeyReplacementRequest> getUserRepoKeyPublicKeyReplacementRequestsForOldKey(InvitationUserRepoKeyPublicKey oldKey) {
		assertNotNull("oldKey", oldKey);
		final Query query = pm().newNamedQuery(getEntityClass(), "getUserRepoKeyPublicKeyReplacementRequests_oldKey");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<UserRepoKeyPublicKeyReplacementRequest> requests = (Collection<UserRepoKeyPublicKeyReplacementRequest>) query.execute(oldKey);
			logger.debug("getUserRepoKeyPublicKeyReplacementRequestsForOldKey: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			requests = load(requests);
			logger.debug("getUserRepoKeyPublicKeyReplacementRequestsForOldKey: Loading result-set with {} elements took {} ms.", requests.size(), System.currentTimeMillis() - startTimestamp);

			return requests;
		} finally {
			query.closeAll();
		}
	}

//	public Collection<UserRepoKeyPublicKeyReplacementRequest> getUserRepoKeyPublicKeyReplacementRequestsForNewKey(UserRepoKeyPublicKey newKey) {
//		assertNotNull("newKey", newKey);
//		final Query query = pm().newNamedQuery(getEntityClass(), "getUserRepoKeyPublicKeyReplacementRequests_newKey");
//		try {
//			long startTimestamp = System.currentTimeMillis();
//			@SuppressWarnings("unchecked")
//			Collection<UserRepoKeyPublicKeyReplacementRequest> requests = (Collection<UserRepoKeyPublicKeyReplacementRequest>) query.execute(newKey);
//			logger.debug("getUserRepoKeyPublicKeyReplacementRequestsForNewKey: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);
//
//			startTimestamp = System.currentTimeMillis();
//			requests = load(requests);
//			logger.debug("getUserRepoKeyPublicKeyReplacementRequestsForNewKey: Loading result-set with {} elements took {} ms.", requests.size(), System.currentTimeMillis() - startTimestamp);
//
//			return requests;
//		} finally {
//			query.closeAll();
//		}
//	}
}
