package org.subshare.ls.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.locker.sync.LockerSyncDaemonImpl;
import org.subshare.core.locker.transport.LockerTransportFactoryRegistry;
import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStoreImpl;
import org.subshare.core.pgp.sync.PgpSyncDaemonImpl;
import org.subshare.core.pgp.transport.PgpTransportFactoryRegistry;
import org.subshare.core.repo.metaonly.MetaOnlyRepoSyncDaemonImpl;
import org.subshare.core.repo.sync.RepoSyncTimerImpl;
import org.subshare.ls.server.ssl.AcceptAllDynamicX509TrustManagerCallback;
import org.subshare.rest.client.locker.transport.RestLockerTransportFactory;
import org.subshare.rest.client.pgp.transport.RestPgpTransportFactory;
import org.subshare.rest.client.transport.CryptreeRestRepoTransportFactoryImpl;

import co.codewizards.cloudstore.core.repo.transport.RepoTransportFactoryRegistry;

public class LocalServerInit {

	private static final Logger logger = LoggerFactory.getLogger(LocalServerInit.class);

	private static boolean initPrepareDone;
	private static boolean initFinishDone;

	private LocalServerInit() {
	}

	public static synchronized void initPrepare() {
		if (! initPrepareDone) {
			PgpTransportFactoryRegistry.getInstance().getPgpTransportFactoryOrFail(RestPgpTransportFactory.class).setDynamicX509TrustManagerCallbackClass(AcceptAllDynamicX509TrustManagerCallback.class);
			LockerTransportFactoryRegistry.getInstance().getLockerTransportFactoryOrFail(RestLockerTransportFactory.class).setDynamicX509TrustManagerCallbackClass(AcceptAllDynamicX509TrustManagerCallback.class);

			final CryptreeRestRepoTransportFactoryImpl cryptreeRestRepoTransportFactoryImpl = RepoTransportFactoryRegistry.getInstance().getRepoTransportFactoryOrFail(CryptreeRestRepoTransportFactoryImpl.class);
			cryptreeRestRepoTransportFactoryImpl.setDynamicX509TrustManagerCallbackClass(AcceptAllDynamicX509TrustManagerCallback.class);
//			cryptreeRestRepoTransportFactoryImpl.setUserRepoKeyRingLookup(new UserRepoKeyRingLookupImpl());

			final PgpAuthenticationCallback pgpAuthenticationCallback = PgpPrivateKeyPassphraseStoreImpl.getInstance().getPgpAuthenticationCallback();
			PgpRegistry.getInstance().setPgpAuthenticationCallback(pgpAuthenticationCallback);

			initPrepareDone = true;

			// Important: We do not yet start the daemons here (i.e. do not invoke PgpSyncDaemonImpl.getInstance(),
			//   LockerSyncDaemonImpl.getInstance() or similar), because the user did not yet enter any password when
			//   this method is invoked. This method only prepares everything so that we *can* start the daemons, now.
		}
	}

	public static synchronized void initFinish() {
		if (! initFinishDone) {
			// *Now* we start the daemons (if they don't run, yet). We perform a sync *now* in the background
			// to make sure the PGP stuff is synced, before the Locker stuff. If the daemons simply run in the
			// background on their own, we don't have any control over the order. This is not essentially necessary,
			// but it reduces error-log-messages ;-)
			final Thread localServerInitFinishThread = new Thread() {
				@Override
				public void run() {
					PgpSyncDaemonImpl.getInstance().sync();
					LockerSyncDaemonImpl.getInstance().sync();
					MetaOnlyRepoSyncDaemonImpl.getInstance().sync();
					RepoSyncTimerImpl.getInstance(); // this is *not* blocking (the above invocations are) => the repo-syncs are in the background.
				}
			};
			localServerInitFinishThread.start();

			initFinishDone = true;
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
