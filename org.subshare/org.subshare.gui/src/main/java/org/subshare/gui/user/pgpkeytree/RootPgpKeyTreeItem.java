package org.subshare.gui.user.pgpkeytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.control.TreeTableView;

import org.subshare.core.pgp.PgpKey;
import org.subshare.core.user.User;

public class RootPgpKeyTreeItem extends PgpKeyTreeItem<User> {

	private final TreeTableView<PgpKeyTreeItem<?>> treeTableView;

	public RootPgpKeyTreeItem(final TreeTableView<PgpKeyTreeItem<?>> treeTableView, final User user) {
		super(assertNotNull("user", user));
		this.treeTableView = assertNotNull("treeTableView", treeTableView);

		for (final PgpKey pgpKey : user.getPgpKeys()) {
			final PgpKeyPgpKeyTreeItem child = new PgpKeyPgpKeyTreeItem(pgpKey);
			getChildren().add(child);
		}
	}

	@Override
	protected TreeTableView<PgpKeyTreeItem<?>> getTreeTableView() {
		return treeTableView;
	}
}