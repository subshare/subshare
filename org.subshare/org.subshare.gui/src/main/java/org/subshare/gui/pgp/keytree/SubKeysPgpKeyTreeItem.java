package org.subshare.gui.pgp.keytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import org.subshare.core.pgp.PgpKey;

public class SubKeysPgpKeyTreeItem extends PgpKeyTreeItem<PgpKey> {

	private boolean childrenInitialised;

	public SubKeysPgpKeyTreeItem(final PgpKey pgpKey) {
		super(assertNotNull("pgpKey", pgpKey));
	}

	@Override
	public ObservableList<TreeItem<PgpKeyTreeItem<?>>> getChildren() {
		final ObservableList<TreeItem<PgpKeyTreeItem<?>>> children = super.getChildren();
		if (! childrenInitialised) {
			childrenInitialised = true;
			final PgpKey pgpKey = getValueObject();
			children.add(new SubKeyPgpKeyTreeItem(pgpKey)); // primary key
			for (final PgpKey subKey : pgpKey.getSubKeys())
				children.add(new SubKeyPgpKeyTreeItem(subKey)); // secondary key
		}
		return children;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public String getName() {
		return "Sub-keys";
	}
}
