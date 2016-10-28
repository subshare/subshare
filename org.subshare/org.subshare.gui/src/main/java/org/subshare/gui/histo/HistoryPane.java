package org.subshare.gui.histo;

import static org.subshare.gui.util.FxmlUtil.*;

import org.subshare.core.repo.LocalRepo;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;

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

	public String getLocalPath() {
		return histoFrameListPane.getLocalPath();
	}
	public void setLocalPath(String localPath) {
		histoFrameListPane.setLocalPath(localPath);
		histoFramePane.setLocalPath(localPath);
	}

	public ReadOnlyObjectProperty<HistoFrameListItem> selectedHistoFrameListItemProperty() {
		return histoFrameListPane.selectedItemProperty();
	}

	public ObservableList<TreeItem<HistoCryptoRepoFileTreeItem>> getSelectedHistoCryptoRepoFileTreeItems() {
		return histoFramePane.getSelectedHistoCryptoRepoFileTreeItems();
	}
}
