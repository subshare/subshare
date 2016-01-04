package org.subshare.gui.histo;

import static org.subshare.gui.util.FxmlUtil.*;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;

import org.subshare.core.repo.LocalRepo;

public class HistoryPane extends SplitPane {

	@FXML
	private HistoFrameListPane histoFrameListPane;

	public HistoryPane() {
		loadDynamicComponentFxml(HistoryPane.class, this);
	}

	public LocalRepo getLocalRepo() {
		return histoFrameListPane.getLocalRepo();
	}
	public void setLocalRepo(final LocalRepo localRepo) {
		histoFrameListPane.setLocalRepo(localRepo);
	}
}
