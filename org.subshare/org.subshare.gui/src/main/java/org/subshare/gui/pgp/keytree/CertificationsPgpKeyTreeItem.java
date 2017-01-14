package org.subshare.gui.pgp.keytree;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static javafx.application.Platform.*;
import static org.subshare.gui.util.PlatformUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpSignature;
import org.subshare.gui.ls.PgpLs;

import co.codewizards.cloudstore.core.bean.WeakPropertyChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class CertificationsPgpKeyTreeItem extends PgpKeyTreeItem<PgpKey> {

	private final String userId;
	private boolean childrenInitialised;

	private final PropertyChangeListener pgpPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			runLater(() -> updateChildren());
		}
	};
	private WeakPropertyChangeListener pgpWeakPropertyChangeListener;

	private void updateChildren() {
		if (childrenInitialised) {
			childrenInitialised = false;
			super.getChildren().clear();

			if (isExpanded())
				this.getChildren();
			else
				unhookPgpPropertyChangeListener();
		}
	}

	public CertificationsPgpKeyTreeItem(final PgpKey pgpKey, final String userId) {
		super(assertNotNull(pgpKey, "pgpKey"));
		this.userId = assertNotNull(userId, "userId");
	}

	@Override
	public ObservableList<TreeItem<PgpKeyTreeItem<?>>> getChildren() {
		final ObservableList<TreeItem<PgpKeyTreeItem<?>>> children = super.getChildren();
		if (! childrenInitialised) {
			childrenInitialised = true;
			hookPgpPropertyChangeListener();
			final Pgp pgp = PgpLs.getPgpOrFail();
			final PgpKey pgpKey = getValueObject();
			final Collection<PgpSignature> signatures = pgp.getCertifications(pgpKey);
			for (PgpSignature signature : signatures) {
			    // There might be certifications for the entire key (userId == null), too!
				if (signature.getUserId() == null || userId.equals(signature.getUserId()))
					getChildren().add(new CertificationPgpKeyTreeItem(pgp, signature));
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
		return "Certifications";
	}

	@Override
	protected void onPgpInvalidated() {
		if (unhookPgpPropertyChangeListener())
			hookPgpPropertyChangeListener();

		super.onPgpInvalidated();
	}

	protected void hookPgpPropertyChangeListener() {
		assertFxApplicationThread();
		if (pgpWeakPropertyChangeListener == null) {
			final Pgp pgp = assertNotNull(getPgp(), "pgp");
			pgpWeakPropertyChangeListener = addWeakPropertyChangeListener(pgp, Pgp.PropertyEnum.localRevision, pgpPropertyChangeListener);
		}
	}

	protected boolean unhookPgpPropertyChangeListener() {
		assertFxApplicationThread();
		if (pgpWeakPropertyChangeListener != null) {
			pgpWeakPropertyChangeListener.removePropertyChangeListener();
			pgpWeakPropertyChangeListener = null;
			return true;
		}
		return false;
	}
}
