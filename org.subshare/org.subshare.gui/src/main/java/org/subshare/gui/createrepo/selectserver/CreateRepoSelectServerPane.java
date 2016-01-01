package org.subshare.gui.createrepo.selectserver;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.InvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;

import org.subshare.core.server.Server;
import org.subshare.gui.createrepo.CreateRepoData;
import org.subshare.gui.ls.ServerRegistryLs;

public abstract class CreateRepoSelectServerPane extends GridPane {

	private final CreateRepoData createRepoData;

	@FXML
	private TableView<ServerListItem> tableView;

	public CreateRepoSelectServerPane(final CreateRepoData createRepoData) {
		this.createRepoData = assertNotNull("createRepoData", createRepoData);
		loadDynamicComponentFxml(CreateRepoSelectServerPane.class, this);
		tableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		tableView.getSelectionModel().selectedItemProperty().addListener((InvalidationListener) observable -> onSelectionChanged());
		populateTableView();
		updateComplete();
	}

	private void populateTableView() {
		Map<Server, ServerListItem> server2ServerListItem = new HashMap<>();
		for (Server server : ServerRegistryLs.getServerRegistry().getServers()) {
			ServerListItem item = new ServerListItem(server);
			server2ServerListItem.put(server, item);
			tableView.getItems().add(item);
		}
		ServerListItem serverListItem = server2ServerListItem.get(createRepoData.getServer());
		tableView.getSelectionModel().clearSelection();
		tableView.getSelectionModel().select(serverListItem);
	}

	private void onSelectionChanged() {
		ServerListItem serverListItem = tableView.getSelectionModel().selectedItemProperty().get();
		Server server = serverListItem == null ? null : serverListItem.getServer();
		createRepoData.setServer(server);
		updateComplete();
	}

	protected boolean isComplete() {
		return createRepoData.getServer() != null;
	}

	protected abstract void updateComplete();

	@Override
	public void requestFocus() {
		super.requestFocus();
		tableView.requestFocus();
	}
}
