package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;

public class DeletedCollisionDao extends Dao<DeletedCollision, DeletedCollisionDao> {

	private static final Logger logger = LoggerFactory.getLogger(DeletedCollisionDao.class);

	public DeletedCollision getDeletedCollisionOrFail(final Uid collisionId) {
		final DeletedCollision result = getDeletedCollision(collisionId);
		assertNotNull(result, "getDeletedCollision(" + collisionId + ")");
		return result;
	}

	public DeletedCollision getDeletedCollision(final Uid collisionId) {
		assertNotNull(collisionId, "collisionId");
		final Query query = pm().newNamedQuery(getEntityClass(), "getDeletedCollision_collisionId");
		try {
			final DeletedCollision result = (DeletedCollision) query.execute(collisionId.toString());
			return result;
		} finally {
			query.closeAll();
		}
	}


	public Collection<DeletedCollision> getDeletedCollisionsChangedAfter(final long localRevision) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getDeletedCollisionsChangedAfter_localRevision");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<DeletedCollision> result = (Collection<DeletedCollision>) query.execute(localRevision);
			logger.debug("getDeletedCollisionsChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getDeletedCollisionsChangedAfter: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}
}
