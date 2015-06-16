package org.subshare.gui.user;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class EmailWrapper {

	private final StringProperty valueProperty = new SimpleStringProperty() {
		@Override
		public void set(final String newValue) {
			super.set(newValue == null ? "" : newValue);
		}
	};

	public EmailWrapper(String value) {
		this.valueProperty.set(value);
	}

	public StringProperty valueProperty() {
		return valueProperty;
	}

	public String getValue() {
		return valueProperty.get();
	}

	public void setValue(String value) {
		this.valueProperty.set(value);
	}
}
