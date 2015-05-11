package org.subshare.core.pgp.sync;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArraySet;

import org.subshare.core.Severity;
import org.subshare.core.observable.ModificationEventType;
import org.subshare.core.observable.ObservableSet;
import org.subshare.core.observable.standard.StandardPostModificationEvent;
import org.subshare.core.observable.standard.StandardPostModificationListener;
import org.subshare.core.observable.standard.StandardPreModificationEvent;
import org.subshare.core.observable.standard.StandardPreModificationListener;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerRegistry;
import org.subshare.core.server.ServerRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.dto.Error;

public class PgpSyncDaemonImpl implements PgpSyncDaemon {

	private static final Logger logger = LoggerFactory.getLogger(PgpSyncDaemonImpl.class);

	public static final String CONFIG_KEY_PGP_SYNC_PERIOD = "pgpSyncPeriod";
	public static final long CONFIG_DEFAULT_VALUE_PGP_SYNC_PERIOD = 3600 * 1000; // 1 hour

	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	private final Map<Server, PgpSyncState> server2State = Collections.synchronizedMap(new HashMap<Server, PgpSyncState>());

	private Timer pgpSyncTimer;
	private TimerTask pgpSyncTimerTask;
	private volatile long pgpSyncPeriod;

	private ServerRegistry serverRegistry;

