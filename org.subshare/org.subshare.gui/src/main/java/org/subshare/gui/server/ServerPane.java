package org.subshare.gui.server;

import static org.subshare.gui.util.FxmlUtil.loadDynamicComponentFxml;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

public class ServerPane extends BorderPane /* GridPane */ {

	@FXML
	private Button createRepositoryButton;

	public ServerPane() {
		loadDynamicComponentFxml(ServerPane.class, this);
	}

	@FXML
	private void createRepositoryButtonClicked(final ActionEvent event) {
		System.out.println("createRepositoryButtonClicked: " + event);
	}
}
