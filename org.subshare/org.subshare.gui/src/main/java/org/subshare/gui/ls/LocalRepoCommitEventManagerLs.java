package org.subshare.gui.ls;

import org.subshare.core.repo.listener.LocalRepoCommitEventManager;
import org.subshare.core.repo.listener.LocalRepoCommitEventManagerImpl;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class LocalRepoCommitEventManagerLs {

	private LocalRepoCommitEventManagerLs() { }

	public static LocalRepoCommitEventManager getLocalRepoCommitEventManager() {
		return LocalServerClient.getInstance().invokeStatic(LocalRepoCommitEventManagerImpl.class, "getInstance");
	}
}
