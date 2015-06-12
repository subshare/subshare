package org.subshare.gui.ls;

import org.subshare.core.locker.sync.LockerSyncDaemon;
import org.subshare.core.locker.sync.LockerSyncDaemonImpl;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class LockerSyncDaemonLs {
	private LockerSyncDaemonLs() {
	}

	public static LockerSyncDaemon getLockerSyncDaemon() {
		return LocalServerClient.getInstance().invokeStatic(LockerSyncDaemonImpl.class, "getInstance");
	}
}
