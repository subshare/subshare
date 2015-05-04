package org.subshare.gui.serverlist;

import static co.codewizards.cloudstore.core.util.Util.cast;
import static org.subshare.gui.util.FxmlUtil.loadDynamicComponentFxml;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;

import org.subshare.core.server.Server;
import org.subshare.core.server.ServerImpl;
import org.subshare.core.server.ServerRegistry;
import org.subshare.core.server.ServerRegistryImpl;
import org.subshare.gui.util.UrlStringConverter;

import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class ServerListPane extends BorderPane {

	private ServerRegistry serverRegistry;

	@FXML
	private Button addButton;
	@FXML
	private Button deleteButton;

	@FXML
	private TableView<ServerListItem> tableView;

	private final ListChangeListener<ServerListItem> selectionListener = new ListChangeListener<ServerListItem>() {
		@Override
		public void onChanged(final ListChangeListener.Change<? extends ServerListItem> c) {
			updateEnabled();
		}
	};

	private PropertyChangeListener serversPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			@SuppressWarnings("unchecked")
			final List<Server> servers = (List<Server>) evt.getNewValue();
			addOrRemoveItemTablesViewCallback(servers);
		}
	};

	private PropertyChangeListener serverPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(final PropertyChangeEvent evt) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
//					final Server server = (Server) evt.getSource();
					// workaround for refresh bug
					List<TableColumn<ServerListItem, ?>> columns = new ArrayList<>(tableView.getColumns());
					tableView.getColumns().clear();
					tableView.getColumns().addAll(columns);
				}
			});
		}
	};

	public ServerListPane() {
		loadDynamicComponentFxml(ServerListPane.class, this);
		tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		tableView.getSelectionModel().getSelectedItems().addListener(selectionListener);
		for (final TableColumn<ServerListItem, ?> tableColumn : tableView.getColumns()) {
			if ("nameColumn".equals(tableColumn.getId())) {
				final TableColumn<ServerListItem, String> tc = cast(tableColumn);
				tc.setCellFactory(cast(TextFieldTableCell.forTableColumn()));
				tc.setOnEditCommit(new EventHandler<CellEditEvent<ServerListItem, String>>() {
					@Override
					public void handle(CellEditEvent<ServerListItem, String> event) {
						event.getRowValue().setName(event.getNewValue());
						getServerRegistry().writeIfNeeded();
					}
				});
			}
			else if ("urlColumn".equals(tableColumn.getId())) {
				final TableColumn<ServerListItem, URL> tc = cast(tableColumn);
				tc.setCellFactory(cast(TextFieldTableCell.forTableColumn(new UrlStringConverter())));
				tc.setOnEditCommit(new EventHandler<CellEditEvent<ServerListItem, URL>>() {
					@Override
					public void handle(CellEditEvent<ServerListItem, URL> event) {
						event.getRowValue().setUrl(event.getNewValue());
						getServerRegistry().writeIfNeeded();
					}
				});
			}

		}
		populateTableViewAsync();
		updateEnabled();
	}

	private void updateEnabled() {
		final boolean selectionEmpty = tableView.getSelectionModel().getSelectedItems().isEmpty();
		deleteButton.setDisable(selectionEmpty);
	}

	private void populateTableViewAsync() {
		new Service<Collection<Server>>() {
			@Override
			protected Task<Collection<Server>> createTask() {
				return new Task<Collection<Server>>() {
					@Override
					protected Collection<Server> call() throws Exception {
						return getServerRegistry().getServers();
					}

					@Override
					protected void succeeded() {
						final Collection<Server> servers;
						try { servers = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
						addTableItemsViewCallback(servers);
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
			serverRegistry.addPropertyChangeListener(ServerRegistry.PropertyEnum.servers_server, serverPropertyChangeListener);
		}
		return serverRegistry;
	}

	private void addOrRemoveItemTablesViewCallback(final Collection<Server> servers) {
		final Set<Server> modelServers = new HashSet<Server>(servers);
		final Map<Server, ServerListItem> viewServer2ServerListItem = new HashMap<>();
		for (final ServerListItem sli : tableView.getItems())
			viewServer2ServerListItem.put(sli.getServer(), sli);

		for (final Server server : servers) {
			if (! viewServer2ServerListItem.containsKey(server)) {
				final ServerListItem sli = new ServerListItem(server);
				viewServer2ServerListItem.put(server, sli);
				tableView.getItems().add(sli);
			}
		}

		if (modelServers.size() < viewServer2ServerListItem.size()) {
			for (final Server server : modelServers)
				viewServer2ServerListItem.remove(server);

			for (final ServerListItem sli : viewServer2ServerListItem.values())
				tableView.getItems().remove(sli);
		}
	}

	private void addTableItemsViewCallback(final Collection<Server> servers) {
		for (final Server server : servers)
			tableView.getItems().add(new ServerListItem(server));

		tableView.requestLayout();
	}

	@Override
	protected void finalize() throws Throwable {
		final ServerRegistry serverRegistry = this.serverRegistry;
		if (serverRegistry != null) {
			serverRegistry.removePropertyChangeListener(ServerRegistry.PropertyEnum.servers, serversPropertyChangeListener);
			serverRegistry.removePropertyChangeListener(ServerRegistry.PropertyEnum.servers_server, serverPropertyChangeListener);
		}
		super.finalize();
	}

	@FXML
	private void addButtonClicked(final ActionEvent event) {
		System.out.println("addButtonClicked: " + event);
		Server server = getLocalServerClient().invokeConstructor(ServerImpl.class);
		server.setName("Server " + new DateTime(new Date()));
		try {
			server.setUrl(new URL("https://host.domain.tld:1234"));
		} catch (MalformedURLException e) {
			throw new RuntimeException();
		}
		getServerRegistry().getServers().add(server);
		getServerRegistry().write();
	}

	@FXML
	private void deleteButtonClicked(final ActionEvent event) {
		System.out.println("deleteButtonClicked: " + event);
	}
}
