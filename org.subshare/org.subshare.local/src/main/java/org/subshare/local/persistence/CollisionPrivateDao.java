package org.subshare.local.persistence;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jdo.JDOHelper;
import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.repo.local.CollisionPrivateFilter;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;

public class CollisionPrivateDao extends Dao<CollisionPrivate, CollisionPrivateDao> {

	private static final Logger logger = LoggerFactory.getLogger(CollisionPrivateDao.class);

	public CollisionPrivate getCollisionPrivate(final Collision collision) {
		requireNonNull(collision, "collision");
// We *must* check before, whether it is already persisted, because we otherwise run into this exception since DN 5:
//			java.lang.NumberFormatException: For input string: "AUTOINCREMENT: start 1 increment 1"
//				at java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:65)
//				at java.base/java.lang.Long.parseLong(Long.java:692)
//				at java.base/java.lang.Long.parseLong(Long.java:817)
//				at org.datanucleus.store.rdbms.mapping.column.BigIntColumnMapping.setObject(BigIntColumnMapping.java:185)
//				at org.datanucleus.store.rdbms.mapping.java.SingleFieldMapping.setObject(SingleFieldMapping.java:190)
//				at org.datanucleus.store.rdbms.mapping.java.PersistableMapping.setObjectAsNull(PersistableMapping.java:391)
//				at org.datanucleus.store.rdbms.mapping.java.PersistableMapping.setObject(PersistableMapping.java:362)
//				at org.datanucleus.store.rdbms.mapping.java.PersistableMapping.setObject(PersistableMapping.java:345)
//				at org.datanucleus.store.rdbms.sql.SQLStatementHelper.applyParametersToStatement(SQLStatementHelper.java:290)
//				at org.datanucleus.store.rdbms.query.JDOQLQuery.performExecute(JDOQLQuery.java:630)
//				at org.datanucleus.store.query.Query.executeQuery(Query.java:1973)
//				at org.datanucleus.store.query.Query.executeWithArray(Query.java:1862)
//				at org.datanucleus.api.jdo.JDOQuery.executeInternal(JDOQuery.java:433)
//				at org.datanucleus.api.jdo.JDOQuery.execute(JDOQuery.java:276)
//				at org.subshare.local.persistence.CollisionPrivateDao.getCollisionPrivate(CollisionPrivateDao.java:29)
//				at org.subshare.local.CryptreeNode.updateCollisionPrivate(CryptreeNode.java:616)
//				at org.subshare.local.CryptreeNode.putCollisionPrivateDto(CryptreeNode.java:560)
//				at org.subshare.local.CryptreeNode.createCollisionIfNeeded(CryptreeNode.java:531)
		if (collision.getId() == Long.MIN_VALUE) {
			return null;
		}
		final Query query = pm().newNamedQuery(getEntityClass(), "getCollisionPrivate_collision");
		try {
			final CollisionPrivate result = (CollisionPrivate) query.execute(collision);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public CollisionPrivate getCollisionPrivateOrFail(final Collision collision) {
		requireNonNull(collision, "collision");
		final CollisionPrivate collisionPrivate = getCollisionPrivate(collision);
		if (collisionPrivate == null)
			throw new IllegalArgumentException("There is no CollisionPrivate for " + collision);

		return collisionPrivate;
	}

	public Collection<CollisionPrivate> getCollisionPrivates(CollisionPrivateFilter filter) {
		requireNonNull(filter, "filter");

		filter = prepareFilter(filter);
		if (filter == null)
			return new HashSet<>();

		final Query query = pm().newQuery(CollisionPrivate.class);
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

			appendToQueryFilter_resolved(qf, qp, qv, filter.getResolved());

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
			Collection<CollisionPrivate> result = (Collection<CollisionPrivate>) query.executeWithMap(qp);
			logger.info("getCollisionPrivates: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.info("getCollisionPrivates: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	private CollisionPrivateFilter prepareFilter(CollisionPrivateFilter filter) {
		requireNonNull(filter, "filter");

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
		requireNonNull(qf, "qf");
		requireNonNull(qp, "qp");
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
		requireNonNull(qf, "qf");
		requireNonNull(qp, "qp");
		if (histoCryptoRepoFileId == null)
			return;

		appendAndIfNeeded(qf);

		qf.append(" (")
		.append("  this.collision.histoCryptoRepoFile1.histoCryptoRepoFileId == :histoCryptoRepoFileId")
		.append("  || this.collision.histoCryptoRepoFile2.histoCryptoRepoFileId == :histoCryptoRepoFileId")
		.append(") ");

		qp.put("histoCryptoRepoFileId", histoCryptoRepoFileId.toString());
	}

	private static void appendToQueryFilter_cryptoRepoFileId_nonRecursive(final StringBuilder qf, final Map<String, Object> qp, final Map<String, Class<?>> qv, final Uid cryptoRepoFileId) {
		requireNonNull(qf, "qf");
		requireNonNull(qp, "qp");
		requireNonNull(qv, "qv");
		if (cryptoRepoFileId == null)
			return;

		appendAndIfNeeded(qf);

		qf.append(" (")
		.append("  this.collision.histoCryptoRepoFile1.cryptoRepoFile.cryptoRepoFileId == :cryptoRepoFileId")
		.append("  || this.collision.histoCryptoRepoFile2.cryptoRepoFile.cryptoRepoFileId == :cryptoRepoFileId")
		.append(") ");

		qp.put("cryptoRepoFileId", cryptoRepoFileId.toString());
	}

	private static void appendToQueryFilter_cryptoRepoFileId_recursive(final StringBuilder qf, final Map<String, Object> qp, final Map<String, Class<?>> qv, final Uid cryptoRepoFileId) {
		requireNonNull(qf, "qf");
		requireNonNull(qp, "qp");
		requireNonNull(qv, "qv");
		if (cryptoRepoFileId == null)
			return;

		appendAndIfNeeded(qf);

		qf.append(" ( this.collision.cryptoRepoFilePath.contains(crf) && crf.cryptoRepoFileId == :cryptoRepoFileId ) ");

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
//		requireNonNull("cryptoRepoFile", crf1, "remoteRepository=%s filter.localPath='%s'", remoteRepository, localPath);

		return crf1;
	}

	private void appendToQueryFilter_resolved(StringBuilder qf, Map<String, Object> qp, final Map<String, Class<?>> qv, Boolean resolved) {
		requireNonNull(qf, "qf");
		requireNonNull(qp, "qp");
		requireNonNull(qv, "qv");
		if (resolved == null)
			return;

		appendAndIfNeeded(qf);

		qf.append(" this.resolved ");

		if (resolved == true)
			qf.append(" != ");
		else
			qf.append(" == ");

		qf.append("null ");
	}

	private static void appendAndIfNeeded(final StringBuilder qf) {
		requireNonNull(qf, "qf");
		if (qf.length() > 0)
			qf.append(" && ");
	}
}
