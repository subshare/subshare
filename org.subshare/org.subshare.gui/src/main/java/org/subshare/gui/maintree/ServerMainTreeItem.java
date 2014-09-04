package org.subshare.gui.maintree;

import javafx.scene.Parent;

import org.subshare.gui.server.ServerPane;

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

	@Override
	protected Parent createMainDetailContent() {
		return new ServerPane();
	}

}
