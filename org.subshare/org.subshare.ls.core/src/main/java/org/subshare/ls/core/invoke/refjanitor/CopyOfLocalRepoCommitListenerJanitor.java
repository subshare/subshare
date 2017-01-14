package org.subshare.ls.core.invoke.refjanitor;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

public class CopyOfLocalRepoCommitListenerJanitor extends AbstractReferenceJanitor {

	private final WeakIdentityHashMap<LocalRepoCommitEventManager, List<IdentityWeakReference<LocalRepoCommitEventListener>>> manager2ListenerRefs = new WeakIdentityHashMap<>();
	private final WeakIdentityHashMap<LocalRepoCommitEventListener, WeakReference<FaultTolerantLocalRepoCommitListener>> originalListener2FaultTolerantLocalRepoCommitListenerRef =
			new WeakIdentityHashMap<>();

	@Override
	public void preInvoke(final ExtMethodInvocationRequest extMethodInvocationRequest) {
		final MethodInvocationRequest methodInvocationRequest = extMethodInvocationRequest.getMethodInvocationRequest();

		final Object bean = methodInvocationRequest.getObject(); // we don't support registering a LocalRepoCommitEventListener statically!
		if (! (bean instanceof LocalRepoCommitEventManager))
			return;

		final LocalRepoCommitEventManager manager = (LocalRepoCommitEventManager) bean;

		final String methodName = methodInvocationRequest.getMethodName();
		final Object[] arguments = methodInvocationRequest.getArguments();

		LocalRepoCommitEventListener listener = null;
		if (arguments.length == 1 && arguments[0] instanceof LocalRepoCommitEventListener) {
			listener = (LocalRepoCommitEventListener) arguments[0];
			arguments[0] = getFaultTolerantLocalRepoCommitListenerOrCreate(listener);
		}
		else
			return;

		assertNotNull(listener, "listener");

		if ("addLocalRepoCommitListener".equals(methodName))
			trackAddLocalRepoCommitListener(manager, listener);
		else if ("removeLocalRepoCommitListener".equals(methodName))
			trackRemoveLocalRepoCommitListener(manager, listener);
	}

	private synchronized FaultTolerantLocalRepoCommitListener getFaultTolerantLocalRepoCommitListenerOrCreate(final LocalRepoCommitEventListener listener) {
		assertNotNull(listener, "listener");

		final WeakReference<FaultTolerantLocalRepoCommitListener> ref = originalListener2FaultTolerantLocalRepoCommitListenerRef.get(listener);
		FaultTolerantLocalRepoCommitListener faultTolerantListener = ref == null ? null : ref.get();
		if (faultTolerantListener == null) {
			faultTolerantListener = new FaultTolerantLocalRepoCommitListener(listener);
			originalListener2FaultTolerantLocalRepoCommitListenerRef.put(listener, new WeakReference<>(faultTolerantListener));
		}
		return faultTolerantListener;
	}

	private synchronized FaultTolerantLocalRepoCommitListener getFaultTolerantLocalRepoCommitListener(final LocalRepoCommitEventListener listener) {
		assertNotNull(listener, "listener");

		final WeakReference<FaultTolerantLocalRepoCommitListener> ref = originalListener2FaultTolerantLocalRepoCommitListenerRef.get(listener);
		final FaultTolerantLocalRepoCommitListener faultTolerantListener = ref == null ? null : ref.get();
		return faultTolerantListener;
	}

	@Override
	public void cleanUp() {
		final Map<LocalRepoCommitEventManager, List<IdentityWeakReference<LocalRepoCommitEventListener>>> bean2ListenerRefs;
		synchronized (this) {
			bean2ListenerRefs = new HashMap<>(this.manager2ListenerRefs);
			this.manager2ListenerRefs.clear();
		}

		for (final Map.Entry<LocalRepoCommitEventManager, List<IdentityWeakReference<LocalRepoCommitEventListener>>> me1 : bean2ListenerRefs.entrySet()) {
			final LocalRepoCommitEventManager manager = me1.getKey();
			if (manager == null)
				throw new IllegalStateException("manager2ListenerRefs.entrySet() contained null-key!");


			for (final IdentityWeakReference<LocalRepoCommitEventListener> ref : me1.getValue()) {
				final LocalRepoCommitEventListener listener = ref.get();
				if (listener != null)
					_removeLocalRepoCommitListener(manager, listener);
			}
		}
	}

	private void _removeLocalRepoCommitListener(final LocalRepoCommitEventManager manager, final LocalRepoCommitEventListener listener) {
		assertNotNull(manager, "manager");
		assertNotNull(listener, "listener");

		final FaultTolerantLocalRepoCommitListener faultTolerantLocalRepoCommitListener = getFaultTolerantLocalRepoCommitListener(listener);
		if (faultTolerantLocalRepoCommitListener == null)
			return;

		manager.removeLocalRepoCommitEventListener(faultTolerantLocalRepoCommitListener);
	}

	private synchronized void trackAddLocalRepoCommitListener(final LocalRepoCommitEventManager manager, final LocalRepoCommitEventListener listener) {
		assertNotNull(manager, "manager");
		assertNotNull(listener, "listener");

		List<IdentityWeakReference<LocalRepoCommitEventListener>> listenerRefs = manager2ListenerRefs.get(manager);
		if (listenerRefs == null) {
			listenerRefs = new LinkedList<>();
			manager2ListenerRefs.put(manager, listenerRefs);
		}
		else
			expunge(listenerRefs);

		// addLocalRepoCommitListener(...) causes the same listener to be added multiple times.
		// Hence, we do the same here: Add it once for each invocation.
		final IdentityWeakReference<LocalRepoCommitEventListener> listenerRef = new IdentityWeakReference<LocalRepoCommitEventListener>(listener);
		listenerRefs.add(listenerRef);
	}

	private synchronized void trackRemoveLocalRepoCommitListener(final LocalRepoCommitEventManager manager, final LocalRepoCommitEventListener listener) {
		assertNotNull(manager, "manager");
		assertNotNull(listener, "listener");

		final List<IdentityWeakReference<LocalRepoCommitEventListener>> listenerRefs = manager2ListenerRefs.get(manager);
		if (listenerRefs == null)
			return;

		final IdentityWeakReference<LocalRepoCommitEventListener> listenerRef = new IdentityWeakReference<LocalRepoCommitEventListener>(listener);
		listenerRefs.remove(listenerRef);

		expunge(listenerRefs);

		if (listenerRefs.isEmpty())
			manager2ListenerRefs.remove(manager);
	}

	private void expunge(final List<IdentityWeakReference<LocalRepoCommitEventListener>> listenerRefs) {
		assertNotNull(listenerRefs, "listenerRefs");
		for (final Iterator<IdentityWeakReference<LocalRepoCommitEventListener>> it = listenerRefs.iterator(); it.hasNext();) {
			final IdentityWeakReference<LocalRepoCommitEventListener> ref = it.next();
			if (ref.get() == null)
				it.remove();
		}
	}

	private static class FaultTolerantLocalRepoCommitListener implements LocalRepoCommitEventListener {
		private static final Logger logger = LoggerFactory.getLogger(CopyOfLocalRepoCommitListenerJanitor.FaultTolerantLocalRepoCommitListener.class);

		private final LocalRepoCommitEventListener delegate;

		public FaultTolerantLocalRepoCommitListener(final LocalRepoCommitEventListener delegate) {
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
