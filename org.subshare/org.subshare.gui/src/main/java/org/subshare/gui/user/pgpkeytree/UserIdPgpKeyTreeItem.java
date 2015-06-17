package org.subshare.gui.user.pgpkeytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import org.subshare.core.pgp.PgpKey;

public class UserIdPgpKeyTreeItem extends PgpKeyTreeItem<String> {

	private final PgpKey pgpKey;

	public UserIdPgpKeyTreeItem(final PgpKey pgpKey, final String userId) {
		super(assertNotNull("userId", userId));
		this.pgpKey = assertNotNull("pgpKey", pgpKey);
	}

	@Override
	public ObservableList<TreeItem<PgpKeyTreeItem<?>>> getChildren() {
		final ObservableList<TreeItem<PgpKeyTreeItem<?>>> children = super.getChildren();
		if (children.isEmpty()) {
			final String userId = getValueObject();
			children.add(new CertificationsPgpKeyTreeItem(pgpKey, userId));
		}
		return children;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

}
