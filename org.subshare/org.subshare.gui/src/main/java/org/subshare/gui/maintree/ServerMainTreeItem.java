package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.ServerRepoRegistry;
import org.subshare.core.server.Server;
import org.subshare.gui.ls.ServerRepoRegistryLs;
import org.subshare.gui.server.ServerPane;

import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ServerMainTreeItem extends MainTreeItem<Server> {

	private static final Image icon = new Image(ServerListMainTreeItem.class.getResource("server_16x16.png").toExternalForm());
	private ServerRepoRegistry serverRepoRegistry;
	private boolean childrenLoaded;

	public ServerMainTreeItem(final Server server) {
		super(assertNotNull("server", server));
		setGraphic(new ImageView(icon));
	}

	private PropertyChangeListener serverReposPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			@SuppressWarnings("unchecked")
			final List<ServerRepo> serverRepos = new ArrayList<ServerRepo>((List<ServerRepo>) evt.getNewValue());
			final Server server = getServer();

			for (Iterator<ServerRepo> it = serverRepos.iterator(); it.hasNext();) {
				if (!server.getServerId().equals(it.next().getServerId()))
					it.remove();
			}

			addOrRemoveTreeItems(serverRepos);
		}
	};

	private void addOrRemoveTreeItems(final List<ServerRepo> serverRepos) {
		final Set<ServerRepo> modelRepos = new HashSet<ServerRepo>(serverRepos);
		final Map<ServerRepo, ServerRepoMainTreeItem> viewRepo2ServerRepoMainTreeItem = new HashMap<>();
		for (TreeItem<String> ti : getChildren()) {
			final ServerRepoMainTreeItem treeItem = (ServerRepoMainTreeItem) ti;
			viewRepo2ServerRepoMainTreeItem.put(treeItem.getServerRepo(), treeItem);
		}

		for (final ServerRepo serverRepo : serverRepos) {
			if (! viewRepo2ServerRepoMainTreeItem.containsKey(serverRepo)) {
				final ServerRepoMainTreeItem treeItem = new ServerRepoMainTreeItem(serverRepo);
				viewRepo2ServerRepoMainTreeItem.put(serverRepo, treeItem);
				getChildren().add(treeItem);
			}
		}

		if (modelRepos.size() < viewRepo2ServerRepoMainTreeItem.size()) {
			for (final ServerRepo serverRepo : modelRepos)
				viewRepo2ServerRepoMainTreeItem.remove(serverRepo);

			for (final ServerRepoMainTreeItem treeItem : viewRepo2ServerRepoMainTreeItem.values())
				getChildren().remove(treeItem);
		}
	}

	@Override
	public ObservableList<TreeItem<String>> getChildren() {
		final ObservableList<TreeItem<String>> children = super.getChildren();
		if (! childrenLoaded) {
			childrenLoaded = true;
			final List<ServerRepo> serverRepos = getServerRepoRegistry().getServerReposOfServer(getServer().getServerId());
			addOrRemoveTreeItems(serverRepos);
		}
		return children;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	protected ServerRepoRegistry getServerRepoRegistry() {
		if (serverRepoRegistry == null) {
			serverRepoRegistry = ServerRepoRegistryLs.getServerRepoRegistry();
			addWeakPropertyChangeListener(serverRepoRegistry, ServerRepoRegistry.PropertyEnum.serverRepos, serverReposPropertyChangeListener);
		}
		return serverRepoRegistry;
	}

	public Server getServer() {
		return getValueObject();
	}

	@Override
	protected String getValueString() {
		return getValueObject().getName();
	}

	@Override
	public String toString() {
		return getValueObject().getName();
	}

	@Override
	protected Parent createMainDetailContent() {
		return new ServerPane(getValueObject());
	}
}
