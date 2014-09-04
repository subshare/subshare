package org.subshare.gui.maintree;

import javafx.scene.Parent;

public class MainTreeItem {

	private Parent mainDetailContent;

//	private final MainTreeItem parent;
//
//	public MainTreeItem(final MainTreeItem parent) {
//		this.parent = parent;
//	}
//
//	public MainTreeItem getParent() {
//		return parent;
//	}

	public Parent getMainDetailContent() {
		if (mainDetailContent == null) {
			mainDetailContent = createMainDetailContent();
		}
		return mainDetailContent;
	}

	protected Parent createMainDetailContent() {
		return null;
	}
}
