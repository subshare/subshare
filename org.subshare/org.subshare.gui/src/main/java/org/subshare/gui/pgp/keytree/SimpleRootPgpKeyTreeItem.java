package org.subshare.gui.pgp.keytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

public class SimpleRootPgpKeyTreeItem extends PgpKeyTreeItem<String> {

	private final PgpKeyTreePane pgpKeyTreePane;

	public SimpleRootPgpKeyTreeItem(final PgpKeyTreePane pgpKeyTreePane) {
		super("");
		this.pgpKeyTreePane = assertNotNull(pgpKeyTreePane, "pgpKeyTreePane");
	}

	@Override
	protected PgpKeyTreePane getPgpKeyTreePane() {
		return pgpKeyTreePane;
	}
}