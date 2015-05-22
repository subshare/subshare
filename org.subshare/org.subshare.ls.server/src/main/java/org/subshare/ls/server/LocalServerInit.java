package org.subshare.ls.server;

import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStoreImpl;
import org.subshare.core.pgp.transport.PgpTransportFactoryRegistry;
import org.subshare.core.user.UserRepoKeyRingLookupImpl;
import org.subshare.ls.server.ssl.AcceptAllDynamicX509TrustManagerCallback;
import org.subshare.rest.client.pgp.transport.RestPgpTransportFactory;
import org.subshare.rest.client.transport.CryptreeRestRepoTransportFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

public class LocalServerInit {

	private static final Logger logger = LoggerFactory.getLogger(LocalServerInit.class);

	private static boolean initialised;

	private LocalServerInit() {
	}

	public static synchronized void init() {
		if (! initialised) {
			PgpTransportFactoryRegistry.getInstance().getPgpTransportFactoryOrFail(RestPgpTransportFactory.class).setDynamicX509TrustManagerCallbackClass(AcceptAllDynamicX509TrustManagerCallback.class);

			final CryptreeRestRepoTransportFactoryImpl cryptreeRestRepoTransportFactoryImpl = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(CryptreeRestRepoTransportFactoryImpl.class);
			cryptreeRestRepoTransportFactoryImpl.setDynamicX509TrustManagerCallbackClass(AcceptAllDynamicX509TrustManagerCallback.class);
			cryptreeRestRepoTransportFactoryImpl.setUserRepoKeyRingLookup(new UserRepoKeyRingLookupImpl());

			final PgpAuthenticationCallback pgpAuthenticationCallback = PgpPrivateKeyPassphraseStoreImpl.getInstance().getPgpAuthenticationCallback();
			PgpRegistry.getInstance().setPgpAuthenticationCallback(pgpAuthenticationCallback);

			initialised = true;
		}
	}

//	public static synchronized void initUserRepoKeyRing() {
//		final UserRegistry userRegistry = UserRegistry.getInstance();
//		final List<User> usersWithUserRepoKey = new ArrayList<User>(1);
//		for (final User user : userRegistry.getUsers()) {
//			if (user.getUserRepoKeyRing() != null)
//				usersWithUserRepoKey.add(user);
//		}
//
//		if (usersWithUserRepoKey.size() > 1)
//			throw new IllegalStateException("There are multiple users with a UserRepoKey! Should only be exactly one!");
//
//		// TODO hook a listener to throw an exception as soon as a 2nd user has a UserRepoKey for early-detection of this illegal state.
//	}
}
