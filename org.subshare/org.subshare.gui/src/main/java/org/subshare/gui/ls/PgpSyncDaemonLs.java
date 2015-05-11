package org.subshare.gui.ls;

import org.subshare.core.pgp.sync.PgpSyncDaemon;
import org.subshare.core.pgp.sync.PgpSyncDaemonImpl;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class PgpSyncDaemonLs {
	private PgpSyncDaemonLs() {
	}

	public static PgpSyncDaemon getPgpSyncDaemon() {
		return LocalServerClient.getInstance().invokeStatic(PgpSyncDaemonImpl.class, "getInstance");
	}
}
