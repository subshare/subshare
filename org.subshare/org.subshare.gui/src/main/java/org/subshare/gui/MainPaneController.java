package org.subshare.gui;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;

import org.subshare.gui.maintree.MainTreeItem;
import org.subshare.gui.maintree.ServerListMainTreeItem;
import org.subshare.gui.maintree.UserListMainTreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainPaneController {

	private static final Logger logger = LoggerFactory.getLogger(MainPaneController.class);

	@FXML
	private SplitPane splitPane;

	@FXML
	private BorderPane mainTreePane;

	@FXML
	private TreeView<String> mainTree;

	@FXML
	private BorderPane mainDetail;

	private final ListChangeListener<? super TreeItem<String>> mainTreeSelectionListener = new ListChangeListener<TreeItem<String>>() {
		@Override
		public void onChanged(final ListChangeListener.Change<? extends TreeItem<String>> c) {
			final ObservableList<? extends TreeItem<String>> selection = c.getList();
			if (selection.isEmpty())
				mainDetail.setCenter(null);
			else
				mainDetail.setCenter(((MainTreeItem<String>)selection.get(0)).getMainDetailContent());
		}
	};

	public void initialize() {
		final TreeItem<String> root = new TreeItem<String>();

//		for (final ServerMainTreeItem serverMainTreeItem : getServerMainTreeItems()) {
//			final TreeItem<MainTreeItem> serverTI = new TreeItem<MainTreeItem>(serverMainTreeItem);
//			root.getChildren().add(serverTI);
//			for (final RepositoryMainTreeItem repositoryMainTreeItem : serverMainTreeItem.getRepositoryMainTreeItems())
//				serverTI.getChildren().add(new TreeItem<MainTreeItem>(repositoryMainTreeItem));
//		}

		root.getChildren().add(new ServerListMainTreeItem());
		root.getChildren().add(new UserListMainTreeItem());
		mainTree.setShowRoot(false);
		mainTree.setRoot(root);
		mainTree.getSelectionModel().getSelectedItems().addListener(mainTreeSelectionListener);
		// TODO we should save and restore the selection!

		// TODO we should save and restore the divider position!
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				splitPane.setDividerPosition(0, 0.3);
			}
		});
	}

//	private ServerListMainTreeItem createServerListMainTreeItem() {
//		ServerListMainTreeItem serverListMainTreeItem = new ServerListMainTreeItem();
//		List<ServerMainTreeItem> serverMainTreeItems = getServerMainTreeItems(serverListMainTreeItem);
//
//	}

//	private List<ServerMainTreeItem> getServerMainTreeItems(final ServerListMainTreeItem serverListMainTreeItem) {
//		// TODO we should store this somehow in a registry, because creating+closing the LocalRepoManagers takes some time.
//		// Additionally: Where do we store a descriptive name for the repository, otherwise?!
//
//		final Map<Server, List<File>> server2LocalRoots = new HashMap<>();
//
//		final ServerRegistry serverRegistry = ServerRegistry.getInstance();
//		final LocalRepoRegistry localRepoRegistry = LocalRepoRegistry.getInstance();
//		for (final UUID localRepositoryId : localRepoRegistry.getRepositoryIds()) {
//			final File localRoot = localRepoRegistry.getLocalRoot(localRepositoryId);
//			if (localRoot == null)
//				continue; // maybe deleted during iteration
//
//			try (final LocalRepoManager localRepoManager = LocalRepoManagerFactory.Helper.getInstance().createLocalRepoManagerForExistingRepository(localRoot);) {
//				for (final URL remoteRoot : localRepoManager.getRemoteRepositoryId2RemoteRootMap().values()) {
//					final Server server = serverRegistry.getServerForRemoteRoot(remoteRoot);
//					if (server == null) // should we better throw an exception?!
//						logger.warn("No Server found for: localRoot='{}' remoteRoot='{}'!", localRoot, remoteRoot);
//
//					List<File> localRoots = server2LocalRoots.get(server);
//					if (localRoots == null) {
//						localRoots = new ArrayList<>();
//						server2LocalRoots.put(server, localRoots);
//					}
//					localRoots.add(localRoot);
//				}
//			}
//		}
//
//		final List<ServerMainTreeItem> result = new ArrayList<ServerMainTreeItem>();
//		for (final Server server : ServerRegistry.getInstance().getServers()) {
//			final ServerMainTreeItem serverMainTreeItem = new ServerMainTreeItem(serverListMainTreeItem);
//			serverMainTreeItem.setName(server.getName());
//			result.add(serverMainTreeItem);
//
//			final List<File> localRoots = server2LocalRoots.get(server);
//			if (localRoots != null) {
//				for (File localRoot : localRoots) {
//					final RepositoryMainTreeItem repositoryMainTreeItem = new RepositoryMainTreeItem(serverMainTreeItem);
//					repositoryMainTreeItem.setName(localRoot.getName()); // TODO should better be a descriptive name from a registry.
//					repositoryMainTreeItem.setLocalRoot(localRoot);
//					serverMainTreeItem.getRepositoryMainTreeItems().add(repositoryMainTreeItem);
//				}
//			}
//		}
//
//		return result;
//	}
}
