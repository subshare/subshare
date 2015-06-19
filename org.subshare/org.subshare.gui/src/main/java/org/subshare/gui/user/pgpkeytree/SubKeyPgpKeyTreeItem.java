package org.subshare.gui.user.pgpkeytree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.text.DateFormat;
import java.util.Date;

import org.subshare.core.pgp.PgpKey;

public class SubKeyPgpKeyTreeItem extends PgpKeyTreeItem<PgpKey> {

	public SubKeyPgpKeyTreeItem(final PgpKey subKey) {
		super(assertNotNull("subKey", subKey)); //$NON-NLS-1$
	}

	@Override
	public String getName() {
		final PgpKey subKey = getValueObject();
		if (subKey.getMasterKey() == subKey)
			return Messages.getString("SubKeyPgpKeyTreeItem.name[masterKey]"); //$NON-NLS-1$
		else
			return Messages.getString("SubKeyPgpKeyTreeItem.name[subKey]"); //$NON-NLS-1$
	}

	@Override
	public String getKeyId() {
		final PgpKey subKey = getValueObject();
		return subKey.getPgpKeyId().toHumanString();
	}

	@Override
	public String getCreated() {
		final PgpKey subKey = getValueObject();
		final Date created = subKey.getCreated();
		if (created == null)
			return null; // should never happen - but we shouldn't throw an NPE in the UI, if it ever does.

		return DateFormat.getDateInstance(DateFormat.SHORT).format(created);
	}

	@Override
	public String getValidTo() {
		final PgpKey subKey = getValueObject();
		final Date validTo = subKey.getValidTo();
		if (validTo == null)
			return null;

		return DateFormat.getDateInstance(DateFormat.SHORT).format(validTo);
	}

	@Override
	public String getUsage() {
		final PgpKey subKey = getValueObject();
		return new PgpKeyFlagsToUsageConverter().toUsage(subKey.getPgpKeyFlags());
	}
}
