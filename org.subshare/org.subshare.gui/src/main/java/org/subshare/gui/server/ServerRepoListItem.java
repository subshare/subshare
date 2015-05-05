package org.subshare.gui.server;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import org.subshare.core.repo.ServerRepo;

public class ServerRepoListItem {

	private ServerRepo serverRepo;

	public ServerRepoListItem() { }

	public ServerRepoListItem(final ServerRepo serverRepo) {
		this.serverRepo = serverRepo;
	}

	public ServerRepo getServerRepo() {
		return serverRepo;
	}
	public void setServerRepo(ServerRepo serverRepo) {
		this.serverRepo = serverRepo;
	}

	public String getName() {
		return assertNotNull("serverRepo", serverRepo).getName();
	}
	public void setName(String name) {
		assertNotNull("serverRepo", serverRepo).setName(name);
	}
}
