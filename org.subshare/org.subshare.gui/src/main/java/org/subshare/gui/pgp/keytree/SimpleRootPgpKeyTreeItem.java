package org.subshare.gui.pgp.keytree;

import static java.util.Objects.*;

public class SimpleRootPgpKeyTreeItem extends PgpKeyTreeItem<String> {

	private final PgpKeyTreePane pgpKeyTreePane;

	public SimpleRootPgpKeyTreeItem(final PgpKeyTreePane pgpKeyTreePane) {
		super("");
		this.pgpKeyTreePane = requireNonNull(pgpKeyTreePane, "pgpKeyTreePane");
	}

	@Override
	protected PgpKeyTreePane getPgpKeyTreePane() {
		return pgpKeyTreePane;
	}
}