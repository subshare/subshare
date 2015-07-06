package org.subshare.gui.welcome.createpgpkey.identity;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import org.subshare.gui.welcome.WelcomeData;

public class IdentityPane extends GridPane {

	private final WelcomeData welcomeData;

	@FXML
	protected Label nameLabel;

	@FXML
	protected TextField nameTextField;

	@FXML
	protected Label emailLabel;

	@FXML
	protected TextField emailTextField;

	@FXML
	protected CheckBox importBackupCheckBox;

	public IdentityPane(final WelcomeData welcomeData) {
		this.welcomeData = assertNotNull("welcomeData", welcomeData);
		loadDynamicComponentFxml(IdentityPane.class, this);

		nameTextField.textProperty().bindBidirectional(welcomeData.getPgpUserId().nameProperty());
		emailTextField.textProperty().bindBidirectional(welcomeData.getPgpUserId().emailProperty());
		importBackupCheckBox.selectedProperty().bindBidirectional(welcomeData.importBackupProperty());

		nameLabel.disableProperty().bind(importBackupCheckBox.selectedProperty());
		nameTextField.disableProperty().bind(importBackupCheckBox.selectedProperty());
		emailLabel.disableProperty().bind(importBackupCheckBox.selectedProperty());
		emailTextField.disableProperty().bind(importBackupCheckBox.selectedProperty());
	}
}
