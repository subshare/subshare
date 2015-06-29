package org.subshare.gui.pgp.keytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.control.TreeTableView;

public class SimpleRootPgpKeyTreeItem extends PgpKeyTreeItem<String> {

	private final TreeTableView<PgpKeyTreeItem<?>> treeTableView;

	public SimpleRootPgpKeyTreeItem(final TreeTableView<PgpKeyTreeItem<?>> treeTableView) {
		super("");
		this.treeTableView = assertNotNull("treeTableView", treeTableView);
	}

	@Override
	protected TreeTableView<PgpKeyTreeItem<?>> getTreeTableView() {
		return treeTableView;
	}
}