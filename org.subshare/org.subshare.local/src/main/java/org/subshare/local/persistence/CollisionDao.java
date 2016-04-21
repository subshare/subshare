package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.repo.local.CollisionFilter;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;

public class CollisionDao extends Dao<Collision, CollisionDao> {

	private static final Logger logger = LoggerFactory.getLogger(CollisionDao.class);

	public Collision getCollisionOrFail(final Uid collisionId) {
		final Collision collision = getCollision(collisionId);
		assertNotNull("getCollision(" + collisionId + ")", collision);
		return collision;
	}

	public Collision getCollision(final Uid collisionId) {
		assertNotNull("collisionId", collisionId);
		final Query query = pm().newNamedQuery(getEntityClass(), "getCollision_collisionId");
		try {
			final Collision result = (Collision) query.execute(collisionId.toString());
			return result;
		} finally {
			query.closeAll();
		}
	}

	/**
	 * Gets the {@link Collision} between the two given {@link HistoCryptoRepoFile}s, or <code>null</code>, if no such object exists.
	 * There is no difference, if a certain {@code HistoCryptoRepoFile} is passed as the first
	 * or the 2nd argument, i.e. they can be interchanged.
	 * <p>
	 * In other words, the following code always prints "EQUAL":
	 * <p>
	 * <pre>
	 * HistoCryptoRepoFile hcrf1 = // get one instance
	 * HistoCryptoRepoFile hcrf2 = // get another instance
	 * Collision collisionA = dao.getCollision(hcrf1, hcrf2);
	 * Collision collisionB = dao.getCollision(hcrf2, hcrf1);
	 * if (collisionA == collisionB))
	 *   System.out.println("EQUAL");
	 * else
	 *   System.out.println("*NOT* EQUAL");
	 * </pre>
	 * @param histoCryptoRepoFile1 the 1st {@code HistoCryptoRepoFile} involved. Must not be <code>null</code>.
	 * @param histoCryptoRepoFile2 the 2nd {@code HistoCryptoRepoFile} involved. Must not be <code>null</code>.
	 * @return the {@code Collision} between the two {@code HistoCryptoRepoFile} instances or <code>null</code>, if there is none.
	 */
	public Collision getCollision(final HistoCryptoRepoFile histoCryptoRepoFile1, final HistoCryptoRepoFile histoCryptoRepoFile2) {
		final Map<String, Object> params = new HashMap<>();
		params.put("histoCryptoRepoFile1", assertNotNull("histoCryptoRepoFile1", histoCryptoRepoFile1));
		params.put("histoCryptoRepoFile2", assertNotNull("histoCryptoRepoFile2", histoCryptoRepoFile2));
		final Query query = pm().newNamedQuery(getEntityClass(), "getCollisions_histoCryptoRepoFile1_histoCryptoRepoFile2");
		try {
			final Collision result = (Collision) query.executeWithMap(params);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<Collision> getCollisionsChangedAfter(final long localRevision) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getCollisionsChangedAfter_localRevision");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<Collision> result = (Collection<Collision>) query.execute(localRevision);
			logger.debug("getCollisionsChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getCollisionsChangedAfter: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<Collision> getCollisions(final CollisionFilter filter) {
		assertNotNull("filter", filter);
		final Query query = pm().newQuery(Collision.class);
		try {
			final StringBuilder qf = new StringBuilder();
			final Map<String, Object> qp = new HashMap<>();

			appendToQueryFilter_histoCryptoRepoFileId(qf, qp, filter.getHistoCryptoRepoFileId());
			appendToQueryFilter_cryptoRepoFileId(qf, qp, filter.getHistoCryptoRepoFileId());

			if (qf.length() > 0)
				query.setFilter(qf.toString());

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<Collision> result = (Collection<Collision>) query.executeWithMap(qp);
			logger.debug("getCollisionsChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getCollisionsChangedAfter: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	private static void appendToQueryFilter_histoCryptoRepoFileId(final StringBuilder qf, final Map<String, Object> qp, final Uid histoCryptoRepoFileId) {
		assertNotNull("qf", qf);
		assertNotNull("qp", qp);
		if (histoCryptoRepoFileId == null)
			return;

		appendAndIfNeeded(qf);

		qf.append("(")
		.append("  this.histoCryptoRepoFile1.histoCryptoRepoFileId == :histoCryptoRepoFileId")
		.append("  || this.histoCryptoRepoFile2.histoCryptoRepoFileId == :histoCryptoRepoFileId")
		.append(")");

		qp.put("histoCryptoRepoFileId", histoCryptoRepoFileId.toString());
	}

	private static void appendToQueryFilter_cryptoRepoFileId(final StringBuilder qf, final Map<String, Object> qp, final Uid cryptoRepoFileId) {
		assertNotNull("qf", qf);
		assertNotNull("qp", qp);
		if (cryptoRepoFileId == null)
			return;

		appendAndIfNeeded(qf);

		qf.append("(")
		.append("  this.histoCryptoRepoFile1.cryptoRepoFile.cryptoRepoFileId == :cryptoRepoFileId")
		.append("  || this.histoCryptoRepoFile2.cryptoRepoFile.cryptoRepoFileId == :cryptoRepoFileId")
		.append(")");

		qp.put("cryptoRepoFileId", cryptoRepoFileId.toString());
	}

	private static void appendAndIfNeeded(final StringBuilder qf) {
		assertNotNull("qf", qf);
		if (qf.length() > 0)
			qf.append(" && ");
	}
}
