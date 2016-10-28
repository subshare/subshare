package org.subshare.gui.statusdialog;

import static org.subshare.gui.util.FxmlUtil.*;

import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

public class StatusPane extends GridPane {

	@FXML
	private Text messageText;

	public StatusPane(final String message) {
		loadDynamicComponentFxml(StatusPane.class, this);

		if (message != null)
			messageText.setText(message);
	}
}
