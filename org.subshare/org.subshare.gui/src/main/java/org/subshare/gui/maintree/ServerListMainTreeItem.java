package org.subshare.gui.maintree;

import javafx.scene.Parent;

import org.subshare.gui.serverlist.ServerListPane;

public class ServerListMainTreeItem extends MainTreeItem {

	@Override
	public String toString() {
		return "Servers";
	}

	@Override
	protected Parent createMainDetailContent() {
		return new ServerListPane();
	}

}
