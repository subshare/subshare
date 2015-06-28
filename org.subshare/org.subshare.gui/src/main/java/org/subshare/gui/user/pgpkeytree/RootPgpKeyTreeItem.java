package org.subshare.gui.user.pgpkeytree;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javafx.application.Platform;
import javafx.scene.control.TreeTableView;

import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.user.User;

public class RootPgpKeyTreeItem extends PgpKeyTreeItem<User> {

	private final TreeTableView<PgpKeyTreeItem<?>> treeTableView;

	private final PropertyChangeListener userPgpKeyIdsPropertyChangeListener = event -> Platform.runLater(() -> updatePgpKeyChildren());

	private final Map<PgpKeyId, PgpKeyPgpKeyTreeItem> pgpKeyId2PgpKeyPgpKeyTreeItem = new HashMap<>();

	public RootPgpKeyTreeItem(final TreeTableView<PgpKeyTreeItem<?>> treeTableView, final User user) {
		super(assertNotNull("user", user));
		this.treeTableView = assertNotNull("treeTableView", treeTableView);

		addWeakPropertyChangeListener(user, User.PropertyEnum.pgpKeyIds, userPgpKeyIdsPropertyChangeListener);
		updatePgpKeyChildren();
	}

	@Override
	protected TreeTableView<PgpKeyTreeItem<?>> getTreeTableView() {
		return treeTableView;
	}

	private void updatePgpKeyChildren() {
		final User user = getValueObject();

		final Set<PgpKeyId> pgpKeyIds = new HashSet<>();
		for (final PgpKey pgpKey : user.getPgpKeys()) {
			final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
			pgpKeyIds.add(pgpKeyId);

			if (! pgpKeyId2PgpKeyPgpKeyTreeItem.containsKey(pgpKeyId)) {
				final PgpKeyPgpKeyTreeItem child = new PgpKeyPgpKeyTreeItem(pgpKey);
				pgpKeyId2PgpKeyPgpKeyTreeItem.put(pgpKeyId, child);
				getChildren().add(child);
			}
		}

		for (final Iterator<Map.Entry<PgpKeyId, PgpKeyPgpKeyTreeItem>> it = pgpKeyId2PgpKeyPgpKeyTreeItem.entrySet().iterator(); it.hasNext();) {
			final Map.Entry<PgpKeyId, PgpKeyPgpKeyTreeItem> me = it.next();
			if (! pgpKeyIds.contains(me.getKey())) {
				getChildren().remove(me.getValue());
				it.remove();
			}
		}
	}
}