package org.subshare.gui.userlist;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpKeyTrustLevel;
import org.subshare.core.user.User;
import org.subshare.gui.ls.PgpLs;

public class UserListItem {

	private static final String NULL = "null";
	private final Pgp pgp = PgpLs.getPgpOrFail();

	private final User user;

	private volatile String firstName;
	private volatile String lastName;
	private volatile String email;
	private volatile String keyTrustLevel;

	public UserListItem(final User user) {
		this.user = user;
		user.addPropertyChangeListener(userPropertyChangeListener);
		pgp.addPropertyChangeListener(pgpPropertyChangeListener);
	}

	@Override
	protected void finalize() throws Throwable {
		user.removePropertyChangeListener(userPropertyChangeListener);
		pgp.removePropertyChangeListener(pgpPropertyChangeListener);
		super.finalize();
	}

	private final PropertyChangeListener userPropertyChangeListener = new UserPropertyChangeListener(this);

	private final PropertyChangeListener pgpPropertyChangeListener = new PgpPropertyChangeListener(this);

	private static class UserPropertyChangeListener implements PropertyChangeListener {
		private final WeakReference<UserListItem> userListItemRef;

		public UserPropertyChangeListener(final UserListItem userListItem) {
			userListItemRef = new WeakReference<UserListItem>(assertNotNull("userListItem", userListItem));
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			final UserListItem userListItem = userListItemRef.get();
			if (userListItem != null) {
				userListItem.firstName = null;
				userListItem.lastName = null;
				userListItem.email = null;
			}
		}
	}

	private static class PgpPropertyChangeListener implements PropertyChangeListener {
		private final WeakReference<UserListItem> userListItemRef;

		public PgpPropertyChangeListener(final UserListItem userListItem) {
			userListItemRef = new WeakReference<UserListItem>(assertNotNull("userListItem", userListItem));
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			final UserListItem userListItem = userListItemRef.get();
			if (userListItem != null)
				userListItem.keyTrustLevel = null;
		}
	}

	/**
	 * Gets the {@code User} associated with this {@code UserListItem}.
	 * <p>
	 * Note: This is a proxy. The real object lives in the {@code LocalServer}, hence we cache all the individual
	 * fields here in this object (there may be really many users).
	 * @return the {@code User} associated with this {@code UserListItem}. Never <code>null</code>.
	 */
	public User getUser() {
		return user;
	}

	public String getFirstName() {
		String firstName = this.firstName;
		if (firstName == null) {
			firstName = assertNotNull("user", user).getFirstName();
			this.firstName = firstName == null ? NULL : firstName;
		}
		return firstName == NULL ? null : firstName;
	}

	public String getLastName() {
		String lastName = this.lastName;
		if (lastName == null) {
			lastName = assertNotNull("user", user).getLastName();
			this.lastName = lastName == null ? NULL : lastName;
		}
		return lastName == NULL ? null : lastName;
	}

	public String getEmail() {
		String email = this.email;
		if (email == null) {
			if (assertNotNull("user", user).getEmails().isEmpty())
				email = NULL;
			else {
				final String firstEmail = user.getEmails().get(0);
				if (user.getEmails().size() == 1)
					email = firstEmail;
				else
					email = String.format("%s (%s more)", firstEmail, user.getEmails().size() - 1);
			}
			this.email = email;
		}
		return NULL == email ? null : email;
	}

	public String getKeyTrustLevel() {
		String keyTrustLevel = this.keyTrustLevel;
		if (keyTrustLevel == null) { // TODO we need a mechanism to invalidate this cached value - maybe a listener in Pgp?
			PgpKeyTrustLevel highestKeyTrustLevel = null;
			for (final PgpKeyId pgpKeyId : user.getPgpKeyIds()) {
				final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
				if (pgpKey != null) {
					final PgpKeyTrustLevel ktl = pgp.getKeyTrustLevel(pgpKey);
					if (highestKeyTrustLevel == null || ktl.compareTo(highestKeyTrustLevel) > 0)
						highestKeyTrustLevel = ktl;
				}
			}
			this.keyTrustLevel = keyTrustLevel = highestKeyTrustLevel == null ? null : highestKeyTrustLevel.toString();
		}
		return keyTrustLevel;
	}
}
