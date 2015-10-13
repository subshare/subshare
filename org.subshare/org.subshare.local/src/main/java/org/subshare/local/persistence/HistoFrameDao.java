package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;
import java.util.UUID;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;

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
}
