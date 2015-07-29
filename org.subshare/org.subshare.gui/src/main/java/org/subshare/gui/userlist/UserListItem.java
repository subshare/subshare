package org.subshare.gui.userlist;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpKeyTrustLevel;
import org.subshare.core.user.User;
import org.subshare.gui.ls.PgpLs;

public class UserListItem {

	private final User user;
	private Pgp pgp;

	private final StringProperty firstName = new SimpleStringProperty(this, "firstName");
	private final StringProperty lastName = new SimpleStringProperty(this, "lastName");
	private final StringProperty email = new SimpleStringProperty(this, "email");
	private final StringProperty keyTrustLevelProperty = new SimpleStringProperty(this, "keyTrustLevel");

	private volatile List<String> emails;
	private volatile String keyTrustLevel;

	public UserListItem(final User user) {
		this.user = assertNotNull("user", user);
		addWeakPropertyChangeListener(user, userPropertyChangeListener);

		// We do *not* use JavaBeanStringProperty, but SimpleStringProperty instead, because our User bean is a proxy.
		// Since there are *many* Users (and thus UserListItems), it would be much slower to use a JavaBeanStringProperty,
		// which delegates every get() invocation to the underlying bean getter - implying an RPC invocation!
//		firstName = new SimpleStringProperty(user, User.PropertyEnum.firstName.name());
//		lastName = new SimpleStringProperty(user, User.PropertyEnum.lastName.name());
//		email = new SimpleStringProperty();
//		keyTrustLevelProperty = new SimpleStringProperty();

		copyDataFromUser();
		copyDataFromPgp();
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
			Platform.runLater(() -> {
				emails = null; // clear cache
				copyDataFromUser();
			});
		}
	};

	private final PropertyChangeListener pgpPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			Platform.runLater(() -> {
				keyTrustLevel = null; // clear cache
				copyDataFromPgp();
			});
		}
	};

	protected void copyDataFromUser() {
		firstName.set(user.getFirstName());
		lastName.set(user.getLastName());
		email.set(getEmail());
	}

	protected void copyDataFromPgp() {
		keyTrustLevelProperty.set(getKeyTrustLevel());
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

	public StringProperty firstNameProperty() {
		return firstName;
	}
	public String getFirstName() {
		return firstName.get();
	}

	public StringProperty lastNameProperty() {
		return lastName;
	}
	public String getLastName() {
		return lastName.get();
	}

	public StringProperty emailProperty() {
		return email;
	}
	public StringProperty keyTrustLevelProperty() {
		return keyTrustLevelProperty;
	}

	public List<String> getEmails() {
		List<String> emails = this.emails;
		if (emails == null)
			this.emails = emails = new ArrayList<>(assertNotNull("user", user).getEmails());

		return emails;
	}

	private String getEmail() {
		if (getEmails().isEmpty())
			return null;
		else {
			final String firstEmail = getEmails().get(0);
			if (user.getEmails().size() == 1)
				return firstEmail;
			else
				return String.format("%s (%s more)", firstEmail, user.getEmails().size() - 1);
		}
	}

	public String getKeyTrustLevel() {
		String keyTrustLevel = this.keyTrustLevel;
		if (keyTrustLevel == null) {
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

		final String firstName = this.firstName.get();
		if (firstName != null && firstName.toLowerCase().contains(filterText))
			return true;

		final String lastName = this.lastName.get();
		if (lastName != null && lastName.toLowerCase().contains(filterText))
			return true;

		for (final String email : this.getEmails()) {
			if (email.toLowerCase().contains(filterText))
				return true;
		}

		return false;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}
}
