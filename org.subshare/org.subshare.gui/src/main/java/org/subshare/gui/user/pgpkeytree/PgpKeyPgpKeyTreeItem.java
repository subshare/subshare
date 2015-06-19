package org.subshare.gui.user.pgpkeytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.text.DateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyFlag;

public class PgpKeyPgpKeyTreeItem extends PgpKeyTreeItem<PgpKey> {

	public PgpKeyPgpKeyTreeItem(final PgpKey pgpKey) {
		super(assertNotNull("pgpKey", pgpKey));
	}

	public PgpKey getPgpKey() {
		return getValueObject();
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
		final List<String> userIds = getPgpKey().getUserIds();
		return userIds.isEmpty() ? getKeyId() : userIds.get(0);
	}

	@Override
	public String getKeyId() {
		return getPgpKey().getPgpKeyId().toHumanString();
	}

	@Override
	public String getCreated() {
		final PgpKey pgpKey = getPgpKey();
		final Date created = pgpKey.getCreated();
		if (created == null)
			return null; // should never happen - but we shouldn't throw an NPE in the UI, if it ever does.

		return DateFormat.getDateInstance(DateFormat.SHORT).format(created);
	}

	@Override
	public String getValidTo() {
		final PgpKey pgpKey = getPgpKey();
		final Date validTo = pgpKey.getValidTo();
		if (validTo == null)
			return null;

		return DateFormat.getDateInstance(DateFormat.SHORT).format(validTo);
	}

	@Override
	public String getUsage() {
		final Set<PgpKeyFlag> allPgpKeyFlags = EnumSet.noneOf(PgpKeyFlag.class);

		final PgpKey pgpKey = getPgpKey();
		allPgpKeyFlags.addAll(pgpKey.getPgpKeyFlags());
		for (final PgpKey subKey : pgpKey.getSubKeys())
			allPgpKeyFlags.addAll(subKey.getPgpKeyFlags());

		return new PgpKeyFlagsToUsageConverter().toUsage(allPgpKeyFlags);
	}
}
