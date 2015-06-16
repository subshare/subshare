package org.subshare.gui.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Collections;

import javafx.beans.property.adapter.JavaBeanStringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import org.subshare.core.user.User;

public class UserPane extends GridPane {

	private final EditUserManager editUserManager;
	private final User user;

	private final JavaBeanStringProperty firstNameProperty;
	private final JavaBeanStringProperty lastNameProperty;

	@FXML
	private TextField firstNameTextField;

	@FXML
	private TextField lastNameTextField;

	public UserPane(final EditUserManager editUserManager, final User user) {
		this.editUserManager = assertNotNull("editUserManager", editUserManager);
		this.user = assertNotNull("user", user);
		loadDynamicComponentFxml(UserPane.class, this);

		try {
			firstNameProperty = JavaBeanStringPropertyBuilder.create()
				    .bean(user)
				    .name(User.PropertyEnum.firstName.name())
				    .build();
			firstNameTextField.textProperty().bindBidirectional(firstNameProperty);

			lastNameProperty = JavaBeanStringPropertyBuilder.create()
				    .bean(user)
				    .name(User.PropertyEnum.lastName.name())
				    .build();
			lastNameTextField.textProperty().bindBidirectional(lastNameProperty);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@FXML
	private void closeButtonClicked(final ActionEvent event) {
		editUserManager.endEditing(Collections.singleton(user));
	}
}
