package org.subshare.gui.welcome.createpgpkey.identity;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

import org.subshare.gui.welcome.WelcomeData;

public class IdentityPane extends GridPane {

	private final WelcomeData welcomeData;

	@FXML
	private Text headerText;

	@FXML
	protected Label firstNameLabel;

	@FXML
	protected TextField firstNameTextField;

	@FXML
	protected Label lastNameLabel;

	@FXML
	protected TextField lastNameTextField;

	@FXML
	protected Label emailLabel;

	@FXML
	protected TextField emailTextField;

	@FXML
	protected CheckBox importBackupCheckBox;

	public IdentityPane(final WelcomeData welcomeData) {
		this.welcomeData = assertNotNull("welcomeData", welcomeData);
		loadDynamicComponentFxml(IdentityPane.class, this);

		firstNameTextField.textProperty().bindBidirectional(welcomeData.firstNameProperty());
		lastNameTextField.textProperty().bindBidirectional(welcomeData.lastNameProperty());
		emailTextField.textProperty().bindBidirectional(welcomeData.getPgpUserId().emailProperty());
		importBackupCheckBox.selectedProperty().bindBidirectional(welcomeData.importBackupProperty());

		firstNameLabel.disableProperty().bind(importBackupCheckBox.selectedProperty());
		firstNameTextField.disableProperty().bind(importBackupCheckBox.selectedProperty());

		lastNameLabel.disableProperty().bind(importBackupCheckBox.selectedProperty());
		lastNameTextField.disableProperty().bind(importBackupCheckBox.selectedProperty());

		emailLabel.disableProperty().bind(importBackupCheckBox.selectedProperty());
		emailTextField.disableProperty().bind(importBackupCheckBox.selectedProperty());

//		// TODO this works, but I don't like this solution, yet (hard-code 32?! why?!) - no it does not yet really work :-(
//		boundsInLocalProperty().addListener(observable -> {
//			headerText.setWrappingWidth(Math.max(getBoundsInLocal().getWidth() - 32, 500));
//		});
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		firstNameTextField.requestFocus();
	}
}
