package org.subshare.gui.pgp.createkey;

import static co.codewizards.cloudstore.core.util.StringUtil.*;
import javafx.beans.property.SimpleStringProperty;

public class UserId {

	private final SimpleStringProperty nameProperty = new SimpleStringProperty();
	private final SimpleStringProperty emailProperty = new SimpleStringProperty();

	public UserId() {
	}

	public UserId(final String name, final String email) {
		nameProperty.set(name);
		emailProperty.set(email);
	}

	public SimpleStringProperty nameProperty() {
		return nameProperty;
	}

	public SimpleStringProperty emailProperty() {
		return emailProperty;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		final String name = trim(nameProperty.get());
		final String email = trim(emailProperty.get());

		if (! isEmpty(name))
			sb.append(name);

		if (! isEmpty(email)) {
			if (sb.length() > 0)
				sb.append(' ');

			sb.append('<').append(email).append('>');
		}
		return sb.toString();
	}
}
