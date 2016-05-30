package org.subshare.gui.histo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.PlatformUtil.*;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;

import org.subshare.core.dto.HistoCryptoRepoFileDto;
import org.subshare.core.repo.LocalRepo;
import org.subshare.gui.histo.exp.ExportFromHistoryData;
import org.subshare.gui.histo.exp.ExportFromHistoryWizard;
import org.subshare.gui.wizard.WizardDialog;

import co.codewizards.cloudstore.core.dto.NormalFileDto;
import co.codewizards.cloudstore.core.dto.Uid;

public class HistoryPaneSupport {

	private final HistoryPaneContainer container;
	private final LocalRepo localRepo;
	private final TabPane tabPane;
	private final Tab historyTab;
	private final Button exportFromHistoryButton;

	private WeakReference<HistoryPane> historyPaneRef;

	private final InvalidationListener selectedHistoCryptoRepoFileTreeItemsInvalidationListener = observable -> selectedHistoCryptoRepoFileTreeItemsChanged();

	private final ObservableSet<Uid> selectedHistoCryptoRepoFileIds = FXCollections.observableSet(new HashSet<Uid>());

	public HistoryPaneSupport(final HistoryPaneContainer container) {
		assertFxApplicationThread();
		this.container = assertNotNull("container", container);
		this.localRepo = assertNotNull("container.localRepo", this.container.getLocalRepo());
		this.tabPane = assertNotNull("container.tabPane", this.container.getTabPane());
		this.historyTab = assertNotNull("container.historyTab", this.container.getHistoryTab());
		this.exportFromHistoryButton = assertNotNull("container.exportFromHistoryButton", this.container.getExportFromHistoryButton());

		tabPane.getSelectionModel().selectedItemProperty().addListener((InvalidationListener) observable -> createOrForgetHistoryPane());
		selectedHistoCryptoRepoFileIds.addListener((InvalidationListener) observable -> selectedHistoCryptoRepoFileIdsChanged());
		configureExportFromHistoryButton();
		createOrForgetHistoryPane();
	}

	private void configureExportFromHistoryButton() {
		final String imageName = "export-from-history_24x24.png";
		final URL imageUrl = HistoryPaneSupport.class.getResource(imageName); //$NON-NLS-1$
		assertNotNull("imageUrl", imageUrl, "imageName = %s", imageName);
		exportFromHistoryButton.setGraphic(new ImageView(imageUrl.toString()));
		exportFromHistoryButton.setTooltip(new Tooltip("Export the selected file(s) from the history."));
		exportFromHistoryButton.setOnAction(event -> exportFromHistoryButtonClicked(event));
	}

	public ObservableSet<Uid> getSelectedHistoCryptoRepoFileIds() {
		return selectedHistoCryptoRepoFileIds;
	}

	private void createOrForgetHistoryPane() {
		assertFxApplicationThread();

		HistoryPane historyPane = historyPaneRef == null ? null : historyPaneRef.get();
		if (historyPane != null)
			historyPane.getSelectedHistoCryptoRepoFileTreeItems().removeListener(selectedHistoCryptoRepoFileTreeItemsInvalidationListener);

		if (historyTab != tabPane.getSelectionModel().getSelectedItem()) {
			historyTab.setContent(null);
			exportFromHistoryButton.setVisible(false);
			selectedHistoCryptoRepoFileIds.clear();
			return;
		}

		if (historyPane == null) {
			historyPane = new HistoryPane();
			historyPane.setLocalRepo(localRepo);
			historyPane.setLocalPath(container.getLocalPath());
			historyPaneRef = new WeakReference<>(historyPane);
		}

		historyPane.getSelectedHistoCryptoRepoFileTreeItems().addListener(selectedHistoCryptoRepoFileTreeItemsInvalidationListener);

		if (historyTab.getContent() == null)
			historyTab.setContent(historyPane);

		exportFromHistoryButton.setVisible(true);
		selectedHistoCryptoRepoFileTreeItemsChanged();
		selectedHistoCryptoRepoFileIdsChanged();
	}

	private void selectedHistoCryptoRepoFileTreeItemsChanged() {
		final HistoryPane historyPane = historyPaneRef == null ? null : historyPaneRef.get();

		final List<TreeItem<HistoCryptoRepoFileTreeItem>> selectedTreeItems;
		if (historyPane == null)
			selectedTreeItems = Collections.emptyList();
		else
			selectedTreeItems = historyPane.getSelectedHistoCryptoRepoFileTreeItems();

		final Set<Uid> newSelectedHistoCryptoRepoFileIds = new HashSet<Uid>();
		for (final TreeItem<HistoCryptoRepoFileTreeItem> treeItem : selectedTreeItems) {
			if (treeItem == null) // this should IMHO really never happen, but it does :-(
				continue;

			final HistoCryptoRepoFileTreeItem ti = treeItem.getValue();
			final HistoCryptoRepoFileDto histoCryptoRepoFileDto = ti.getHistoCryptoRepoFileDto();
			if (histoCryptoRepoFileDto != null
					&& ti.getPlainHistoCryptoRepoFileDto().getRepoFileDto() instanceof NormalFileDto) // TODO support all types!
				newSelectedHistoCryptoRepoFileIds.add(histoCryptoRepoFileDto.getHistoCryptoRepoFileId());
		}

		selectedHistoCryptoRepoFileIds.retainAll(newSelectedHistoCryptoRepoFileIds);
		selectedHistoCryptoRepoFileIds.addAll(newSelectedHistoCryptoRepoFileIds);
	}

	private void selectedHistoCryptoRepoFileIdsChanged() {
		exportFromHistoryButton.setDisable(selectedHistoCryptoRepoFileIds.isEmpty());
	}

	private void exportFromHistoryButtonClicked(final ActionEvent event) {
		final ExportFromHistoryData exportFromHistoryData = new ExportFromHistoryData(localRepo);
		exportFromHistoryData.getHistoCryptoRepoFileIds().addAll(selectedHistoCryptoRepoFileIds);
		final ExportFromHistoryWizard wizard = new ExportFromHistoryWizard(exportFromHistoryData);
		final WizardDialog dialog = new WizardDialog(tabPane.getScene().getWindow(), wizard);
		dialog.show(); // no need to wait ;-)
	}
}
