package org.subshare.gui.ls;

import org.subshare.core.locker.sync.LockerSync;
import org.subshare.core.server.Server;
import org.subshare.core.sync.Sync;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class LockerSyncLs {

	private LockerSyncLs() {
	}

	public static Sync createLockerSync(Server server) {
		Sync result = LocalServerClient.getInstance().invokeConstructor(LockerSync.class, server);
		return result;
	}
}
