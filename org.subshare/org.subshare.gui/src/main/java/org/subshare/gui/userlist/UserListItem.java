package org.subshare.gui.userlist;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpKeyValidity;
import org.subshare.core.pgp.PgpOwnerTrust;
import org.subshare.core.user.User;
import org.subshare.gui.ls.PgpLs;

public class UserListItem {

	private final User user;
	private Pgp pgp;

	private final StringProperty firstName = new SimpleStringProperty(this, "firstName");
	private final StringProperty lastName = new SimpleStringProperty(this, "lastName");
	private final StringProperty email = new SimpleStringProperty(this, "email");
	private final StringProperty keyValidityProperty = new SimpleStringProperty(this, "keyValidity");
	private final StringProperty ownerTrustProperty = new SimpleStringProperty(this, "ownerTrust");

	private volatile List<String> emails;
	private volatile String keyValidity;
	private volatile String ownerTrust;

	public UserListItem(final User user) {
		this.user = assertNotNull("user", user);
		addWeakPropertyChangeListener(user, userPropertyChangeListener);

		// We do *not* use JavaBeanStringProperty, but SimpleStringProperty instead, because our User bean is a proxy.
		// Since there are *many* Users (and thus UserListItems), it would be much slower to use a JavaBeanStringProperty,
		// which delegates every get() invocation to the underlying bean getter - implying an RPC invocation!
//		firstName = new SimpleStringProperty(user, User.PropertyEnum.firstName.name());
//		lastName = new SimpleStringProperty(user, User.PropertyEnum.lastName.name());
//		email = new SimpleStringProperty();
//		keyValidityProperty = new SimpleStringProperty();

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
				keyValidity = null; // clear cache
				ownerTrust = null; // clear cache
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
		keyValidityProperty.set(getKeyValidity());
		ownerTrustProperty.set(getOwnerTrust());
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
	public StringProperty keyValidityProperty() {
		return keyValidityProperty;
	}
	public StringProperty ownerTrustProperty() {
		return ownerTrustProperty;
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

	public String getKeyValidity() {
		String keyValidity = this.keyValidity;
		if (keyValidity == null) {
			final SortedSet<PgpKeyValidity> keyValidities = new TreeSet<>(Collections.reverseOrder());
			for (final PgpKeyId pgpKeyId : user.getPgpKeyIds()) {
				final PgpKey pgpKey = getPgp().getPgpKey(pgpKeyId);
				if (pgpKey != null) {
					final PgpKeyValidity v = getPgp().getKeyValidity(pgpKey);
					keyValidities.add(v);
				}
			}

//			if (keyValidities.isEmpty())
//				keyValidities.add(PgpKeyValidity.NOT_TRUSTED);

			StringBuilder sb = new StringBuilder();
			for (PgpKeyValidity kv : keyValidities) {
				if (sb.length() > 0)
					sb.append(", ");

				sb.append(kv.toShortString());
			}

			this.keyValidity = keyValidity = sb.toString();
		}
		return keyValidity;
	}

	public String getOwnerTrust() {
		String ownerTrust = this.ownerTrust;
		if (ownerTrust == null) {
			final SortedSet<PgpOwnerTrust> ownerTrusts = new TreeSet<>(Collections.reverseOrder());
			for (final PgpKeyId pgpKeyId : user.getPgpKeyIds()) {
				final PgpKey pgpKey = getPgp().getPgpKey(pgpKeyId);
				if (pgpKey != null) {
					final PgpOwnerTrust ot = getPgp().getOwnerTrust(pgpKey);
					ownerTrusts.add(ot);
				}
			}

			if (ownerTrusts.isEmpty())
				ownerTrusts.add(PgpOwnerTrust.UNKNOWN);

			StringBuilder sb = new StringBuilder();
			for (PgpOwnerTrust ot : ownerTrusts) {
				if (sb.length() > 0)
					sb.append(", ");

				sb.append(ot.toShortString());
			}

			this.ownerTrust = ownerTrust = sb.toString();
		}
		return ownerTrust;
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
