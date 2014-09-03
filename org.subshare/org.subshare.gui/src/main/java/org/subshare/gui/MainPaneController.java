package org.subshare.gui;

import java.util.Collections;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import org.subshare.gui.maintree.MainTreeItem;
import org.subshare.gui.maintree.ServerMainTreeItem;
import org.subshare.gui.maintree.UserManagementMainTreeItem;

public class MainPaneController {

	@FXML
	private SplitPane splitPane;

	@FXML
	private TreeView<MainTreeItem> mainTree;

	public void initialize() {
		final TreeItem<MainTreeItem> root = new TreeItem<MainTreeItem>();

		for (final ServerMainTreeItem serverMainTreeItem : getServerMainTreeItems())
			root.getChildren().add(new TreeItem<MainTreeItem>(serverMainTreeItem));

		root.getChildren().add(new TreeItem<MainTreeItem>(new UserManagementMainTreeItem()));
		mainTree.setShowRoot(false);
		mainTree.setRoot(root);

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
