package org.subshare.gui.ls;

import org.subshare.core.server.ServerRegistry;
import org.subshare.core.server.ServerRegistryImpl;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class ServerRegistryLs {
	private static volatile boolean initialisedServerRepoRegistry;

	private ServerRegistryLs() {
	}

	public static ServerRegistry getServerRegistry() {
		if (!initialisedServerRepoRegistry) {
			initialisedServerRepoRegistry = true;
			ServerRepoRegistryLs.getServerRepoRegistry();
		}
		return LocalServerClient.getInstance().invokeStatic(ServerRegistryImpl.class, "getInstance");
	}
}
