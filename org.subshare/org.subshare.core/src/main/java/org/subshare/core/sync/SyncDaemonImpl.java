package org.subshare.core.sync;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.observable.ModificationEventType;
import org.subshare.core.observable.ObservableSet;
import org.subshare.core.observable.standard.StandardPostModificationEvent;
import org.subshare.core.observable.standard.StandardPostModificationListener;
import org.subshare.core.observable.standard.StandardPreModificationEvent;
import org.subshare.core.observable.standard.StandardPreModificationListener;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerRegistry;
import org.subshare.core.server.ServerRegistryImpl;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.dto.Error;

public abstract class SyncDaemonImpl implements SyncDaemon {

	private static final Logger logger = LoggerFactory.getLogger(SyncDaemonImpl.class);

	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	private final Map<Server, SyncState> server2State = Collections.synchronizedMap(new HashMap<Server, SyncState>());

	private Timer syncTimer;
	private TimerTask syncTimerTask;
	private volatile long syncPeriod;
	private final AtomicBoolean syncRunning = new AtomicBoolean();

	private ServerRegistry serverRegistry;

	protected abstract String getConfigKeySyncPeriod();

	protected abstract long getConfigDefaultValueSyncPeriod();

	private final ObservableSet<SyncState> states = ObservableSet.decorate(new CopyOnWriteArraySet<SyncState>());
	{
		states.getHandler().addPreModificationListener(new StandardPreModificationListener() {
			@Override
			public void modificationOccurring(StandardPreModificationEvent event) {
				@SuppressWarnings("unchecked")
				Collection<SyncState> changeCollection = event.getChangeCollection();

				if ((ModificationEventType.GROUP_ADD & event.getType()) != 0) {
					for (SyncState state : changeCollection) {
						final SyncState oldState = server2State.remove(state.getServer());
						if (oldState != null)
							states.remove(oldState);
					}
				}
			}
		});

		states.getHandler().addPostModificationListener(new StandardPostModificationListener() {
			@Override
			public void modificationOccurred(StandardPostModificationEvent event) {
				@SuppressWarnings("unchecked")
				Collection<SyncState> changeCollection = event.getChangeCollection();

				if ((ModificationEventType.GROUP_ADD & event.getType()) != 0) {
					for (SyncState state : changeCollection)
						server2State.put(state.getServer(), state);

					if (!changeCollection.isEmpty())
						firePropertyChange(PropertyEnum.states_added, null, changeCollection);
				}
				else if ((ModificationEventType.GROUP_REMOVE & event.getType()) != 0) {
					for (SyncState state : changeCollection) {
						final SyncState removed = server2State.remove(state.getServer());
						if (removed != null && removed != state.getServer())
							throw new IllegalStateException("removed != state.server");
					}

					if (!changeCollection.isEmpty())
						firePropertyChange(PropertyEnum.states_removed, null, changeCollection);
				}
				else if ((ModificationEventType.GROUP_CLEAR & event.getType()) != 0) {
					// save all states
					final List<SyncState> removed = new ArrayList<>(server2State.values());

					// *all* instead of (empty!) changeCollection
					server2State.clear();

					if (!removed.isEmpty())
						firePropertyChange(PropertyEnum.states_removed, null, removed);
				}
				else if ((ModificationEventType.GROUP_RETAIN & event.getType()) != 0) {
					final List<SyncState> removed = new ArrayList<>();
					for (SyncState state : states) {
						if (!changeCollection.contains(state)) { // IMHO changeCollection is the retained collection, i.e. all elements *not* contained there are removed.
							removed.add(state);
							server2State.remove(state.getServer());
						}
					}

					if (!removed.isEmpty())
						firePropertyChange(PropertyEnum.states_removed, null, removed);
				}

				firePropertyChange(PropertyEnum.states, null, getStates());
			}
		});
	}

	protected SyncDaemonImpl() {
		createSyncTimerTask(true);
		addWeakPropertyChangeListener(getServerRegistry(), serverRegistryPropertyChangeListener);
	}

	private final PropertyChangeListener serverRegistryPropertyChangeListener = new PropertyChangeListener() {
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

	@Override
	public void sync() {
		_sync();
		createSyncTimerTask(false); // always recreate, if manually invoked (to postpone the period).
	}

	private void _sync() {
		if (! syncRunning.compareAndSet(false, true))
			return;

		try {
			final Set<Server> oldServers = new HashSet<Server>(server2State.keySet());

			for (final Server server : getServerRegistry().getServers()) {
				oldServers.remove(server);
				try (final Sync sync = createSync(server);) {
					final Date syncStarted = new Date();
					try {
						final long startTimestamp = System.currentTimeMillis();
						sync.sync();

						final String message = String.format("Synchronizing with server '%s' (%s) took %d ms.",
								server.getName(), server.getUrl(), System.currentTimeMillis() - startTimestamp);

						getStates().add(new SyncState(server, server.getUrl(), Severity.INFO, message, null,
								syncStarted, new Date()));
					} catch (Exception x) {
						logger.error("_sync: " + x, x);
						getStates().add(new SyncState(server, server.getUrl(), Severity.ERROR, x.getLocalizedMessage(), new Error(x),
								syncStarted, new Date()));
					}
				}
			}

			final List<SyncState> oldStates = new ArrayList<>(oldServers.size());
			for (Server server : oldServers) {
				final SyncState state = server2State.remove(server);
				if (state != null)
					oldStates.add(state);
			}

			if (!oldStates.isEmpty())
				getStates().removeAll(oldStates);
		} catch (final Exception x) { // catch all exceptions to make sure the timer does not stop!
			logger.error("_sync: " + x, x);
		} finally {
			syncRunning.set(false);
		}
	}

	protected abstract Sync createSync(Server server);

	private synchronized void recreateSyncTimerTaskIfPeriodChanged() {
		final long syncPeriod = ConfigImpl.getInstance().getPropertyAsLong(getConfigKeySyncPeriod(), getConfigDefaultValueSyncPeriod());
		if (this.syncPeriod != syncPeriod) {
			destroySyncTimerTask();
			if (! createSyncTimerTask(false))
				destroySyncTimer();
		}
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

	private synchronized Timer getSyncTimer() {
		if (syncTimer == null)
			syncTimer = new Timer(true);

		return syncTimer;
	}


	protected ServerRegistry getServerRegistry() {
		if (serverRegistry == null)
			serverRegistry = ServerRegistryImpl.getInstance();

		return serverRegistry;
	}

	@Override
	public Set<SyncState> getStates() {
		return states;
	}

	@Override
	public SyncState getState(final Server server) {
		assertNotNull("server", server);
		return server2State.get(server);
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
	}

	protected void firePropertyChange(Property property, Object oldValue, Object newValue) {
		propertyChangeSupport.firePropertyChange(property.name(), oldValue, newValue);
	}
}
