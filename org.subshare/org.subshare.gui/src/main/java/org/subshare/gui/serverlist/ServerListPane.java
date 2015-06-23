package org.subshare.gui.serverlist;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

import org.subshare.core.Severity;
import org.subshare.core.locker.sync.LockerSyncDaemon;
import org.subshare.core.pgp.sync.PgpSyncDaemon;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerRegistry;
import org.subshare.core.sync.SyncState;
import org.subshare.gui.IconSize;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.ls.LockerSyncDaemonLs;
import org.subshare.gui.ls.PgpSyncDaemonLs;
import org.subshare.gui.ls.ServerRegistryLs;
import org.subshare.gui.severity.SeverityImageRegistry;
import org.subshare.gui.util.UrlStringConverter;

import co.codewizards.cloudstore.core.dto.DateTime;
import co.codewizards.cloudstore.core.util.StringUtil;

public class ServerListPane extends BorderPane {

	private ServerRegistry serverRegistry;

	private PgpSyncDaemon pgpSyncDaemon;

	private LockerSyncDaemon lockerSyncDaemon;

	@FXML
	private Button addButton;
	@FXML
	private Button deleteButton;

	@FXML
	private TableView<ServerListItem> tableView;

	@FXML
	private TableColumn<ServerListItem, String> nameColumn;

	@FXML
	private TableColumn<ServerListItem, URL> urlColumn;

	@FXML
	private TableColumn<ServerListItem, Severity> severityIconColumn;

	private final ListChangeListener<ServerListItem> selectionListener = new ListChangeListener<ServerListItem>() {
		@Override
		public void onChanged(final ListChangeListener.Change<? extends ServerListItem> c) {
			updateEnabled();
		}
	};

