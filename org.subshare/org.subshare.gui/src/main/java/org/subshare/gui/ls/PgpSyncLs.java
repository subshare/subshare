package org.subshare.gui.ls;

import org.subshare.core.pgp.sync.PgpSync;
import org.subshare.core.server.Server;
import org.subshare.core.sync.Sync;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class PgpSyncLs {

	private PgpSyncLs() {
	}

	public static Sync createPgpSync(Server server) {
		Sync result = LocalServerClient.getInstance().invokeConstructor(PgpSync.class, server);
		return result;
	}
}
