package org.subshare.gui.ls;

import org.subshare.core.repo.ServerRepoRegistry;
import org.subshare.core.repo.ServerRepoRegistryImpl;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class ServerRepoRegistryLs {
	private ServerRepoRegistryLs() {
	}

	public static ServerRepoRegistry getServerRepoRegistry() {
		return LocalServerClient.getInstance().invokeStatic(ServerRepoRegistryImpl.class, "getInstance");
	}
}
