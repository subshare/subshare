package org.subshare.gui.histo;

import static org.subshare.gui.util.FxmlUtil.*;
import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;

import org.subshare.core.repo.LocalRepo;

public class HistoryPane extends SplitPane {

	@FXML
	private HistoFrameListPane histoFrameListPane;

	@FXML
	private HistoFramePane histoFramePane;

	public HistoryPane() {
		loadDynamicComponentFxml(HistoryPane.class, this);
		histoFrameListPane.selectedItemProperty().addListener((InvalidationListener) observable -> {
			final HistoFrameListItem histoFrameListItem = histoFrameListPane.selectedItemProperty().get();
			histoFramePane.setHistoFrameId(histoFrameListItem == null ? null : histoFrameListItem.getHistoFrameDto().getHistoFrameId());
		});
	}

	public LocalRepo getLocalRepo() {
		return histoFrameListPane.getLocalRepo();
	}
	public void setLocalRepo(final LocalRepo localRepo) {
		histoFrameListPane.setLocalRepo(localRepo);
		histoFramePane.setLocalRepo(localRepo);
	}
}
