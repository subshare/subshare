package org.subshare.test;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStore;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStoreImpl;
import org.subshare.ls.server.SsLocalServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.ls.client.LocalServerClient;
import co.codewizards.cloudstore.ls.rest.client.LocalServerRestClient;

public class LocalServerClientIT extends AbstractIT {

	private static SsLocalServer localServer;
	private static LocalServerClient client;

	@BeforeClass
	public static void beforeLocalServerClientIT() {
		localServer = new SsLocalServer();
		localServer.start();

		final LocalServerRestClient localServerRestClient = new LocalServerRestClient() {
		};

		client = new LocalServerClient() {
			@Override
			protected LocalServerRestClient _getLocalServerRestClient() {
				return localServerRestClient;
			}
		};
	}

	@AfterClass
	public static void afterLocalServerClientIT() {
		client.close();
		localServer.stop();
	}

	@Test
	public void invokeSimpleStaticMethod() throws Exception {
		Long remoteMillis = client.invokeStatic(System.class, "currentTimeMillis");
		long localMillis = System.currentTimeMillis();
		assertThat(remoteMillis).isNotNull();
		assertThat(localMillis - remoteMillis).isBetween(0L, 10000L);
	}

	@Test
	public void invokeDeniedMethods() throws Exception {
		PgpPrivateKeyPassphraseStore store = client.invokeStatic(PgpPrivateKeyPassphraseStoreImpl.class, "getInstance");

		PgpKeyId pgpKeyId = new PgpKeyId("d7a92a24aa97ddbd");
		store.putPassphrase(pgpKeyId, "top secret".toCharArray());

		try {
			store.getPgpAuthenticationCallback();
			fail("Succeeded invoking a method that should be denied!");
		} catch (SecurityException x) {
			doNothing();
		}

		try {
			client.invoke(store, "getPassphrase", pgpKeyId);
			fail("Succeeded invoking a method that should be denied!");
		} catch (SecurityException x) {
			doNothing();
		}
	}
}
