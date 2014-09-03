package org.subshare.gui.maintree;

public class ServerMainTreeItem extends MainTreeItem {
	private String name;

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
