package org.subshare.ls.core.invoke.refjanitor;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.subshare.core.repo.listener.LocalRepoCommitEvent;
import org.subshare.core.repo.listener.LocalRepoCommitEventListener;
import org.subshare.core.repo.listener.LocalRepoCommitEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.collection.WeakIdentityHashMap;
import co.codewizards.cloudstore.core.ref.IdentityWeakReference;
import co.codewizards.cloudstore.ls.core.invoke.MethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.filter.ExtMethodInvocationRequest;
import co.codewizards.cloudstore.ls.core.invoke.refjanitor.AbstractReferenceJanitor;

public class LocalRepoCommitEventListenerJanitor extends AbstractReferenceJanitor {

	private final WeakIdentityHashMap<LocalRepoCommitEventManager, Map<UUID, List<IdentityWeakReference<LocalRepoCommitEventListener>>>> manager2LocalRepositoryId2ListenerRefs = new WeakIdentityHashMap<>();
	private final WeakIdentityHashMap<LocalRepoCommitEventListener, FaultTolerantLocalRepoCommitEventListener> originalListener2FaultTolerantLocalRepoCommitEventListener =
			new WeakIdentityHashMap<>();

	@Override
	public void preInvoke(final ExtMethodInvocationRequest extMethodInvocationRequest) {
		final MethodInvocationRequest methodInvocationRequest = extMethodInvocationRequest.getMethodInvocationRequest();

		final Object object = methodInvocationRequest.getObject(); // we don't support registering a LocalRepoCommitEventListener statically!
		if (! (object instanceof LocalRepoCommitEventManager))
			return;

		final LocalRepoCommitEventManager manager = (LocalRepoCommitEventManager) object;

		final String methodName = methodInvocationRequest.getMethodName();
		final Object[] arguments = methodInvocationRequest.getArguments();

		UUID localRepositoryId = null;
		LocalRepoCommitEventListener listener = null;
		if (arguments.length == 1 && arguments[0] instanceof LocalRepoCommitEventListener) {
			listener = (LocalRepoCommitEventListener) arguments[0];
			arguments[0] = getFaultTolerantLocalRepoCommitEventListenerOrCreate(listener);
		}
		else if (arguments.length == 2 && arguments[1] instanceof LocalRepoCommitEventListener) {
			listener = (LocalRepoCommitEventListener) arguments[1];
			if (arguments[0] instanceof UUID)
				localRepositoryId = (UUID) arguments[0];
			else
				return;

			arguments[1] = getFaultTolerantLocalRepoCommitEventListenerOrCreate(listener);
		}
		else
			return;

		assertNotNull(listener, "listener");

		if ("addLocalRepoCommitEventListener".equals(methodName))
			trackAddLocalRepoCommitEventListener(manager, localRepositoryId, listener);
		else if ("removeLocalRepoCommitEventListener".equals(methodName))
			trackRemoveLocalRepoCommitEventListener(manager, localRepositoryId, listener);
	}

	private synchronized FaultTolerantLocalRepoCommitEventListener getFaultTolerantLocalRepoCommitEventListenerOrCreate(final LocalRepoCommitEventListener listener) {
		assertNotNull(listener, "listener");

		FaultTolerantLocalRepoCommitEventListener faultTolerantListener = originalListener2FaultTolerantLocalRepoCommitEventListener.get(listener);
		if (faultTolerantListener == null) {
			faultTolerantListener = new FaultTolerantLocalRepoCommitEventListener(listener);
			originalListener2FaultTolerantLocalRepoCommitEventListener.put(listener, faultTolerantListener);
		}
		return faultTolerantListener;
	}

	private synchronized FaultTolerantLocalRepoCommitEventListener getFaultTolerantLocalRepoCommitEventListener(final LocalRepoCommitEventListener listener) {
		assertNotNull(listener, "listener");

		final FaultTolerantLocalRepoCommitEventListener faultTolerantListener = originalListener2FaultTolerantLocalRepoCommitEventListener.get(listener);
		return faultTolerantListener;
	}

	@Override
	public void cleanUp() {
		final Map<LocalRepoCommitEventManager, Map<UUID, List<IdentityWeakReference<LocalRepoCommitEventListener>>>> manager2LocalRepositoryId2ListenerRefs;
		synchronized (this) {
			manager2LocalRepositoryId2ListenerRefs = new HashMap<>(this.manager2LocalRepositoryId2ListenerRefs);
			this.manager2LocalRepositoryId2ListenerRefs.clear();
		}

		for (final Map.Entry<LocalRepoCommitEventManager, Map<UUID, List<IdentityWeakReference<LocalRepoCommitEventListener>>>> me1 : manager2LocalRepositoryId2ListenerRefs.entrySet()) {
			final LocalRepoCommitEventManager manager = me1.getKey();
			if (manager == null)
				throw new IllegalStateException("manager2LocalRepositoryId2ListenerRefs.entrySet() contained null-key!");

			for (final Map.Entry<UUID, List<IdentityWeakReference<LocalRepoCommitEventListener>>> me2 : me1.getValue().entrySet()) {
				final UUID localRepositoryId = me2.getKey();

				for (final IdentityWeakReference<LocalRepoCommitEventListener> ref : me2.getValue()) {
					final LocalRepoCommitEventListener listener = ref.get();
					if (listener != null)
						_removeLocalRepoCommitEventListener(manager, localRepositoryId, listener);
				}
			}
		}
	}

