package org.subshare.core.repo.listener;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;

public class LocalRepoCommitEventManagerImpl implements LocalRepoCommitEventManager {

	private static final class Holder {
		public static final LocalRepoCommitEventManagerImpl instance = new LocalRepoCommitEventManagerImpl();
	}

	private final Map<UUID, ExecutorService> localRepositoryId2ExecutorService = new HashMap<>();

	private Map<UUID, List<LocalRepoCommitEventListener>> localRepositoryId2Listeners = new HashMap<>();

	protected LocalRepoCommitEventManagerImpl() {
	}

	public static LocalRepoCommitEventManager getInstance() {
		return Holder.instance;
	}

	/**
	 * Fires a {@link LocalRepoCommitEvent}.
	 * <p>
	 * Though the listeners are notified asynchronously, it is guaranteed that they are notified in the
	 * same order of this method being invoked. However, this guarantee only holds for the same local-repository.
	 * Events for different local-repositories are fired on separate, non-synchronized threads.
	 * <p>
	 * API consumers must not invoke this method! This method is invoked internally!
	 * @param localRepoManager the manager responsible for the repo that was just modified. Must not be <code>null</code>.
	 * @param modifications the individual modifications performed in the currently committed transaction.
	 * Must not be <code>null</code>.
	 */
	public void fireLater(final LocalRepoManager localRepoManager, final List<EntityModification> modifications) {
		assertNotNull("localRepoManager", localRepoManager);
		assertNotNull("modifications", modifications);
		final UUID localRepositoryId = localRepoManager.getRepositoryId();

		final Iterator<LocalRepoCommitEventListener> globalListenerIterator = getListeners(null).iterator();
		final Iterator<LocalRepoCommitEventListener> specificListenerIterator = getListeners(localRepositoryId).iterator();

		if (! (globalListenerIterator.hasNext() || specificListenerIterator.hasNext()))
			return;

		final LocalRepoCommitEvent event = new LocalRepoCommitEvent(this, localRepoManager, modifications);
		getExecutorService(localRepositoryId).submit(new Runnable() {
			@Override
			public void run() {
				while (specificListenerIterator.hasNext()) {
					final LocalRepoCommitEventListener listener = specificListenerIterator.next();
					if (listener != null)
						listener.postCommit(event);
				}

				while (globalListenerIterator.hasNext()) {
					final LocalRepoCommitEventListener listener = globalListenerIterator.next();
					if (listener != null)
						listener.postCommit(event);
				}
			}
		});
	}

	@Override
	public void addLocalRepoCommitEventListener(final LocalRepoCommitEventListener listener) {
		addLocalRepoCommitEventListener(null, listener);
	}

	@Override
	public void addLocalRepoCommitEventListener(final UUID localRepositoryId, final LocalRepoCommitEventListener listener) {
		assertNotNull("listener", listener);
		getListeners(localRepositoryId).add(listener);
	}

	@Override
	public void removeLocalRepoCommitEventListener(final LocalRepoCommitEventListener listener) {
		removeLocalRepoCommitEventListener(null, listener);
	}

	@Override
	public void removeLocalRepoCommitEventListener(final UUID localRepositoryId, LocalRepoCommitEventListener listener) {
		assertNotNull("listener", listener);
		getListeners(localRepositoryId).remove(listener);
	}

	private List<LocalRepoCommitEventListener> getListeners(final UUID localRepositoryId) {
		// localRepositoryId may be null!
		synchronized (localRepositoryId2Listeners) {
			List<LocalRepoCommitEventListener> listeners = localRepositoryId2Listeners.get(localRepositoryId);
			if (listeners == null) {
				listeners = new CopyOnWriteArrayList<>();
				localRepositoryId2Listeners.put(localRepositoryId, listeners);
			}
			return listeners;
		}
	}

	/**
	 * Gets a separate single-thread-executor for each distinct {@code localRepositoryId} to ensure
	 * that the events for one repository are processed in the order in which they are fired.
	 * @param localRepositoryId the local repository's ID. Never <code>null</code>.
	 * @return the single-thread-executor for the given {@code localRepositoryId}. Never <code>null</code>.
	 */
	private ExecutorService getExecutorService(final UUID localRepositoryId) {
		assertNotNull("localRepositoryId", localRepositoryId);
		synchronized (localRepositoryId2ExecutorService) {
			ExecutorService executorService = localRepositoryId2ExecutorService.get(localRepositoryId);
			if (executorService == null) {
				executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
					@Override
					public Thread newThread(final Runnable r) {
						return new Thread(r, "LocalRepoCommitEventManager_" + localRepositoryId);
					}
				});
			}
			return executorService;
		}
	}
}