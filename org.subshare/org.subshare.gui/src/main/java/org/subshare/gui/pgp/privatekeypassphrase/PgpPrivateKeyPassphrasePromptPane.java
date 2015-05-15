package org.subshare.gui.pgp.privatekeypassphrase;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;

import org.subshare.core.pgp.PgpKey;

public class PgpPrivateKeyPassphrasePromptPane extends GridPane {

	private final PgpKey pgpKey;

	@FXML
	private Label headerLabel;

	@FXML
	private ComboBox<String> userIdsComboBox;

	@FXML
	private PasswordField passwordField;

	public PgpPrivateKeyPassphrasePromptPane(final PgpKey pgpKey) {
		loadDynamicComponentFxml(PgpPrivateKeyPassphrasePromptPane.class, this);
		this.pgpKey = assertNotNull("pgpKey", pgpKey);
		userIdsComboBox.setItems(FXCollections.observableArrayList(pgpKey.getUserIds()));
	}

}
