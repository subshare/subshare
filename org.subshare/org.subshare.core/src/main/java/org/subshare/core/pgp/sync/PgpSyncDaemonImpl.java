package org.subshare.core.pgp.sync;

import static java.util.Objects.*;

import org.subshare.core.server.Server;
import org.subshare.core.sync.Sync;
import org.subshare.core.sync.SyncDaemonImpl;

public class PgpSyncDaemonImpl extends SyncDaemonImpl implements PgpSyncDaemon {

	public static final String CONFIG_KEY_PGP_SYNC_PERIOD = "pgpSyncPeriod";
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
		requireNonNull(server, "server");
		return new PgpSync(server);
	}

	private static final class Holder {
		public static final PgpSyncDaemonImpl instance = new PgpSyncDaemonImpl();
	}

	public static PgpSyncDaemon getInstance() {
		return Holder.instance;
	}
}
