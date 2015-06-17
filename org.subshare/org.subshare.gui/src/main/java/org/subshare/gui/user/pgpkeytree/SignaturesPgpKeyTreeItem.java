package org.subshare.gui.user.pgpkeytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpSignature;
import org.subshare.gui.ls.PgpLs;

public class SignaturesPgpKeyTreeItem extends PgpKeyTreeItem<PgpKey> {

	private final String userId;
	private boolean childrenInitialised;

	public SignaturesPgpKeyTreeItem(final PgpKey pgpKey, final String userId) {
		super(assertNotNull("pgpKey", pgpKey));
		this.userId = assertNotNull("userId", userId);
	}

	@Override
	public ObservableList<TreeItem<PgpKeyTreeItem<?>>> getChildren() {
		final ObservableList<TreeItem<PgpKeyTreeItem<?>>> children = super.getChildren();
		if (! childrenInitialised) {
			childrenInitialised = true;
			final Pgp pgp = PgpLs.getPgpOrFail();
			final PgpKey pgpKey = getValueObject();
			final Collection<PgpSignature> signatures = pgp.getUserIdSignatures(pgpKey);
			for (PgpSignature signature : signatures) {
				if (userId.equals(signature.getUserId()))
					getChildren().add(new SignaturePgpKeyTreeItem(pgp, signature));
			}
		}
		return children;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public String getName() {
		return "Signatures";
	}
}
