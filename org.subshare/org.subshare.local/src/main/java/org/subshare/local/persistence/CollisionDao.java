package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.repo.local.CollisionFilter;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;

public class CollisionDao extends Dao<Collision, CollisionDao> {

	private static final Logger logger = LoggerFactory.getLogger(CollisionDao.class);

	public Collision getCollisionOrFail(final Uid collisionId) {
		final Collision collision = getCollision(collisionId);
		assertNotNull(collision, "getCollision(" + collisionId + ")");
		return collision;
	}

	public Collision getCollision(final Uid collisionId) {
		assertNotNull(collisionId, "collisionId");
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
		assertNotNull(histoCryptoRepoFile1, "histoCryptoRepoFile1");
		assertNotNull(histoCryptoRepoFile2, "histoCryptoRepoFile2");
		final Map<String, Object> params = new HashMap<>();
		params.put("histoCryptoRepoFile1", assertNotNull(histoCryptoRepoFile1, "histoCryptoRepoFile1"));
		params.put("histoCryptoRepoFile2", assertNotNull(histoCryptoRepoFile2, "histoCryptoRepoFile2"));
		final Query query = pm().newNamedQuery(getEntityClass(), "getCollision_histoCryptoRepoFile1_histoCryptoRepoFile2");
		try {
			final Collision result = (Collision) query.executeWithMap(params);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collision getCollisionWithDuplicateCryptoRepoFileId(HistoCryptoRepoFile histoCryptoRepoFile1, Uid duplicateCryptoRepoFileId) {
		assertNotNull(histoCryptoRepoFile1, "histoCryptoRepoFile1");
		assertNotNull(duplicateCryptoRepoFileId, "duplicateCryptoRepoFileId");
		final Map<String, Object> params = new HashMap<>();
		params.put("histoCryptoRepoFile1", assertNotNull(histoCryptoRepoFile1, "histoCryptoRepoFile1"));
		params.put("duplicateCryptoRepoFileId", assertNotNull(duplicateCryptoRepoFileId, "duplicateCryptoRepoFileId").toString());
		final Query query = pm().newNamedQuery(getEntityClass(), "getCollision_histoCryptoRepoFile1_duplicateCryptoRepoFileId");
		try {
			final Collision result = (Collision) query.executeWithMap(params);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<Collision> getCollisionsWithDuplicateCryptoRepoFileId(Uid cryptoRepoFileId) {
		assertNotNull(cryptoRepoFileId, "cryptoRepoFileId");
		final Query query = pm().newNamedQuery(getEntityClass(), "getCollisions_duplicateCryptoRepoFileId");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<Collision> result = (Collection<Collision>) query.execute(cryptoRepoFileId.toString());
			logger.debug("getCollisionsWithDuplicateCryptoRepoFileId: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getCollisionsWithDuplicateCryptoRepoFileId: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collision getCollision(final HistoCryptoRepoFile histoCryptoRepoFile1, final HistoCryptoRepoFile histoCryptoRepoFile2, final Uid duplicateCryptoRepoFileId) {
		assertNotNull(histoCryptoRepoFile1, "histoCryptoRepoFile1");

		if (duplicateCryptoRepoFileId != null)
			return getCollisionWithDuplicateCryptoRepoFileId(histoCryptoRepoFile1, duplicateCryptoRepoFileId);
		else
			return getCollision(histoCryptoRepoFile1, histoCryptoRepoFile2);
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

	public Collection<Collision> getCollisions(CollisionFilter filter) {
		assertNotNull(filter, "filter");

		filter = prepareFilter(filter);
		if (filter == null)
			return new HashSet<>();

		final Query query = pm().newQuery(Collision.class);
		try {
			final StringBuilder qf = new StringBuilder();
			final Map<String, Object> qp = new HashMap<>();
			final Map<String, Class<?>> qv = new HashMap<>();

			appendToQueryFilter_collisionIds(qf, qp, qv, filter.getCollisionIds());

			appendToQueryFilter_histoCryptoRepoFileId(qf, qp, qv, filter.getHistoCryptoRepoFileId());

			if (filter.isIncludeChildrenRecursively())
				appendToQueryFilter_cryptoRepoFileId_recursive(qf, qp, qv, filter.getCryptoRepoFileId());
			else
				appendToQueryFilter_cryptoRepoFileId_nonRecursive(qf, qp, qv, filter.getCryptoRepoFileId());

//			appendToQueryFilter_resolved(qf, qp, qv, filter.getResolved());

			if (qf.length() > 0)
				query.setFilter(qf.toString());

			if (! qv.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				for (Map.Entry<String, Class<?>> me : qv.entrySet()) {
					sb.append(me.getValue().getName()).append(' ').append(me.getKey()).append(';');
				}
				query.declareVariables(sb.toString());
			}

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<Collision> result = (Collection<Collision>) query.executeWithMap(qp);
			logger.info("getCollisions: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.info("getCollisions: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	private CollisionFilter prepareFilter(CollisionFilter filter) {
		assertNotNull(filter, "filter");

		if (filter.getLocalPath() != null && filter.getCryptoRepoFileId() != null)
			throw new IllegalArgumentException("filter.localPath and filter.cryptoRepoFileId must not both be set! Exactly one of them!");

		if (filter.getLocalPath() != null) {
			filter = filter.clone(); // we modify it, hence we must clone
			if (filter.getCryptoRepoFileId() != null)
				throw new IllegalArgumentException("filter.localPath and filter.cryptoRepoFileId must not both be set! Exactly one of them!");

			final CryptoRepoFile filterCryptoRepoFile = getCryptoRepoFileForLocalPath(filter.getLocalPath());
			if (filterCryptoRepoFile == null)
				return null; // not yet uploaded, symlink, whatever reason => can be no collision, either - maybe we change the symlink-behaviour, later...

			filter.setLocalPath(null);
			filter.setCryptoRepoFileId(filterCryptoRepoFile.getCryptoRepoFileId());
		}
		return filter;
	}

	private static void appendToQueryFilter_collisionIds(final StringBuilder qf, final Map<String, Object> qp, final Map<String, Class<?>> qv, final Set<Uid> collisionIds) {
		assertNotNull(qf, "qf");
		assertNotNull(qp, "qp");
		if (collisionIds == null)
			return;

		final Set<String> collisionIdsAsString = new HashSet<>(collisionIds.size());
		for (Uid collisionId : collisionIds)
			collisionIdsAsString.add(collisionId.toString());

		appendAndIfNeeded(qf);
		qf.append(" :collisionIds.contains(this.collisionId) ");
		qp.put("collisionIds", collisionIdsAsString);
	}

	private static void appendToQueryFilter_histoCryptoRepoFileId(final StringBuilder qf, final Map<String, Object> qp, final Map<String, Class<?>> qv, final Uid histoCryptoRepoFileId) {
		assertNotNull(qf, "qf");
		assertNotNull(qp, "qp");
		if (histoCryptoRepoFileId == null)
			return;

		appendAndIfNeeded(qf);

		qf.append(" (")
		.append("  this.histoCryptoRepoFile1.histoCryptoRepoFileId == :histoCryptoRepoFileId")
		.append("  || this.histoCryptoRepoFile2.histoCryptoRepoFileId == :histoCryptoRepoFileId")
		.append(") ");

		qp.put("histoCryptoRepoFileId", histoCryptoRepoFileId.toString());
	}

	private static void appendToQueryFilter_cryptoRepoFileId_nonRecursive(final StringBuilder qf, final Map<String, Object> qp, final Map<String, Class<?>> qv, final Uid cryptoRepoFileId) {
		assertNotNull(qf, "qf");
		assertNotNull(qp, "qp");
		assertNotNull(qv, "qv");
		if (cryptoRepoFileId == null)
			return;

		appendAndIfNeeded(qf);

		qf.append(" (")
		.append("  this.histoCryptoRepoFile1.cryptoRepoFile.cryptoRepoFileId == :cryptoRepoFileId")
		.append("  || this.histoCryptoRepoFile2.cryptoRepoFile.cryptoRepoFileId == :cryptoRepoFileId")
		.append(") ");

		qp.put("cryptoRepoFileId", cryptoRepoFileId.toString());
	}

	private static void appendToQueryFilter_cryptoRepoFileId_recursive(final StringBuilder qf, final Map<String, Object> qp, final Map<String, Class<?>> qv, final Uid cryptoRepoFileId) {
		assertNotNull(qf, "qf");
		assertNotNull(qp, "qp");
		assertNotNull(qv, "qv");
		if (cryptoRepoFileId == null)
			return;

		appendAndIfNeeded(qf);

		qf.append(" ( this.cryptoRepoFilePath.contains(crf) && crf.cryptoRepoFileId == :cryptoRepoFileId ) ");

		qv.put("crf", CryptoRepoFile.class);
		qp.put("cryptoRepoFileId", cryptoRepoFileId.toString());
	}

	private CryptoRepoFile getCryptoRepoFileForLocalPath(final String localPath) {
		if (localPath == null)
			return null;

		final RemoteRepository remoteRepository = getDao(SsRemoteRepositoryDao.class).getUniqueRemoteRepository();
		if (remoteRepository == null)
			return null;

		final CryptoRepoFile crf1 = getDao(CryptoRepoFileDao.class).getCryptoRepoFile(remoteRepository, localPath);
		// crf1 might indeed be null, which is currently the case, if there is a symlink in the localPath.
		// Additionally, it might be null, because there was nothing uploaded, yet.
//		assertNotNull("cryptoRepoFile", crf1, "remoteRepository=%s filter.localPath='%s'", remoteRepository, localPath);

		return crf1;
	}

//	private void appendToQueryFilter_resolved(StringBuilder qf, Map<String, Object> qp, final Map<String, Class<?>> qv, Boolean resolved) {
//		assertNotNull("qf", qf);
//		assertNotNull("qp", qp);
//		assertNotNull("qv", qv);
//		if (resolved == null)
//			return;
//
//		appendAndIfNeeded(qf);
//
//		qf.append(" this.resolved ");
//
//		if (resolved == true)
//			qf.append(" != ");
//		else
//			qf.append(" == ");
//
//		qf.append("null ");
//	}

	private static void appendAndIfNeeded(final StringBuilder qf) {
		assertNotNull(qf, "qf");
		if (qf.length() > 0)
			qf.append(" && ");
	}
}
