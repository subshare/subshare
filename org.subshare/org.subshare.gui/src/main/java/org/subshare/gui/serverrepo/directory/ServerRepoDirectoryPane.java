package org.subshare.gui.serverrepo.directory;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import org.subshare.core.repo.metaonly.ServerRepoFile;
import org.subshare.gui.checkout.CheckOutWizard;

public class ServerRepoDirectoryPane extends GridPane {
	private final ServerRepoFile serverRepoFile;

	@FXML
	private TextField localPathTextField;

	@FXML
	private TextField serverUrlTextField;

	public ServerRepoDirectoryPane(final ServerRepoFile serverRepoFile) {
		this.serverRepoFile = assertNotNull("serverRepoFile", serverRepoFile);
		loadDynamicComponentFxml(ServerRepoDirectoryPane.class, this);
		localPathTextField.setText(serverRepoFile.getLocalPath());
		serverUrlTextField.setText(serverRepoFile.getServerUrl().toExternalForm());
	}

	@FXML
	private void checkOutButtonClicked(final ActionEvent event) {
		new CheckOutWizard(serverRepoFile).checkOut(getScene().getWindow());
	}
}
