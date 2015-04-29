package org.subshare.gui.maintree;

import javafx.scene.Parent;

import org.subshare.gui.userlist.UserListPane;

public class UserListMainTreeItem extends MainTreeItem<String> {

	public UserListMainTreeItem() {
		super("Users");
	}

	@Override
	protected Parent createMainDetailContent() {
		return new UserListPane();
	}
}
