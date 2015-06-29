package org.subshare.gui.pgp.keytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import org.subshare.core.pgp.PgpKey;

public class UserIdsPgpKeyTreeItem extends PgpKeyTreeItem<PgpKey> {

	private boolean childrenInitialised;

	public UserIdsPgpKeyTreeItem(final PgpKey pgpKey) {
		super(assertNotNull("pgpKey", pgpKey));
	}

	@Override
	public ObservableList<TreeItem<PgpKeyTreeItem<?>>> getChildren() {
		final ObservableList<TreeItem<PgpKeyTreeItem<?>>> children = super.getChildren();
		if (!childrenInitialised) {
			childrenInitialised = true;
			final PgpKey pgpKey = getValueObject();
			for (String userId : pgpKey.getUserIds())
				children.add(new UserIdPgpKeyTreeItem(pgpKey, userId));
		}
		return children;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public String getName() {
		return "Identities";
	}
}
