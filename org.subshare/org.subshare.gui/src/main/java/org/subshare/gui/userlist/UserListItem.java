package org.subshare.gui.userlist;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyTrustLevel;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.user.User;

public class UserListItem {

	private final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();

	private User user;

	public UserListItem() { }

	public UserListItem(final User user) {
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	public void setUser(final User user) {
		this.user = user;
	}

	public String getFirstName() {
		return assertNotNull("user", user).getFirstName();
	}

	public String getLastName() {
		return assertNotNull("user", user).getLastName();
	}

	public String getEmail() {
		if (assertNotNull("user", user).getEmails().isEmpty())
			return null;

		final String firstEmail = user.getEmails().get(0);
		if (user.getEmails().size() == 1)
			return firstEmail;

		return String.format("%s (%s more)", firstEmail, user.getEmails().size() - 1);
	}

	public String getKeyTrustLevel() {
		PgpKeyTrustLevel highestKeyTrustLevel = null;
		for (final Long pgpKeyId : user.getPgpKeyIds()) {
			final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
			if (pgpKey != null) {
				final PgpKeyTrustLevel ktl = pgp.getKeyTrustLevel(pgpKey);
				if (highestKeyTrustLevel == null || ktl.compareTo(highestKeyTrustLevel) > 0)
					highestKeyTrustLevel = ktl;
			}
		}
		return highestKeyTrustLevel == null ? null : highestKeyTrustLevel.toString();
	}

}
