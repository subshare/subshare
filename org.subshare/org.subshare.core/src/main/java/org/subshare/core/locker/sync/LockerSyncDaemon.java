package org.subshare.core.locker.sync;

import org.subshare.core.sync.SyncDaemon;

public interface LockerSyncDaemon extends SyncDaemon {
	public static interface Property extends SyncDaemon.Property {
	}

	public static enum PropertyEnum implements Property {
	}
}
