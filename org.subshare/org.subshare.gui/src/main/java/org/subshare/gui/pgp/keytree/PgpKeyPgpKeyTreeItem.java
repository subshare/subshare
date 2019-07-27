package org.subshare.gui.pgp.keytree;

import static java.util.Objects.*;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyAlgorithm;
import org.subshare.core.pgp.PgpKeyFlag;
import org.subshare.core.pgp.PgpKeyValidity;
import org.subshare.core.pgp.PgpOwnerTrust;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class PgpKeyPgpKeyTreeItem extends PgpKeyTreeItem<PgpKey> {

	public PgpKeyPgpKeyTreeItem(final PgpKey pgpKey) {
		super(requireNonNull(pgpKey, "pgpKey"));
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
	public String getKeyValidity() {
		final PgpKey pgpKey = getPgpKey();
		final PgpKeyValidity kv = getPgp().getKeyValidity(pgpKey);
		return kv.toShortString();
	}

	@Override
	public String getOwnerTrust() {
		final PgpKey pgpKey = getPgpKey();
		final PgpOwnerTrust ot = getPgp().getOwnerTrust(pgpKey);
		return ot.toShortString();
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
	public String getAlgorithm() {
		final Set<PgpKeyAlgorithm> allPgpKeyAlgorithms = new LinkedHashSet<>();

		final PgpKey pgpKey = getPgpKey();
		allPgpKeyAlgorithms.add(pgpKey.getAlgorithm());
		for (final PgpKey subKey : pgpKey.getSubKeys())
			allPgpKeyAlgorithms.add(subKey.getAlgorithm());

		final StringBuilder sb = new StringBuilder();
		for (final PgpKeyAlgorithm algorithm : allPgpKeyAlgorithms) {
			if (sb.length() > 0)
				sb.append(", ");

			sb.append(PgpKeyAlgorithmName.getPgpKeyAlgorithmName(algorithm));
		}

		return sb.toString();
	}

	@Override
	public String getStrength() {
		final Set<Integer> allPgpKeyStrengths = new LinkedHashSet<>();

		final PgpKey pgpKey = getPgpKey();
		allPgpKeyStrengths.add(pgpKey.getStrength());
		for (final PgpKey subKey : pgpKey.getSubKeys())
			allPgpKeyStrengths.add(subKey.getStrength());

		final StringBuilder sb = new StringBuilder();
		for (final Integer strength : allPgpKeyStrengths) {
			if (sb.length() > 0)
				sb.append(", ");

			sb.append(strength);
		}

		return sb.toString();
	}

	@Override
	public String getUsage() {
		final Set<PgpKeyFlag> allPgpKeyFlags = new LinkedHashSet<>();

		final PgpKey pgpKey = getPgpKey();
		allPgpKeyFlags.addAll(pgpKey.getPgpKeyFlags());
		for (final PgpKey subKey : pgpKey.getSubKeys())
			allPgpKeyFlags.addAll(subKey.getPgpKeyFlags());

		return new PgpKeyFlagsToUsageConverter().toUsage(allPgpKeyFlags);
	}
}
