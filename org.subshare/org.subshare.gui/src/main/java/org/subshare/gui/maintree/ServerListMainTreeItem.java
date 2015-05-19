package org.subshare.gui.maintree;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;

import org.subshare.core.server.Server;
import org.subshare.core.server.ServerRegistry;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.ls.ServerRegistryLs;
import org.subshare.gui.serverlist.ServerListPane;

public class ServerListMainTreeItem extends MainTreeItem<String> {

	private ServerRegistry serverRegistry;

	private PropertyChangeListener serversPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			@SuppressWarnings("unchecked")
			final Set<Server> servers = new LinkedHashSet<Server>((List<Server>) evt.getNewValue());
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					addOrRemoveTreeItemsViewCallback(servers);
				}
			});
		}
	};

	public ServerListMainTreeItem() {
		super("Servers");

		new Service<List<Server>>() {
			@Override
			protected Task<List<Server>> createTask() {
				return new SsTask<List<Server>>() {
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

	protected synchronized ServerRegistry getServerRegistry() {
		if (serverRegistry == null) {
			serverRegistry = ServerRegistryLs.getServerRegistry();
			serverRegistry.addPropertyChangeListener(ServerRegistry.PropertyEnum.servers, serversPropertyChangeListener);
		}
		return serverRegistry;
	}

	protected void addOrRemoveTreeItemsViewCallback(final Set<Server> servers) {
		assertNotNull("servers", servers);
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

		if (servers.size() < viewServer2ServerMainTreeItem.size()) {
			for (final Server server : servers)
				viewServer2ServerMainTreeItem.remove(server);

			for (final ServerMainTreeItem smti : viewServer2ServerMainTreeItem.values())
				getChildren().remove(smti);
		}
	}

	private void addTableItemsViewCallback(final Collection<Server> servers) {
		assertNotNull("servers", servers);
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
