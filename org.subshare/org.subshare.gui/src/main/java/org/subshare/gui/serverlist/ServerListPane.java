package org.subshare.gui.serverlist;

import static org.subshare.gui.util.FxmlUtil.loadDynamicComponentFxml;

import java.util.Collection;
import java.util.concurrent.ExecutionException;

import javafx.collections.ListChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;

import org.subshare.core.server.Server;
import org.subshare.core.server.ServerRegistry;

public class ServerListPane extends BorderPane {

	@FXML
	private Button addButton;
	@FXML
	private Button editButton;
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

	public ServerListPane() {
		loadDynamicComponentFxml(ServerListPane.class, this);
		tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		tableView.getSelectionModel().getSelectedItems().addListener(selectionListener);
		populateTableViewAsync();
		updateEnabled();
	}

	private void updateEnabled() {
		final boolean selectionEmpty = tableView.getSelectionModel().getSelectedItems().isEmpty();
		editButton.disableProperty().set(selectionEmpty);
		deleteButton.disableProperty().set(selectionEmpty);
	}

	private void populateTableViewAsync() {
		new Service<Collection<Server>>() {
			@Override
			protected Task<Collection<Server>> createTask() {
				return new Task<Collection<Server>>() {
					@Override
					protected Collection<Server> call() throws Exception {
						return ServerRegistry.getInstance().getServers();
					}

					@Override
					protected void succeeded() {
						final Collection<Server> servers;
						try { servers = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
						populateTableViewCallback(servers);
					}
				};
			}
		}.start();
	}

	private void populateTableViewCallback(final Collection<Server> servers) {
		for (final Server server : servers)
			tableView.getItems().add(new ServerListItem(server));

		tableView.requestLayout();
	}

	@FXML
	private void addButtonClicked(final ActionEvent event) {
		System.out.println("addButtonClicked: " + event);
		Server server = new Server();

	}

	@FXML
	private void editButtonClicked(final ActionEvent event) {
		System.out.println("editButtonClicked: " + event);
	}

	@FXML
	private void deleteButtonClicked(final ActionEvent event) {
		System.out.println("deleteButtonClicked: " + event);
	}
}
