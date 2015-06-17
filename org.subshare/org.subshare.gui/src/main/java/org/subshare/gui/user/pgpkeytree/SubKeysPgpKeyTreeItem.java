package org.subshare.gui.user.pgpkeytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.pgp.PgpKey;

public class SubKeysPgpKeyTreeItem extends PgpKeyTreeItem<PgpKey> {

	public SubKeysPgpKeyTreeItem(final PgpKey pgpKey) {
		super(assertNotNull("pgpKey", pgpKey));
		for (final PgpKey subKey : pgpKey.getSubKeys()) {

		}
	}

	@Override
	public String getName() {
		return "Sub-keys";
	}
}
