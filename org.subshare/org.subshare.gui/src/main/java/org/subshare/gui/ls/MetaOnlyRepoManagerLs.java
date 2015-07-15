package org.subshare.gui.ls;

import org.subshare.core.repo.metaonly.MetaOnlyRepoManager;
import org.subshare.core.repo.metaonly.MetaOnlyRepoManagerImpl;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class MetaOnlyRepoManagerLs {

	private MetaOnlyRepoManagerLs() {
	}

	public static MetaOnlyRepoManager getMetaOnlyRepoManager() {
		return LocalServerClient.getInstance().invokeStatic(MetaOnlyRepoManagerImpl.class, "getInstance");
	}
}
