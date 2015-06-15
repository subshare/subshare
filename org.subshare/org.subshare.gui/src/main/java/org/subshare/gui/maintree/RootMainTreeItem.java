package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.control.TreeView;

public class RootMainTreeItem extends MainTreeItem<String> {

	private final TreeView<String> mainTree;

	public RootMainTreeItem(final TreeView<String> mainTree) {
		this.mainTree = assertNotNull("mainTree", mainTree);
	}

	@Override
	protected TreeView<String> getMainTree() {
		return mainTree;
	}
}