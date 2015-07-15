package org.subshare.gui.ls;

import org.subshare.core.repo.metaonly.MetaOnlyRepoSyncDaemon;
import org.subshare.core.repo.metaonly.MetaOnlyRepoSyncDaemonImpl;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class MetaOnlyRepoSyncDaemonLs {

	private MetaOnlyRepoSyncDaemonLs() {
	}

	public static MetaOnlyRepoSyncDaemon getMetaOnlyRepoSyncDaemon() {
		return LocalServerClient.getInstance().invokeStatic(MetaOnlyRepoSyncDaemonImpl.class, "getInstance");
	}
}
