package org.subshare.gui.serverrepo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.server.Server;
import org.subshare.gui.checkout.CheckOutWizard;

public class ServerRepoPane extends GridPane {
	private final Server server;
	private final ServerRepo serverRepo;

	public ServerRepoPane(final Server server, final ServerRepo serverRepo) {
		this.server = assertNotNull("server", server);
		this.serverRepo = assertNotNull("serverRepo", serverRepo);
		loadDynamicComponentFxml(ServerRepoPane.class, this);
	}

	@FXML
	private void checkOutButtonClicked(final ActionEvent event) {
		new CheckOutWizard(server, serverRepo).checkOut(getScene().getWindow());
	}
}
