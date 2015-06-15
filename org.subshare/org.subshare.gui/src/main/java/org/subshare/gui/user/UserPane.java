package org.subshare.gui.user;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.Collections;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

import org.subshare.core.user.User;

public class UserPane extends GridPane {

	private final EditUserManager editUserManager;
	private final User user;

	public UserPane(final EditUserManager editUserManager, final User user) {
		this.editUserManager = assertNotNull("editUserManager", editUserManager);
		this.user = assertNotNull("user", user);
		loadDynamicComponentFxml(UserPane.class, this);
	}

	@FXML
	private void closeButtonClicked(final ActionEvent event) {
		editUserManager.endEditing(Collections.singleton(user));
	}
}
