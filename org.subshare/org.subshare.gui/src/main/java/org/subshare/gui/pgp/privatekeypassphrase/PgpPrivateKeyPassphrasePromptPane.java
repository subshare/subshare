package org.subshare.gui.pgp.privatekeypassphrase;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import org.subshare.core.pgp.PgpKey;

public abstract class PgpPrivateKeyPassphrasePromptPane extends GridPane {

	private final PgpKey pgpKey;

	@FXML
	private HBox errorMessageBox;

	@FXML
	private Label errorMessageLabel;

	@FXML
	private Label headerLabel;

	@FXML
	private ComboBox<String> userIdsComboBox;

	@FXML
	private TextField keyIdTextField;

	@FXML
	private PasswordField passwordField;

	@FXML
	private Button okButton;

	@FXML
	private Button cancelButton;

	public PgpPrivateKeyPassphrasePromptPane(final PgpKey pgpKey, final String errorMessage) {
		loadDynamicComponentFxml(PgpPrivateKeyPassphrasePromptPane.class, this);
		this.pgpKey = assertNotNull("pgpKey", pgpKey);
		userIdsComboBox.setItems(FXCollections.observableArrayList(this.pgpKey.getUserIds()));
		userIdsComboBox.getSelectionModel().select(0);
		keyIdTextField.setText(this.pgpKey.getPgpKeyId().toHumanString());

		getChildren().remove(errorMessageBox);
		if (errorMessage == null) {
//			errorMessageBox.setVisible(false);
//			getRowConstraints().add(0, new RowConstraints(0, 0, 0));
		}
		else {
			add(errorMessageBox, 0, 0);
			GridPane.setColumnSpan(errorMessageBox, 2);
//			errorMessageBox.setVisible(true);
			errorMessageLabel.setText(errorMessage);
		}
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		passwordField.requestFocus();
	}

	public char[] getPassphrase() {
		return passwordField.getText().toCharArray();
	}

	@FXML
	protected abstract void okButtonClicked(final ActionEvent event);

	@FXML
	protected abstract void cancelButtonClicked(final ActionEvent event);
}
