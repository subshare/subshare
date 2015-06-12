package org.subshare.core.locker.sync;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.server.Server;
import org.subshare.core.sync.Sync;
import org.subshare.core.sync.SyncDaemonImpl;

public class LockerSyncDaemonImpl extends SyncDaemonImpl implements LockerSyncDaemon {

	public static final String CONFIG_KEY_PGP_SYNC_PERIOD = "lockerSyncPeriod";
	public static final long CONFIG_DEFAULT_VALUE_PGP_SYNC_PERIOD = 3600 * 1000; // 1 hour

	@Override
	protected String getConfigKeySyncPeriod() {
		return CONFIG_KEY_PGP_SYNC_PERIOD;
	}

	@Override
	protected long getConfigDefaultValueSyncPeriod() {
		return CONFIG_DEFAULT_VALUE_PGP_SYNC_PERIOD;
	}

	@Override
	protected Sync createSync(final Server server) {
		assertNotNull("server", server);
		return new LockerSync(server);
	}

	private static final class Holder {
		public static final LockerSyncDaemonImpl instance = new LockerSyncDaemonImpl();
	}

	public static LockerSyncDaemon getInstance() {
		return Holder.instance;
	}
}
