package org.subshare.core.repo.metaonly;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.repo.ServerRepoRegistry;
import org.subshare.core.repo.ServerRepoRegistryImpl;

import co.codewizards.cloudstore.core.config.ConfigImpl;


public class MetaOnlyRepoSyncDaemonImpl implements MetaOnlyRepoSyncDaemon {

	public static final String CONFIG_KEY_READ_ONLY_META_REPO_SYNC_PERIOD = "readOnlyMetaRepoSyncPeriod";
//	public static final long CONFIG_DEFAULT_VALUE_READ_ONLY_META_REPO_SYNC_PERIOD = 3600 * 1000; // 1 hour
	public static final long CONFIG_DEFAULT_VALUE_READ_ONLY_META_REPO_SYNC_PERIOD = 5 * 60 * 1000; // 5 minutes

	private static final Logger logger = LoggerFactory.getLogger(MetaOnlyRepoSyncDaemonImpl.class);

	private Timer syncTimer;
	private TimerTask syncTimerTask;
	private volatile long syncPeriod;
	private final AtomicBoolean syncRunning = new AtomicBoolean();

	private ServerRepoRegistry serverRepoRegistry;

	private MetaOnlyRepoSyncDaemonImpl() {
		createSyncTimerTask(true);
		addWeakPropertyChangeListener(getServerRepoRegistry(), serverRepoRegistryPropertyChangeListener);
	}

	private static final class Holder {
		public static final MetaOnlyRepoSyncDaemonImpl instance = new MetaOnlyRepoSyncDaemonImpl();
	}

	public static MetaOnlyRepoSyncDaemon getInstance() {
		return Holder.instance;
	}

	private final PropertyChangeListener serverRepoRegistryPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			getSyncTimer().schedule(new TimerTask() {
				@Override
				public void run() {
					_sync();
				}
			}, 0L);
		}
	};

	private synchronized boolean createSyncTimerTask(final boolean runImmediately) {
		destroySyncTimerTask(); // just in case

		final long syncPeriod = ConfigImpl.getInstance().getPropertyAsLong(getConfigKeySyncPeriod(), getConfigDefaultValueSyncPeriod());
		this.syncPeriod = syncPeriod;
		if (syncPeriod <= 0)
			return false;

		syncTimerTask = new TimerTask() {
			@Override
			public void run() {
				_sync();
				recreateSyncTimerTaskIfPeriodChanged();
			}
		};

		final long delay = runImmediately ? 500L : syncPeriod;
		getSyncTimer().schedule(syncTimerTask, delay, syncPeriod);
		return true;
	}

	private synchronized void recreateSyncTimerTaskIfPeriodChanged() {
		final long syncPeriod = ConfigImpl.getInstance().getPropertyAsLong(getConfigKeySyncPeriod(), getConfigDefaultValueSyncPeriod());
		if (this.syncPeriod != syncPeriod) {
			destroySyncTimerTask();
			if (! createSyncTimerTask(false))
				destroySyncTimer();
		}
	}

	@Override
	public void sync() {
		_sync();
		createSyncTimerTask(false); // always recreate, if manually invoked (to postpone the period).
	}

	private void _sync() {
		if (! syncRunning.compareAndSet(false, true))
			return;

		try {
			MetaOnlyRepoManagerImpl.getInstance().sync(); // TODO store state somewhere - either here or in manager.
		} catch (final Exception x) { // catch all exceptions to make sure the timer does not stop!
			logger.error("_sync: " + x, x);
		} finally {
			syncRunning.set(false);
		}
	}


	private synchronized Timer getSyncTimer() {
		if (syncTimer == null)
			syncTimer = new Timer(true); // it's ok to interrupt at any time => daemon is fine.

		return syncTimer;
	}

	private synchronized void destroySyncTimer() {
		if (syncTimer != null) {
			syncTimer.cancel();
			syncTimer = null;
		}
	}

	private synchronized void destroySyncTimerTask() {
		if (syncTimerTask != null) {
			syncTimerTask.cancel();
			syncTimerTask = null;
		}
	}

	protected ServerRepoRegistry getServerRepoRegistry() {
		if (serverRepoRegistry == null)
			serverRepoRegistry = ServerRepoRegistryImpl.getInstance();

		return serverRepoRegistry;
	}

	protected String getConfigKeySyncPeriod() {
		return CONFIG_KEY_READ_ONLY_META_REPO_SYNC_PERIOD;
	}

	protected long getConfigDefaultValueSyncPeriod() {
		return CONFIG_DEFAULT_VALUE_READ_ONLY_META_REPO_SYNC_PERIOD;
	}

}
