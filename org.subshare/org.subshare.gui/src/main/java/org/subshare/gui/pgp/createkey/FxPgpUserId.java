package org.subshare.gui.pgp.createkey;

import static co.codewizards.cloudstore.core.util.StringUtil.*;
import javafx.beans.property.StringProperty;
import javafx.beans.property.adapter.JavaBeanStringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;

import org.subshare.core.pgp.PgpUserId;

public class FxPgpUserId extends PgpUserId {
	private static final long serialVersionUID = 1L;

	private transient JavaBeanStringProperty nameProperty;
	private transient JavaBeanStringProperty emailProperty;

	public FxPgpUserId() {
		this((String) null);
	}

	public FxPgpUserId(final String userIdString) {
		super(userIdString);
	}

	public FxPgpUserId(final String name, final String email) {
		setName(name);
		setEmail(email);
	}

	public FxPgpUserId(PgpUserId other) {
		super(other);
	}

	public synchronized StringProperty nameProperty() {
		if (nameProperty == null) {
			try {
				nameProperty = JavaBeanStringPropertyBuilder.create().bean(this).name(PropertyEnum.name.name()).build();
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}
		return nameProperty;
	}

	public synchronized StringProperty emailProperty() {
		if (emailProperty == null) {
			try {
				emailProperty = JavaBeanStringPropertyBuilder.create().bean(this).name(PropertyEnum.email.name()).build();
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}
		return emailProperty;
	}
}