	private final PropertyChangeListener serversPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			@SuppressWarnings("unchecked")
			final Set<Server> servers = new LinkedHashSet<Server>((List<Server>) evt.getNewValue());
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					addOrRemoveItemTablesViewCallback(servers);
				}
			});
		}
	};

	private final PropertyChangeListener serverPropertyChangeListener = new PropertyChangeListener() {
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

	private final PropertyChangeListener syncStatePropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(final PropertyChangeEvent evt) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					for (ServerListItem serverListItem : tableView.getItems())
						updateSyncStates(serverListItem);

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

		nameColumn.setCellFactory(cast(TextFieldTableCell.forTableColumn()));
		nameColumn.setOnEditCommit(new EventHandler<CellEditEvent<ServerListItem, String>>() {
			@Override
			public void handle(CellEditEvent<ServerListItem, String> event) {
				event.getRowValue().setName(event.getNewValue());
			}
		});

		urlColumn.setCellFactory(cast(TextFieldTableCell.forTableColumn(new UrlStringConverter())));
		urlColumn.setOnEditCommit(new EventHandler<CellEditEvent<ServerListItem, URL>>() {
			@Override
			public void handle(CellEditEvent<ServerListItem, URL> event) {
				event.getRowValue().setUrl(event.getNewValue());
			}
		});

		severityIconColumn.setCellFactory(l -> new TableCell<ServerListItem, Severity>() {
			@Override
			public void updateItem(final Severity severity, final boolean empty) {
				if (empty)
					setGraphic(null);
				else {
					assertNotNull("severity", severity);
					final ServerListItem serverListItem = (ServerListItem) getTableRow().getItem();
					if (serverListItem == null)
						return;

					final String tooltipText = serverListItem.getTooltipText();

					final ImageView imageView = new ImageView(SeverityImageRegistry.getInstance().getImage(severity, IconSize._16x16));

					if (!StringUtil.isEmpty(tooltipText)) {
						Tooltip tooltip = new Tooltip(tooltipText);
						setTooltip(tooltip);
					}
					setGraphic(imageView);
				}
			}
		});

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
				return new SsTask<Collection<Server>>() {
					@Override
					protected Collection<Server> call() throws Exception {
						getPgpSyncDaemon(); // hook listener
						getLockerSyncDaemon(); // hook listener
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

	protected ServerRegistry getServerRegistry() {
		if (serverRegistry == null) {
			serverRegistry = ServerRegistryLs.getServerRegistry();
			serverRegistry.addPropertyChangeListener(ServerRegistry.PropertyEnum.servers, serversPropertyChangeListener);
			serverRegistry.addPropertyChangeListener(ServerRegistry.PropertyEnum.servers_server, serverPropertyChangeListener);
		}
		return serverRegistry;
	}

	protected PgpSyncDaemon getPgpSyncDaemon() {
		if (pgpSyncDaemon == null) {
			pgpSyncDaemon = PgpSyncDaemonLs.getPgpSyncDaemon();
			pgpSyncDaemon.addPropertyChangeListener(syncStatePropertyChangeListener);
		}
		return pgpSyncDaemon;
	}

	protected LockerSyncDaemon getLockerSyncDaemon() {
		if (lockerSyncDaemon == null) {
			lockerSyncDaemon = LockerSyncDaemonLs.getLockerSyncDaemon();
			lockerSyncDaemon.addPropertyChangeListener(syncStatePropertyChangeListener);
		}
		return lockerSyncDaemon;
	}

	private void addOrRemoveItemTablesViewCallback(final Set<Server> servers) {
		assertNotNull("servers", servers);
		final Map<Server, ServerListItem> viewServer2ServerListItem = new HashMap<>();
		for (final ServerListItem sli : tableView.getItems())
			viewServer2ServerListItem.put(sli.getServer(), sli);

		for (final Server server : servers) {
			if (! viewServer2ServerListItem.containsKey(server)) {
				final ServerListItem sli = new ServerListItem(server);
				updateSyncStates(sli);
				viewServer2ServerListItem.put(server, sli);
				tableView.getItems().add(sli);
			}
		}

		if (servers.size() < viewServer2ServerListItem.size()) {
			for (final Server server : servers)
				viewServer2ServerListItem.remove(server);

			for (final ServerListItem sli : viewServer2ServerListItem.values())
				tableView.getItems().remove(sli);
		}

//		tableView.requestLayout();
	}

	protected void updateSyncStates(final ServerListItem serverListItem) {
		final Server server = assertNotNull("serverListItem", serverListItem).getServer();
		assertNotNull("serverListItem.server", server);

		SyncState state = getPgpSyncDaemon().getState(serverListItem.getServer());
		serverListItem.setPgpSyncState(state);

		state = getLockerSyncDaemon().getState(serverListItem.getServer());
		serverListItem.setLockerSyncState(state);
	}

	private void addTableItemsViewCallback(final Collection<Server> servers) {
		assertNotNull("servers", servers);
		for (final Server server : servers) {
			ServerListItem serverListItem = new ServerListItem(server);
			updateSyncStates(serverListItem);
			tableView.getItems().add(serverListItem);
		}
		tableView.requestLayout();
	}

	@Override
	protected void finalize() throws Throwable {
		final ServerRegistry serverRegistry = this.serverRegistry;
		if (serverRegistry != null) {
			serverRegistry.removePropertyChangeListener(ServerRegistry.PropertyEnum.servers, serversPropertyChangeListener);
			serverRegistry.removePropertyChangeListener(ServerRegistry.PropertyEnum.servers_server, serverPropertyChangeListener);
		}

		final LockerSyncDaemon lockerSyncDaemon = this.lockerSyncDaemon;
		if (lockerSyncDaemon != null)
			lockerSyncDaemon.removePropertyChangeListener(syncStatePropertyChangeListener);

		final PgpSyncDaemon pgpSyncDaemon = this.pgpSyncDaemon;
		if (pgpSyncDaemon != null)
			pgpSyncDaemon.removePropertyChangeListener(syncStatePropertyChangeListener);

		super.finalize();
	}

	@FXML
	private void addButtonClicked(final ActionEvent event) {
		System.out.println("addButtonClicked: " + event);
		Server server = getServerRegistry().createServer();
		server.setName("Server " + new DateTime(new Date()));
		try {
			server.setUrl(new URL("https://host.domain.tld:1234"));
		} catch (MalformedURLException e) {
			throw new RuntimeException();
		}
		getServerRegistry().getServers().add(server);
	}

	@FXML
	private void deleteButtonClicked(final ActionEvent event) {
		final ObservableList<ServerListItem> selectedItems = tableView.getSelectionModel().getSelectedItems();
		final List<Server> selectedServers = new ArrayList<Server>(selectedItems.size());
		for (ServerListItem serverListItem : selectedItems)
			selectedServers.add(serverListItem.getServer());

		// TODO how to handle servers that are in use?
		getServerRegistry().getServers().removeAll(selectedServers);
	}
}
