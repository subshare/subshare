package org.subshare.gui.ls;

import org.subshare.core.repo.ServerRepoManager;
import org.subshare.core.repo.ServerRepoManagerImpl;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class ServerRepoManagerLs {
	private ServerRepoManagerLs() {
	}

	public static ServerRepoManager getServerRepoManager() {
		return LocalServerClient.getInstance().invokeStatic(ServerRepoManagerImpl.class, "getInstance");
	}
}
