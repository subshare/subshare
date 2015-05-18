package org.subshare.test;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;
import mockit.Mock;
import mockit.MockUp;

import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.gnupg.BcWithLocalGnuPgPgp;
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
	private static MockUp<BcWithLocalGnuPgPgp> pgpMockUp;

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

		pgpMockUp = new MockUp<BcWithLocalGnuPgPgp>() {
			@Mock
			PgpKey getPgpKey(PgpKeyId pgpKeyId) {
				if ("d7a92a24aa97ddbd".equals(pgpKeyId.toString()))
					return PgpKey.TEST_DUMMY_PGP_KEY;

				throw new UnsupportedOperationException("Not implemented!");
			}

			@Mock
			void testPassphrase(PgpKey pgpKey, char[] passphrase) throws IllegalArgumentException, SecurityException {
				// nothing ;-)
			}
		};
	}

	@AfterClass
	public static void afterLocalServerClientIT() {
		client.close();
		localServer.stop();

		if (pgpMockUp != null)
			pgpMockUp.tearDown();

		pgpMockUp = null;
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
