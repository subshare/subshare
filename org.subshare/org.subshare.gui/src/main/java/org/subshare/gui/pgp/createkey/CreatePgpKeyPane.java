package org.subshare.gui.pgp.createkey;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

import org.subshare.core.pgp.CreatePgpKeyParam;

public abstract class CreatePgpKeyPane extends GridPane {

	private final CreatePgpKeyParam createPgpKeyParam;

	@FXML
	private TableView<UserId> emailsTableView;

	@FXML
	private PasswordField passwordField;

	@FXML
	private PasswordField passwordField2;

	@FXML
	private Spinner<Integer> validityNumberSpinner;

	@FXML
	private ComboBox<TimeUnit> validityTimeUnitComboBox;

	@FXML
	private ComboBox<String> algorithmComboBox;

	@FXML
	private ComboBox<Integer> strengthComboBox;

	public CreatePgpKeyPane(final CreatePgpKeyParam createPgpKeyParam) {
		loadDynamicComponentFxml(CreatePgpKeyPane.class, this);
		this.createPgpKeyParam = assertNotNull("createPgpKeyParam", createPgpKeyParam);
	}

	@FXML
	protected abstract void okButtonClicked(final ActionEvent event);

	@FXML
	protected abstract void cancelButtonClicked(final ActionEvent event);

	public CreatePgpKeyParam getCreatePgpKeyParam() {
		return createPgpKeyParam;
	}
}
