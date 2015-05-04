package org.subshare.gui.maintree;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;

import org.subshare.core.server.Server;
import org.subshare.core.server.ServerRegistry;
import org.subshare.core.server.ServerRegistryImpl;
import org.subshare.gui.serverlist.ServerListPane;

import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class ServerListMainTreeItem extends MainTreeItem<String> {

	// TODO we should use the LocalServer somehow! Maybe use proxies? Or switch to ServerDto? What about notifications, then?
	// Or is this not necessary for the Server[Registry], because it's not needed by the real JVM, anyway?
	// Or do we only notify the separate JVM whenever we changed the file and make it reload it?
	private ServerRegistry serverRegistry;

	private PropertyChangeListener serversPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			@SuppressWarnings("unchecked")
			final List<Server> servers = (List<Server>) evt.getNewValue();
			addOrRemoveTreeItemsViewCallback(servers);
		}
	};

	public ServerListMainTreeItem() {
		super("Servers");

		new Service<List<Server>>() {
			@Override
			protected Task<List<Server>> createTask() {
				return new Task<List<Server>>() {
					@Override
					protected List<Server> call() throws Exception {
						return getServerRegistry().getServers();
					}

					@Override
					protected void succeeded() {
						final List<Server> servers;
						try { servers = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
						addTableItemsViewCallback(servers);
						super.succeeded();
					}
				};
			}
		}.start();
	}

	protected LocalServerClient getLocalServerClient() {
		return LocalServerClient.getInstance();
	}

	protected ServerRegistry getServerRegistry() {
		if (serverRegistry == null) {
			serverRegistry = getLocalServerClient().invokeStatic(ServerRegistryImpl.class, "getInstance");
			serverRegistry.addPropertyChangeListener(ServerRegistry.PropertyEnum.servers, serversPropertyChangeListener);
		}
		return serverRegistry;
	}

	protected void addOrRemoveTreeItemsViewCallback(List<Server> servers) {
		final Set<Server> modelServers = new HashSet<Server>(servers);
		final Map<Server, ServerMainTreeItem> viewServer2ServerMainTreeItem = new HashMap<>();
		for (final TreeItem<?> ti : getChildren()) {
			final ServerMainTreeItem smti = (ServerMainTreeItem) ti;
			viewServer2ServerMainTreeItem.put(smti.getValueObject(), smti);
		}

		for (final Server server : servers) {
			if (! viewServer2ServerMainTreeItem.containsKey(server)) {
				final ServerMainTreeItem smti = new ServerMainTreeItem(server);
				viewServer2ServerMainTreeItem.put(server, smti);
				getChildren().add(smti);
			}
		}

		if (modelServers.size() < viewServer2ServerMainTreeItem.size()) {
			for (final Server server : modelServers)
				viewServer2ServerMainTreeItem.remove(server);

			for (final ServerMainTreeItem smti : viewServer2ServerMainTreeItem.values())
				getChildren().remove(smti);
		}
	}

	private void addTableItemsViewCallback(final Collection<Server> servers) {
		for (final Server server : servers)
			getChildren().add(new ServerMainTreeItem(server));
	}

	@Override
	protected void finalize() throws Throwable {
		final ServerRegistry serverRegistry = this.serverRegistry;
		if (serverRegistry != null)
			serverRegistry.removePropertyChangeListener(ServerRegistry.PropertyEnum.servers, serversPropertyChangeListener);

		super.finalize();
	}

	@Override
	protected Parent createMainDetailContent() {
		return new ServerListPane();
	}
}
