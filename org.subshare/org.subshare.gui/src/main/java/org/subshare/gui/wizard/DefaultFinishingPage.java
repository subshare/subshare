package org.subshare.gui.wizard;

import static org.subshare.gui.util.FxmlUtil.*;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

public class DefaultFinishingPage extends GridPane implements FinishingPage {

	@FXML
	private Text headerText;

	public DefaultFinishingPage() {
		loadDynamicComponentFxml(DefaultFinishingPage.class, this);
	}

	public Text getHeaderText() {
		return headerText;
	}
}
