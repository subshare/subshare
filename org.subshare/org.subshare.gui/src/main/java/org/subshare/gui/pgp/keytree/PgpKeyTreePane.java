package org.subshare.gui.pgp.keytree;

import static org.subshare.gui.util.FxmlUtil.*;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.BorderPane;

public class PgpKeyTreePane extends BorderPane {

	@FXML
	private TreeTableView<PgpKeyTreeItem<?>> treeTableView;

	public PgpKeyTreePane() {
		loadDynamicComponentFxml(PgpKeyTreePane.class, this);

		treeTableView.setShowRoot(false);
		treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}

	public TreeTableView<PgpKeyTreeItem<?>> getTreeTableView() {
		return treeTableView;
	}
}
