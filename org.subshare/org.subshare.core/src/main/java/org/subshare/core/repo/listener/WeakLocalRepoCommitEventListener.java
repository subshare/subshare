package org.subshare.core.repo.listener;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

public class WeakLocalRepoCommitEventListener implements LocalRepoCommitEventListener {

	private static ReferenceQueue<LocalRepoCommitEventListener> listenerRefQueue = new ReferenceQueue<LocalRepoCommitEventListener>();
	private static Map<Reference<LocalRepoCommitEventListener>, WeakLocalRepoCommitEventListener> listenerRef2WeakPropertyChangeListener =
			Collections.synchronizedMap(new IdentityHashMap<Reference<LocalRepoCommitEventListener>, WeakLocalRepoCommitEventListener>());

	private final LocalRepoCommitEventManager manager;
	private final UUID localRepositoryId;
	private final WeakReference<LocalRepoCommitEventListener> listenerRef;
	private boolean registered;

	public WeakLocalRepoCommitEventListener(final LocalRepoCommitEventManager manager, final LocalRepoCommitEventListener listener) {
		this(manager, null, listener);
	}

	public WeakLocalRepoCommitEventListener(final LocalRepoCommitEventManager manager, final UUID localRepositoryId, final LocalRepoCommitEventListener listener) {
		expunge();

		this.manager = assertNotNull(manager, "manager");
		this.localRepositoryId = localRepositoryId;

		listenerRef = new WeakReference<LocalRepoCommitEventListener>(listener, listenerRefQueue);
		listenerRef2WeakPropertyChangeListener.put(listenerRef, this);
	}

	@Override
	public void postCommit(LocalRepoCommitEvent event) {
		expunge();

		final LocalRepoCommitEventListener listener = listenerRef.get();
		if (listener != null)
			listener.postCommit(event);
	}

	private static void expunge() {
		Reference<? extends LocalRepoCommitEventListener> ref;
		while ((ref = listenerRefQueue.poll()) != null) {
			final WeakLocalRepoCommitEventListener weakLocalRepoCommitEventListener = listenerRef2WeakPropertyChangeListener.remove(ref);

			if (weakLocalRepoCommitEventListener != null)
				weakLocalRepoCommitEventListener.removeLocalRepoCommitEventListener();
		}
	}

	public synchronized WeakLocalRepoCommitEventListener addLocalRepoCommitEventListener() {
		if (! registered) {
			manager.addLocalRepoCommitEventListener(localRepositoryId, this);
			registered = true;
		}
		return this;
	}

	public synchronized WeakLocalRepoCommitEventListener removeLocalRepoCommitEventListener() {
		if (registered) {
			manager.removeLocalRepoCommitEventListener(localRepositoryId, this);
			registered = false;
		}
		return this;
	}
}
