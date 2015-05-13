package org.subshare.ls.server;

import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStoreImpl;
import org.subshare.core.pgp.transport.PgpTransportFactoryRegistry;
import org.subshare.ls.server.ssl.AcceptAllDynamicX509TrustManagerCallback;
import org.subshare.rest.client.pgp.transport.RestPgpTransportFactory;
import org.subshare.rest.client.transport.CryptreeRepoTransportFactoryImpl;

import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

public class LocalServerInit {
	private static boolean initialised;

	private LocalServerInit() {
	}

	public static synchronized void init() {
		if (! initialised) {
			RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(CryptreeRepoTransportFactoryImpl.class).setDynamicX509TrustManagerCallbackClass(AcceptAllDynamicX509TrustManagerCallback.class);
			PgpTransportFactoryRegistry.getInstance().getPgpTransportFactoryOrFail(RestPgpTransportFactory.class).setDynamicX509TrustManagerCallbackClass(AcceptAllDynamicX509TrustManagerCallback.class);

			final PgpAuthenticationCallback pgpAuthenticationCallback = PgpPrivateKeyPassphraseStoreImpl.getInstance().getPgpAuthenticationCallback();
			PgpRegistry.getInstance().setPgpAuthenticationCallback(pgpAuthenticationCallback);

			initialised = true;
		}
	}
}
