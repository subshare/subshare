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

import org.subshare.core.pgp.PgpKey;

public abstract class PgpPrivateKeyPassphrasePromptPane extends GridPane {

	private final PgpKey pgpKey;

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

	public PgpPrivateKeyPassphrasePromptPane(final PgpKey pgpKey) {
		loadDynamicComponentFxml(PgpPrivateKeyPassphrasePromptPane.class, this);
		this.pgpKey = assertNotNull("pgpKey", pgpKey);
		userIdsComboBox.setItems(FXCollections.observableArrayList(this.pgpKey.getUserIds()));
		userIdsComboBox.getSelectionModel().select(0);
		keyIdTextField.setText("0x" + this.pgpKey.getPgpKeyId());
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
