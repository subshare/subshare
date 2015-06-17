package org.subshare.gui.user.pgpkeytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.pgp.PgpKey;

public class SubKeyPgpKeyTreeItem extends PgpKeyTreeItem<PgpKey> {

	public SubKeyPgpKeyTreeItem(final PgpKey subKey) {
		super(assertNotNull("subKey", subKey));
	}

	@Override
	public String getName() {
//		final PgpKey subKey = getValueObject();
		return null;
	}

	@Override
	public String getKeyId() {
		final PgpKey subKey = getValueObject();
		return subKey.getPgpKeyId().toHumanString();
	}
}
