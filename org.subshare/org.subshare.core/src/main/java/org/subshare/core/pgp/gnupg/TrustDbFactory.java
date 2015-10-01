package org.subshare.core.pgp.gnupg;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;

import org.bouncycastle.openpgp.wot.TrustDb;
import org.bouncycastle.openpgp.wot.key.PgpKeyRegistry;

import co.codewizards.cloudstore.core.oio.File;

public class TrustDbFactory {
	private final File trustDbFile;
	private final PgpKeyRegistry pgpKeyRegistry;

	private TrustDb trustDb;
	private IdentityHashMap<TrustDb, TrustDb> proxies = new IdentityHashMap<>();
	private DeferredCloseThread deferredCloseThread;

	public TrustDbFactory(final File trustDbFile, final PgpKeyRegistry pgpKeyRegistry) {
		this.trustDbFile = assertNotNull("trustDbFile", trustDbFile);
		this.pgpKeyRegistry = assertNotNull("pgpKeyRegistry", pgpKeyRegistry);
	}

	public synchronized TrustDb createTrustDb() {
		if (trustDb == null)
			trustDb = TrustDb.Helper.createInstance(trustDbFile.getIoFile(), pgpKeyRegistry);

		final Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[] { TrustDb.class }, new InvocationHandler() {
			@Override
			public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
				final TrustDb trustDbProxy = (TrustDb) proxy;

				synchronized (TrustDbFactory.this) {
					if ("close".equals(method.getName())) {
						_close(trustDbProxy);
						return null;
					}

					_assertIsOpen(trustDbProxy);
					final Object result = method.invoke(trustDb, args);
					return result;
				}
			}
		});

		final TrustDb trustDbProxy = (TrustDb) proxy;
		proxies.put(trustDbProxy, trustDbProxy);
		return trustDbProxy;
	}

	protected void _close(final TrustDb trustDbProxy) {
		assertNotNull("trustDbProxy", trustDbProxy);
		if (_isOpen(trustDbProxy)) {
			proxies.remove(trustDbProxy);

			if (proxies.isEmpty()) {
				deferredCloseThread = new DeferredCloseThread();
				deferredCloseThread.start();
			}
		}
	}

	protected void _assertIsOpen(final TrustDb trustDbProxy) {
		assertNotNull("trustDbProxy", trustDbProxy);
		if (! _isOpen(trustDbProxy))
			throw new IllegalStateException("trustDbProxy is already closed!");
	}

	protected boolean _isOpen(final TrustDb trustDbProxy) {
		assertNotNull("trustDbProxy", trustDbProxy);
		return proxies.containsKey(trustDbProxy);
	}

	private class DeferredCloseThread extends Thread {
		@Override
		public void run() {
			try {
				Thread.sleep(10000); // defer closing by 10 seconds to avoid quick open-close-reopen-reclose-cycles
			} catch (final InterruptedException e) {
				doNothing();
			}

			synchronized (TrustDbFactory.this) {
				if (deferredCloseThread != DeferredCloseThread.this)
					return;

				if (proxies.isEmpty() && trustDb != null) {
					trustDb.close();
					trustDb = null;
				}

				deferredCloseThread = null;
			}
		}
	}
}
