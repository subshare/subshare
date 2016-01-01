package org.subshare.core.repo.sync;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.LocalRepoRegistryImpl;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncActivity;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemonImpl;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncState;

public class RepoSyncTimerImpl implements RepoSyncTimer {

	private static final Logger logger = LoggerFactory.getLogger(RepoSyncTimerImpl.class);

	private final Timer timer = new Timer("RepoSyncTimer");
	private TimerTask timerTask;
	private final Map<UUID, Long> localRepositoryId2NextSyncTimestamp = new HashMap<>();

	private static class Holder {
		public static final RepoSyncTimer instance = new RepoSyncTimerImpl();
	}

	private final PropertyChangeListener repoSyncDaemonStatesPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			scheduleTimerTask();
		}
	};

	protected RepoSyncTimerImpl() {
//		addWeakPropertyChangeListener(getRepoSyncDaemon(), RepoSyncDaemon.PropertyEnum.states, repoSyncDaemonStatesPropertyChangeListener);
		// The above line does not work with Java 7 :-( seems to be a bug in the handling of generics - in Java 8 it works fine.
		// Switched to the non-type-safe version below - temporarily.
		addWeakPropertyChangeListenerNonTypeSafe(getRepoSyncDaemon(), RepoSyncDaemon.PropertyEnum.states, repoSyncDaemonStatesPropertyChangeListener);

		scheduleTimerTask();
	}

	public static RepoSyncTimer getInstance() {
		return Holder.instance;
	}

	private void scheduleTimerTask() {
		cancelTimerTask(); // just in case, there's one scheduled.

		long globalNextSyncTimestamp = Long.MAX_VALUE;

		for (final LocalRepo localRepo : LocalRepoRegistryImpl.getInstance().getLocalRepos()) {
			final long nextSyncTimestamp = calculateNextSyncTimestamp(localRepo);
			globalNextSyncTimestamp = Math.min(nextSyncTimestamp, globalNextSyncTimestamp);
		}

		synchronized (this) {
			cancelTimerTask(); // we must ensure there's none, yet, before we (re)assign one - in the same synchronized-block.
			timerTask = new TimerTask() {
				@Override
				public void run() { onTimerTaskRun(); }
			};
			timer.schedule(timerTask, new Date(globalNextSyncTimestamp));
		}
	}

	private void onTimerTaskRun() {
		startSyncs();
		scheduleTimerTask();
	}

	private synchronized void cancelTimerTask() {
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}
	}

	private void startSyncs() {
		for (final LocalRepo localRepo : LocalRepoRegistryImpl.getInstance().getLocalRepos()) {
			final long nextSyncTimestamp = calculateNextSyncTimestamp(localRepo);
			if (nextSyncTimestamp <= System.currentTimeMillis())
				getRepoSyncDaemon().startSync(localRepo.getLocalRoot());
		}
	}

	private long calculateNextSyncTimestamp(final LocalRepo localRepo) {
		assertNotNull("localRepo", localRepo);
		final long now = System.currentTimeMillis();

		final long lastSyncTimestamp;
		if (isSyncQueuedOrInProgress(localRepo))
			lastSyncTimestamp = now;
		else
			lastSyncTimestamp = getLastSyncTimestamp(localRepo);

		final long syncPeriod = getSyncPeriod(localRepo);
		final long nextSyncTimestamp = Math.max(lastSyncTimestamp + syncPeriod, now);
		setNextSyncTimestamp(localRepo, nextSyncTimestamp);
		return nextSyncTimestamp;
	}

	private long getLastSyncTimestamp(final LocalRepo localRepo) {
		final UUID localRepositoryId = assertNotNull("localRepo", localRepo).getRepositoryId();
		final List<RepoSyncState> states = getRepoSyncDaemon().getStates(localRepositoryId);

		long result = 0L;
		for (RepoSyncState repoSyncState : states) {
			final Date syncFinished = repoSyncState.getSyncFinished();
			if (syncFinished.getTime() > result)
				result = syncFinished.getTime();
		}
		return result;
	}

	private boolean isSyncQueuedOrInProgress(final LocalRepo localRepo) {
		final UUID localRepositoryId = assertNotNull("localRepo", localRepo).getRepositoryId();

		final Set<RepoSyncActivity> activities = getRepoSyncDaemon().getActivities(localRepositoryId);
		for (final RepoSyncActivity activity : activities) {
			switch (activity.getActivityType()) {
				case IN_PROGRESS:
				case QUEUED:
					return true;
				default:
					final Exception x = new Exception("Unknown RepoSyncActivityType: " + activity.getActivityType());
					logger.warn("getLastSyncTimestamp: " + x, x);
					break;
			}
		}
		return false;
	}

	public synchronized long getNextSyncTimestamp(final LocalRepo localRepo) {
		final UUID localRepositoryId = assertNotNull("localRepo", localRepo).getRepositoryId();
		final Long result = localRepositoryId2NextSyncTimestamp.get(localRepositoryId);
		return result == null ? 0L : result;
	}

	public synchronized void setNextSyncTimestamp(final LocalRepo localRepo, long lastSyncTimestamp) {
		final UUID localRepositoryId = assertNotNull("localRepo", localRepo).getRepositoryId();
		localRepositoryId2NextSyncTimestamp.put(localRepositoryId, lastSyncTimestamp);
	}

	private long getSyncPeriod(final LocalRepo localRepo) {
		final Config config = ConfigImpl.getInstanceForDirectory(assertNotNull("localRepo", localRepo).getLocalRoot());
		final long syncPeriod = config.getPropertyAsPositiveOrZeroLong(CONFIG_KEY_SYNC_PERIOD, DEFAULT_SYNC_PERIOD);
		return syncPeriod;
	}

	private RepoSyncDaemon getRepoSyncDaemon() {
		return RepoSyncDaemonImpl.getInstance();
	}
}