	private final ObservableSet<PgpSyncState> states = ObservableSet.decorate(new CopyOnWriteArraySet<PgpSyncState>());
	{
		states.getHandler().addPreModificationListener(new StandardPreModificationListener() {
			@Override
			public void modificationOccurring(StandardPreModificationEvent event) {
				@SuppressWarnings("unchecked")
				Collection<PgpSyncState> changeCollection = event.getChangeCollection();

				if ((ModificationEventType.GROUP_ADD & event.getType()) != 0) {
					for (PgpSyncState state : changeCollection) {
						final PgpSyncState oldState = server2State.remove(state.getServer());
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
				Collection<PgpSyncState> changeCollection = event.getChangeCollection();

				if ((ModificationEventType.GROUP_ADD & event.getType()) != 0) {
					for (PgpSyncState state : changeCollection)
						server2State.put(state.getServer(), state);

					if (!changeCollection.isEmpty())
						firePropertyChange(PropertyEnum.states_added, null, changeCollection);
				}
				else if ((ModificationEventType.GROUP_REMOVE & event.getType()) != 0) {
					for (PgpSyncState state : changeCollection) {
						final PgpSyncState removed = server2State.remove(state.getServer());
						if (removed != null && removed != state.getServer())
							throw new IllegalStateException("removed != state.server");
					}

					if (!changeCollection.isEmpty())
						firePropertyChange(PropertyEnum.states_removed, null, changeCollection);
				}
				else if ((ModificationEventType.GROUP_CLEAR & event.getType()) != 0) {
					// save all states
					final List<PgpSyncState> removed = new ArrayList<>(server2State.values());

					// *all* instead of (empty!) changeCollection
					server2State.clear();

					if (!removed.isEmpty())
						firePropertyChange(PropertyEnum.states_removed, null, removed);
				}
				else if ((ModificationEventType.GROUP_RETAIN & event.getType()) != 0) {
					final List<PgpSyncState> removed = new ArrayList<>();
					for (PgpSyncState state : states) {
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

	private static final class Holder {
		public static final PgpSyncDaemonImpl instance = new PgpSyncDaemonImpl();
	}

	protected PgpSyncDaemonImpl() {
		createPgpSyncTimerTask();

		getServerRegistry().addPropertyChangeListener(serverRegistryPropertyChangeListener);
	}

	private final PropertyChangeListener serverRegistryPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			getPgpSyncTimer().schedule(new TimerTask() {
				@Override
				public void run() {
					_sync();
				}
			}, 0L);
		}
	};

	@Override
	protected void finalize() throws Throwable {
		final ServerRegistry sr = this.serverRegistry;
		this.serverRegistry = null;
		if (sr != null)
			sr.removePropertyChangeListener(serverRegistryPropertyChangeListener);

		super.finalize();
	}


	public void sync() {
		_sync();
		createPgpSyncTimerTask(); // always recreate, if manually invoked (to postpone the period).
	}

	private void _sync() {
		try {
			final Set<Server> oldServers = new HashSet<Server>(server2State.keySet());

			for (final Server server : getServerRegistry().getServers()) {
				oldServers.remove(server);
				try (final PgpSync pgpSync = new PgpSync(server);) {
					try {
						final long startTimestamp = System.currentTimeMillis();
						pgpSync.sync();

						final String message = String.format("Synchronizing PGP keys with server '%s' (%s) took %d ms.",
								server.getName(), pgpSync.getServerUrl(), System.currentTimeMillis() - startTimestamp);

						getStates().add(new PgpSyncState(server, pgpSync.getServerUrl(), Severity.INFO, message, null));
					} catch (Exception x) {
						logger.error("_sync: " + x, x);
						getStates().add(new PgpSyncState(server, pgpSync.getServerUrl(), Severity.ERROR, x.getLocalizedMessage(), new Error(x)));
					}
				}
			}

			final List<PgpSyncState> oldStates = new ArrayList<>(oldServers.size());
			for (Server server : oldServers) {
				final PgpSyncState state = server2State.remove(server);
				if (state != null)
					oldStates.add(state);
			}

			if (!oldStates.isEmpty())
				getStates().removeAll(oldStates);
		} catch (final Exception x) { // catch all exceptions to make sure the timer does not stop!
			logger.error("_sync: " + x, x);
		}
	}

	private synchronized void recreatePgpSyncTimerTaskIfPeriodChanged() {
		final long pgpSyncPeriod = Config.getInstance().getPropertyAsLong(CONFIG_KEY_PGP_SYNC_PERIOD, CONFIG_DEFAULT_VALUE_PGP_SYNC_PERIOD);
		if (this.pgpSyncPeriod != pgpSyncPeriod) {
			destroyPgpSyncTimerTask();
			if (! createPgpSyncTimerTask())
				destroyPgpSyncTimer();
		}
	}

	private synchronized void destroyPgpSyncTimer() {
		if (pgpSyncTimer != null) {
			pgpSyncTimer.cancel();
			pgpSyncTimer = null;
		}
	}

	private synchronized void destroyPgpSyncTimerTask() {
		if (pgpSyncTimerTask != null) {
			pgpSyncTimerTask.cancel();
			pgpSyncTimerTask = null;
		}
	}

	private synchronized boolean createPgpSyncTimerTask() {
		destroyPgpSyncTimerTask(); // just in case

		final long pgpSyncPeriod = Config.getInstance().getPropertyAsLong(CONFIG_KEY_PGP_SYNC_PERIOD, CONFIG_DEFAULT_VALUE_PGP_SYNC_PERIOD);
		this.pgpSyncPeriod = pgpSyncPeriod;
		if (pgpSyncPeriod <= 0)
			return false;

		pgpSyncTimerTask = new TimerTask() {
			@Override
			public void run() {
				_sync();
				recreatePgpSyncTimerTaskIfPeriodChanged();
			}
		};

		getPgpSyncTimer().schedule(pgpSyncTimerTask, 1000L, pgpSyncPeriod);
		return true;
	}

	private synchronized Timer getPgpSyncTimer() {
		if (pgpSyncTimer == null)
			pgpSyncTimer = new Timer(true);

		return pgpSyncTimer;
	}


	protected ServerRegistry getServerRegistry() {
		if (serverRegistry == null)
			serverRegistry = ServerRegistryImpl.getInstance();

		return serverRegistry;
	}

	public static PgpSyncDaemon getInstance() {
		return Holder.instance;
	}

	@Override
	public Set<PgpSyncState> getStates() {
		return states;
	}

	@Override
	public PgpSyncState getState(final Server server) {
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
