package org.subshare.gui.welcome.createpgpkey.passphrase;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Arrays;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.adapter.JavaBeanObjectProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.gui.util.CharArrayStringConverter;
import org.subshare.gui.welcome.WelcomeData;

public abstract class PassphrasePane extends GridPane {

	private final WelcomeData welcomeData;

	@FXML
	private Label passwordLabel;

	@FXML
	private PasswordField passwordField;
	private final JavaBeanObjectProperty<char[]> passwordProperty;

	@FXML
	private CheckBox noPasswordCheckBox;

	@FXML
	private Label passwordLabel2;

	@FXML
	private PasswordField passwordField2;

	private final InvalidationListener updateDisabledInvalidationListener = (InvalidationListener) observable -> updateComplete();

	public PassphrasePane(final WelcomeData welcomeData) {
		this.welcomeData = assertNotNull("welcomeData", welcomeData);
		loadDynamicComponentFxml(PassphrasePane.class, this);

		passwordProperty = createPasswordProperty();
		Bindings.bindBidirectional(
				passwordField.textProperty(), passwordProperty, new CharArrayStringConverter());
		passwordProperty.addListener(updateDisabledInvalidationListener);
		passwordField2.textProperty().addListener(updateDisabledInvalidationListener);

		noPasswordCheckBox.selectedProperty().addListener(updateDisabledInvalidationListener);
		passwordField.disableProperty().bind(noPasswordCheckBox.selectedProperty());
		passwordField2.disableProperty().bind(noPasswordCheckBox.selectedProperty());
		passwordLabel.disableProperty().bind(noPasswordCheckBox.selectedProperty());
		passwordLabel2.disableProperty().bind(noPasswordCheckBox.selectedProperty());

		updateComplete();
	}

	@SuppressWarnings("unchecked")
	private JavaBeanObjectProperty<char[]> createPasswordProperty() {
		try {
			return JavaBeanObjectPropertyBuilder.create()
					.bean(welcomeData.getCreatePgpKeyParam())
					.name(CreatePgpKeyParam.PropertyEnum.passphrase.name())
					.build();
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	protected boolean isComplete() {
		boolean complete = true;

		if (! noPasswordCheckBox.isSelected()) {
			final char[] p1 = welcomeData.getCreatePgpKeyParam().getPassphrase();
			final char[] p2 = passwordField2.getText().toCharArray();
			complete &= Arrays.equals(p1, p2);

			if (complete && p1.length == 0)
				complete = false;
		}

		return complete;
	}

	protected abstract void updateComplete();
}
