package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.repo.local.HistoFrameFilter;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.util.CollectionUtil;
import co.codewizards.cloudstore.local.persistence.Dao;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;

public class HistoFrameDao extends Dao<HistoFrame, HistoFrameDao> {

	private static final Logger logger = LoggerFactory.getLogger(HistoFrameDao.class);

	public Collection<HistoFrame> getHistoFramesChangedAfter(final long localRevision) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getHistoFramesChangedAfter_localRevision");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<HistoFrame> result = (Collection<HistoFrame>) query.execute(localRevision);
			logger.debug("getHistoFramesChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getHistoFramesChangedAfter: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public HistoFrame getHistoFrameOrFail(final Uid histoFrameId) {
		final HistoFrame histoFrame = getHistoFrame(histoFrameId);
		if (histoFrame == null)
			throw new IllegalArgumentException(String.format("There is no HistoFrame with histoFrameId='%s'!", histoFrameId));

		return histoFrame;
	}

	public HistoFrame getHistoFrame(final Uid histoFrameId) {
		assertNotNull("histoFrameId", histoFrameId);
		final Query query = pm().newNamedQuery(getEntityClass(), "getHistoFrame_histoFrameId");
		try {
			final HistoFrame result = (HistoFrame) query.execute(histoFrameId.toString());
			return result;
		} finally {
			query.closeAll();
		}
	}

	@Override
	public void deletePersistent(HistoFrame entity) {
		super.deletePersistent(entity);
	}

	@Override
	public void deletePersistentAll(Collection<? extends HistoFrame> entities) {
		super.deletePersistentAll(entities);
	}

	public HistoFrame getUnsealedHistoFrameOrFail(final UUID fromRepositoryId) {
		final HistoFrame histoFrame = getUnsealedHistoFrame(fromRepositoryId);
		if (histoFrame == null)
			throw new IllegalStateException(String.format("There is no unsealed HistoFrame with fromRepositoryId='%s'!", fromRepositoryId));

		return histoFrame;
	}

	public HistoFrame getUnsealedHistoFrame(final UUID fromRepositoryId) {
		assertNotNull("fromRepositoryId", fromRepositoryId);
		final Query query = pm().newNamedQuery(getEntityClass(), "getHistoFrame_fromRepositoryId_sealed");
		try {
			final HistoFrame result = (HistoFrame) query.execute(fromRepositoryId.toString(), null);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<HistoFrame> getHistoFrames(final HistoFrameFilter filter) {
		assertNotNull("filter", filter);

		if ("/".equals(filter.getLocalPath())) // the root is normally simply "", but we are tolerant to "/".
			filter.setLocalPath("");

		final Set<HistoFrame> result = new HashSet<>();

		if (! isEmpty(filter.getLocalPath())) { // null means no filtering and "" means localPath is root (/), i.e. no filter either.
			final RemoteRepository remoteRepository = getDao(SsRemoteRepositoryDao.class).getUniqueRemoteRepositoryOrFail();
			final CryptoRepoFileDao crfDao = getDao(CryptoRepoFileDao.class);
			final CryptoRepoFile crf1 = crfDao.getCryptoRepoFile(remoteRepository, filter.getLocalPath());
			assertNotNull("cryptoRepoFile", crf1, "remoteRepository=%s filter.localPath='%s'", remoteRepository, filter.getLocalPath());

			final Set<Long> childCryptoRepoFileOids = crfDao.getChildCryptoRepoFileOidsRecursively(crf1);
			for (final Set<Long> partialChildCryptoRepoFileOids : CollectionUtil.splitSet(childCryptoRepoFileOids, 1000))
				populateHistoFrames(result, filter, partialChildCryptoRepoFileOids);
		}
		else
			populateHistoFrames(result, filter, null);

		return result;
	}


	private void populateHistoFrames(final Set<HistoFrame> result, final HistoFrameFilter filter, final Set<Long> childCryptoRepoFileOids) {
		assertNotNull("result", result);
		assertNotNull("filter", filter);
//		childCryptoRepoFileOids may be null!

		final Query query = pm().newQuery(getEntityClass());
		try {
			final StringBuilder q = new StringBuilder();
			final Map<String, Object> p = new HashMap<>();
			final Map<String, Class<?>> v = new HashMap<>();

			if (filter.getMaxResultSize() > 0) {
				query.setRange(0, filter.getMaxResultSize());
				query.setOrdering("this.signature.signatureCreated DESC");
			}

			if (filter.getSignatureCreatedFrom() != null) {
				if (q.length() > 0)
					q.append(" && ");

				q.append("this.signature.signatureCreated >= :signatureCreatedFrom");
				p.put("signatureCreatedFrom", filter.getSignatureCreatedFrom());
			}

			if (filter.getSignatureCreatedTo() != null) {
				if (q.length() > 0)
					q.append(" && ");

				q.append("this.signature.signatureCreated < :signatureCreatedTo");
				p.put("signatureCreatedTo", filter.getSignatureCreatedTo());
			}

			if (childCryptoRepoFileOids != null) {
				if (q.length() > 0)
					q.append(" && ");

				q.append("this == hcrf.histoFrame && :crfOids.contains(hcrf.cryptoRepoFile.id)");
				p.put("crfOids", childCryptoRepoFileOids);
				v.put("hcrf", HistoCryptoRepoFile.class);
			}

			if (! v.isEmpty()) {
				StringBuilder sb = new StringBuilder();
				for (Map.Entry<String, Class<?>> me : v.entrySet()) {
					sb.append(me.getValue().getName()).append(' ').append(me.getKey()).append(';');
				}
				query.declareVariables(sb.toString());
			}

			if (q.length() > 0)
				query.setFilter(q.toString());

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<HistoFrame> r = (List<HistoFrame>) query.executeWithMap(p);
			logger.debug("getHistoFrames: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			r = load(r);
			logger.debug("getHistoFrames: Loading result-set with {} elements took {} ms.", r.size(), System.currentTimeMillis() - startTimestamp);

			result.addAll(r);
		} finally {
			query.closeAll();
		}
	}



}
