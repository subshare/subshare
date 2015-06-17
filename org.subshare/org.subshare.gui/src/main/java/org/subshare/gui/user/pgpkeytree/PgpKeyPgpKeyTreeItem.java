package org.subshare.gui.user.pgpkeytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import org.subshare.core.pgp.PgpKey;

public class PgpKeyPgpKeyTreeItem extends PgpKeyTreeItem<PgpKey> {

	public PgpKeyPgpKeyTreeItem(final PgpKey pgpKey) {
		super(assertNotNull("pgpKey", pgpKey));
	}

	@Override
	public ObservableList<TreeItem<PgpKeyTreeItem<?>>> getChildren() {
		final ObservableList<TreeItem<PgpKeyTreeItem<?>>> children = super.getChildren();
		if (children.isEmpty()) {
			final PgpKey pgpKey = getValueObject();
			children.add(new UserIdsPgpKeyTreeItem(pgpKey));
			children.add(new SubKeysPgpKeyTreeItem(pgpKey));
		}
		return children;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public String getName() {
		final List<String> userIds = getValueObject().getUserIds();
		return userIds.isEmpty() ? getKeyId() : userIds.get(0);
	}

	@Override
	public String getKeyId() {
		return getValueObject().getPgpKeyId().toHumanString();
	}
}
