package org.subshare.gui.maintree;

public class RepositoryMainTreeItem extends MainTreeItem {

	private final ServerMainTreeItem serverMainTreeItem;

	private String name;

	public RepositoryMainTreeItem(final ServerMainTreeItem serverMainTreeItem) {
		this.serverMainTreeItem = serverMainTreeItem;
	}

	public ServerMainTreeItem getServerMainTreeItem() {
		return serverMainTreeItem;
	}

	public String getName() {
		return name;
	}
	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

}
