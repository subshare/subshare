package org.subshare.gui;

import java.util.Arrays;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

import org.subshare.gui.maintree.LocalRepoListMainTreeItem;
import org.subshare.gui.maintree.MainTreeItem;
import org.subshare.gui.maintree.RootMainTreeItem;
import org.subshare.gui.maintree.ServerListMainTreeItem;
import org.subshare.gui.maintree.UserListMainTreeItem;
import org.subshare.gui.scene.MainDetailContent;

public class MainPaneController {
	@FXML
	private SplitPane splitPane;

	@FXML
	private BorderPane mainTreePane;

	@FXML
	private TreeView<String> mainTreeView;

	@FXML
	private BorderPane mainDetail;

	public void initialize() {
		final RootMainTreeItem root = new RootMainTreeItem(mainTreeView);

		root.getChildren().addAll(FXCollections.observableArrayList(Arrays.asList(
				new LocalRepoListMainTreeItem(),
				new ServerListMainTreeItem(),
				new UserListMainTreeItem()
				)));

		mainTreeView.setShowRoot(false);
		mainTreeView.setRoot(root);
		mainTreeView.getSelectionModel().selectedItemProperty().addListener(
				(ChangeListener<TreeItem<String>>) (observable, oldValue, newValue) -> onSelectionChange() );

		// TODO we should save and restore the selection!
		mainTreeView.getSelectionModel().select(0);

		// TODO we should save and restore the divider position!
		Platform.runLater(() -> {
			Platform.runLater(() -> {
				Platform.runLater(() -> splitPane.setDividerPosition(0, 0.3));
			});
		});
	}

	private void onSelectionChange() {
		final Node oldMainDetailContent = mainDetail.getCenter();
		final MainTreeItem<?> selectedItem = (MainTreeItem<?>) mainTreeView.getSelectionModel().getSelectedItem();
		final Node newMainDetailContent = selectedItem == null ? null : selectedItem.getMainDetailContent();

		if (oldMainDetailContent instanceof MainDetailContent)
			((MainDetailContent) oldMainDetailContent).onHidden();

		mainDetail.setCenter(newMainDetailContent);

		if (newMainDetailContent instanceof MainDetailContent)
			((MainDetailContent) newMainDetailContent).onShown();
	}
}
