package org.subshare.gui.resolvecollision.loading;

import static org.subshare.gui.util.FxmlUtil.*;

import javafx.scene.layout.GridPane;

public class LoadingPane extends GridPane {

	public LoadingPane() {
		loadDynamicComponentFxml(LoadingPane.class, this);
	}

}
