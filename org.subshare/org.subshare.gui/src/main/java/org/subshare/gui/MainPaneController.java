package org.subshare.gui;

import java.util.Collections;
import java.util.List;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

import org.subshare.gui.maintree.MainTreeItem;
import org.subshare.gui.maintree.ServerMainTreeItem;
import org.subshare.gui.maintree.UserListMainTreeItem;

public class MainPaneController {

	@FXML
	private SplitPane splitPane;

	@FXML
	private TreeView<MainTreeItem> mainTree;

	@FXML
	private BorderPane mainDetail;

	private final ListChangeListener<? super TreeItem<MainTreeItem>> mainTreeSelectionListener = new ListChangeListener<TreeItem<MainTreeItem>>() {
		@Override
		public void onChanged(final ListChangeListener.Change<? extends TreeItem<MainTreeItem>> c) {
			final ObservableList<? extends TreeItem<MainTreeItem>> selection = c.getList();
			if (selection.isEmpty())
				mainDetail.setCenter(null);
			else
				mainDetail.setCenter(selection.get(0).getValue().getMainDetailContent());
		}
	};

	public void initialize() {
		final TreeItem<MainTreeItem> root = new TreeItem<MainTreeItem>();

		for (final ServerMainTreeItem serverMainTreeItem : getServerMainTreeItems())
			root.getChildren().add(new TreeItem<MainTreeItem>(serverMainTreeItem));

		root.getChildren().add(new TreeItem<MainTreeItem>(new UserListMainTreeItem()));
		mainTree.setShowRoot(false);
		mainTree.setRoot(root);
		mainTree.getSelectionModel().getSelectedItems().addListener(mainTreeSelectionListener);

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				splitPane.setDividerPosition(0, 0.3);
			}
		});
	}

	private List<ServerMainTreeItem> getServerMainTreeItems() {
		// TODO We should have a registry maintaining servers. Where do we put it?
		// Here in this GUI-project? Or do we place it in o.c.core? Or maybe even in CloudStore?

		final ServerMainTreeItem serverMainTreeItem = new ServerMainTreeItem();
		serverMainTreeItem.setName("SubShare");
		return Collections.singletonList(serverMainTreeItem);
	}

}
