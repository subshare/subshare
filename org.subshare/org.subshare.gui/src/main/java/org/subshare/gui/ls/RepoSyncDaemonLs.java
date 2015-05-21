package org.subshare.gui.ls;

import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemonImpl;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class RepoSyncDaemonLs {
	private RepoSyncDaemonLs() {
	}

	public static RepoSyncDaemon getRepoSyncDaemon() {
		return LocalServerClient.getInstance().invokeStatic(RepoSyncDaemonImpl.class, "getInstance");
	}
}
