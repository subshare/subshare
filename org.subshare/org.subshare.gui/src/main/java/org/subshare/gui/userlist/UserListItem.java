package org.subshare.gui.userlist;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.adapter.JavaBeanStringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpKeyTrustLevel;
import org.subshare.core.user.User;
import org.subshare.gui.ls.PgpLs;

public class UserListItem {

	private static final String NULL = "null";

	private final User user;
	private Pgp pgp;

	private volatile String firstName;
	private final JavaBeanStringProperty firstNameProperty;
	private volatile String lastName;
	private volatile List<String> emails;
	private volatile String email;
	private volatile String keyTrustLevel;

	public UserListItem(final User user) {
		this.user = assertNotNull("user", user);
		addWeakPropertyChangeListener(user, userPropertyChangeListener);
		try {
			firstNameProperty = JavaBeanStringPropertyBuilder.create().bean(user).name(User.PropertyEnum.firstName.name()).build();
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
//		user.removePropertyChangeListener(userPropertyChangeListener);
//
//		if (pgp != null)
//			pgp.removePropertyChangeListener(pgpPropertyChangeListener);

		super.finalize();
	}

	protected synchronized Pgp getPgp() {
		if (pgp == null) {
			pgp = PgpLs.getPgpOrFail();
			addWeakPropertyChangeListener(pgp, pgpPropertyChangeListener);
		}
		return pgp;
	}

	private final PropertyChangeListener userPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			firstName = null;
			lastName = null;
			email = null;
		}
	};

	private final PropertyChangeListener pgpPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			keyTrustLevel = null;
		}
	};

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

	// TODO rewrite this all to JavaBeanStringProperty instances!
	public String getFirstName() {
		String firstName = this.firstName;
		if (firstName == null) {
			firstName = assertNotNull("user", user).getFirstName();
			this.firstName = firstName == null ? NULL : firstName;
		}
		return firstName == NULL ? null : firstName;
	}
	public JavaBeanStringProperty firstNameProperty() {
		return firstNameProperty;
	}

	public String getLastName() {
		String lastName = this.lastName;
		if (lastName == null) {
			lastName = assertNotNull("user", user).getLastName();
			this.lastName = lastName == null ? NULL : lastName;
		}
		return lastName == NULL ? null : lastName;
	}

	public List<String> getEmails() {
		List<String> emails = this.emails;
		if (emails == null)
			this.emails = emails = new ArrayList<>(assertNotNull("user", user).getEmails());

		return emails;
	}

	public String getEmail() {
		String email = this.email;
		if (email == null) {
			if (getEmails().isEmpty())
				email = NULL;
			else {
				final String firstEmail = getEmails().get(0);
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
				final PgpKey pgpKey = getPgp().getPgpKey(pgpKeyId);
				if (pgpKey != null) {
					final PgpKeyTrustLevel ktl = getPgp().getKeyTrustLevel(pgpKey);
					if (highestKeyTrustLevel == null || ktl.compareTo(highestKeyTrustLevel) > 0)
						highestKeyTrustLevel = ktl;
				}
			}
			this.keyTrustLevel = keyTrustLevel = highestKeyTrustLevel == null ? null : highestKeyTrustLevel.toString();
		}
		return keyTrustLevel;
	}

	public boolean matchesFilter(final String filterText) {
		if (filterText.isEmpty())
			return true;

		final String firstName = this.getFirstName();
		if (firstName != null && firstName.toLowerCase().contains(filterText))
			return true;

		final String lastName = this.getLastName();
		if (lastName != null && lastName.toLowerCase().contains(filterText))
			return true;

		for (final String email : this.getEmails()) {
			if (email.toLowerCase().contains(filterText))
				return true;
		}

		return false;
	}
}
