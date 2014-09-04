package org.subshare.gui.maintree;

import javafx.scene.Parent;

import org.subshare.gui.userlist.UserListPane;

public class UserListMainTreeItem extends MainTreeItem {

	@Override
	public String toString() {
		return "Users";
	}

	@Override
	protected Parent createMainDetailContent() {
		return new UserListPane();
	}

}
