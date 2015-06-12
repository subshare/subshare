package org.subshare.gui.ls;

import co.codewizards.cloudstore.core.repo.local.LocalRepoManagerFactory;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class LocalRepoManagerFactoryLs {

	private LocalRepoManagerFactoryLs() {
	}

	public static LocalRepoManagerFactory getLocalRepoManagerFactory() {
		final LocalRepoManagerFactory factory = LocalServerClient.getInstance().invokeStatic(
				LocalRepoManagerFactory.Helper.class, "getInstance");
		return factory;
	}
}