	private void _removeLocalRepoCommitEventListener(final LocalRepoCommitEventManager manager, final UUID localRepositoryId, final LocalRepoCommitEventListener listener) {
		assertNotNull(manager, "manager");
		assertNotNull(listener, "listener");

		final FaultTolerantLocalRepoCommitEventListener faultTolerantLocalRepoCommitEventListener = getFaultTolerantLocalRepoCommitEventListener(listener);
		if (faultTolerantLocalRepoCommitEventListener == null)
			return;

		manager.removeLocalRepoCommitEventListener(localRepositoryId, faultTolerantLocalRepoCommitEventListener);
	}

	private synchronized void trackAddLocalRepoCommitEventListener(final LocalRepoCommitEventManager manager, final UUID localRepositoryId, final LocalRepoCommitEventListener listener) {
		assertNotNull(manager, "manager");
		assertNotNull(listener, "listener");

		Map<UUID, List<IdentityWeakReference<LocalRepoCommitEventListener>>> localRepositoryId2ListenerRefs = manager2LocalRepositoryId2ListenerRefs.get(manager);
		if (localRepositoryId2ListenerRefs == null) {
			localRepositoryId2ListenerRefs = new HashMap<>();
			manager2LocalRepositoryId2ListenerRefs.put(manager, localRepositoryId2ListenerRefs);
		}

		List<IdentityWeakReference<LocalRepoCommitEventListener>> listenerRefs = localRepositoryId2ListenerRefs.get(localRepositoryId);
		if (listenerRefs == null) {
			listenerRefs = new LinkedList<>();
			localRepositoryId2ListenerRefs.put(localRepositoryId, listenerRefs);
		}
		else
			expunge(listenerRefs);

		// addLocalRepoCommitEventListener(...) causes the same listener to be added multiple times.
		// Hence, we do the same here: Add it once for each invocation.
		final IdentityWeakReference<LocalRepoCommitEventListener> listenerRef = new IdentityWeakReference<LocalRepoCommitEventListener>(listener);
		listenerRefs.add(listenerRef);
	}

	private synchronized void trackRemoveLocalRepoCommitEventListener(final LocalRepoCommitEventManager manager, final UUID localRepositoryId, final LocalRepoCommitEventListener listener) {
		assertNotNull(manager, "manager");
		assertNotNull(listener, "listener");

		final Map<UUID, List<IdentityWeakReference<LocalRepoCommitEventListener>>> localRepositoryId2ListenerRefs = manager2LocalRepositoryId2ListenerRefs.get(manager);
		if (localRepositoryId2ListenerRefs == null)
			return;

		final List<IdentityWeakReference<LocalRepoCommitEventListener>> listenerRefs = localRepositoryId2ListenerRefs.get(localRepositoryId);
		if (listenerRefs == null)
			return;

		final IdentityWeakReference<LocalRepoCommitEventListener> listenerRef = new IdentityWeakReference<LocalRepoCommitEventListener>(listener);
		listenerRefs.remove(listenerRef);

		expunge(listenerRefs);

		if (listenerRefs.isEmpty())
			localRepositoryId2ListenerRefs.remove(localRepositoryId);

		if (localRepositoryId2ListenerRefs.isEmpty())
			manager2LocalRepositoryId2ListenerRefs.remove(manager);
	}

	private void expunge(final List<IdentityWeakReference<LocalRepoCommitEventListener>> listenerRefs) {
		assertNotNull(listenerRefs, "listenerRefs");
		for (final Iterator<IdentityWeakReference<LocalRepoCommitEventListener>> it = listenerRefs.iterator(); it.hasNext();) {
			final IdentityWeakReference<LocalRepoCommitEventListener> ref = it.next();
			if (ref.get() == null)
				it.remove();
		}
	}

	private static class FaultTolerantLocalRepoCommitEventListener implements LocalRepoCommitEventListener {
		private static final Logger logger = LoggerFactory.getLogger(LocalRepoCommitEventListenerJanitor.FaultTolerantLocalRepoCommitEventListener.class);

		private final LocalRepoCommitEventListener delegate;

		public FaultTolerantLocalRepoCommitEventListener(final LocalRepoCommitEventListener delegate) {
			this.delegate = assertNotNull(delegate, "delegate");
		}

		@Override
		public void postCommit(LocalRepoCommitEvent event) {
			try {
				delegate.postCommit(event);
			} catch (final Exception x) {
				logger.error("postCommit: " + x, x);
			}
		}

		@Override
		protected void finalize() throws Throwable {
			logger.debug("finalize: entered.");
			super.finalize();
		}
	}
}
