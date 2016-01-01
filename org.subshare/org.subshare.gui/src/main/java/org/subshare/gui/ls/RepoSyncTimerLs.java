package org.subshare.gui.ls;

import org.subshare.core.repo.sync.RepoSyncTimer;
import org.subshare.core.repo.sync.RepoSyncTimerImpl;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class RepoSyncTimerLs {
	private RepoSyncTimerLs() {
	}

	public static RepoSyncTimer getRepoSyncTimer() {
		return LocalServerClient.getInstance().invokeStatic(RepoSyncTimerImpl.class, "getInstance");
	}
}
