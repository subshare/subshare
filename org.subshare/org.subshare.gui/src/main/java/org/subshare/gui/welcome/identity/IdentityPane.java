package org.subshare.gui.welcome.identity;

import static java.util.Objects.*;
import static org.subshare.gui.util.FxmlUtil.*;

import org.subshare.gui.welcome.IdentityData;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

public class IdentityPane extends GridPane {

	private final IdentityData identityData;

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

	public IdentityPane(final IdentityData identityData) {
		this.identityData = requireNonNull(identityData, "identityData");
		loadDynamicComponentFxml(IdentityPane.class, this);

		firstNameTextField.textProperty().bindBidirectional(identityData.firstNameProperty());
		lastNameTextField.textProperty().bindBidirectional(identityData.lastNameProperty());
		emailTextField.textProperty().bindBidirectional(identityData.getPgpUserId().emailProperty());
		importBackupCheckBox.selectedProperty().bindBidirectional(identityData.importBackupProperty());

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
