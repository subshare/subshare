package org.subshare.gui.ls;

import org.subshare.core.repo.LocalRepoRegistry;
import org.subshare.core.repo.LocalRepoRegistryImpl;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class LocalRepoRegistryLs {
	private LocalRepoRegistryLs() {
	}

	public static LocalRepoRegistry getLocalRepoRegistry() {
		return LocalServerClient.getInstance().invokeStatic(LocalRepoRegistryImpl.class, "getInstance");
	}
}
