package org.subshare.gui.server;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import javafx.stage.DirectoryChooser;

import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.ServerRepoRegistry;
import org.subshare.core.server.Server;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.ls.PgpPrivateKeyPassphraseManagerLs;
import org.subshare.gui.ls.ServerRepoManagerLs;
import org.subshare.gui.ls.ServerRepoRegistryLs;
import org.subshare.gui.ls.UserRegistryLs;

import co.codewizards.cloudstore.core.oio.File;

public class ServerPane extends BorderPane /* GridPane */ {

	private ServerRepoRegistry serverRepoRegistry;

	private final Server server;

	@FXML
	private Button createRepositoryButton;

	@FXML
	private TableView<ServerRepoListItem> tableView;

	private final ListChangeListener<ServerRepoListItem> selectionListener = new ListChangeListener<ServerRepoListItem>() {
		@Override
		public void onChanged(final ListChangeListener.Change<? extends ServerRepoListItem> c) {
			updateEnabled();
		}
	};

	private PropertyChangeListener serverReposPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			@SuppressWarnings("unchecked")
			final List<ServerRepo> serverRepos = new ArrayList<ServerRepo>((List<ServerRepo>) evt.getNewValue());

			for (Iterator<ServerRepo> it = serverRepos.iterator(); it.hasNext();) {
				if (!server.getServerId().equals(it.next().getServerId()))
					it.remove();
			}

			addOrRemoveItemTablesViewCallback(serverRepos);
		}
	};

	private PropertyChangeListener serverRepoPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(final PropertyChangeEvent evt) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
//					final ServerRepo serverRepo = (ServerRepo) evt.getSource();
					// workaround for refresh bug
					List<TableColumn<ServerRepoListItem, ?>> columns = new ArrayList<>(tableView.getColumns());
					tableView.getColumns().clear();
					tableView.getColumns().addAll(columns);
				}
			});
		}
	};

	public ServerPane(final Server server) {
		this.server = assertNotNull("server", server);
		loadDynamicComponentFxml(ServerPane.class, this);
		tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		tableView.getSelectionModel().getSelectedItems().addListener(selectionListener);
		for (final TableColumn<ServerRepoListItem, ?> tableColumn : tableView.getColumns()) {
			if ("nameColumn".equals(tableColumn.getId())) {
				final TableColumn<ServerRepoListItem, String> tc = cast(tableColumn);
				tc.setCellFactory(cast(TextFieldTableCell.forTableColumn()));
				tc.setOnEditCommit(new EventHandler<CellEditEvent<ServerRepoListItem, String>>() {
					@Override
					public void handle(CellEditEvent<ServerRepoListItem, String> event) {
						event.getRowValue().setName(event.getNewValue());
						getServerRepoRegistry().writeIfNeeded();
					}
				});
			}
		}
		populateTableViewAsync();
		updateEnabled();
	}

	private void addOrRemoveItemTablesViewCallback(final Collection<ServerRepo> serverRepos) {
		final Set<ServerRepo> modelRepos = new HashSet<ServerRepo>(serverRepos);
		final Map<ServerRepo, ServerRepoListItem> viewRepo2ServerRepoListItem = new HashMap<>();
		for (final ServerRepoListItem sli : tableView.getItems())
			viewRepo2ServerRepoListItem.put(sli.getServerRepo(), sli);

		for (final ServerRepo serverRepo : serverRepos) {
			if (! viewRepo2ServerRepoListItem.containsKey(serverRepo)) {
				final ServerRepoListItem sli = new ServerRepoListItem(serverRepo);
				viewRepo2ServerRepoListItem.put(serverRepo, sli);
				tableView.getItems().add(sli);
			}
		}

		if (modelRepos.size() < viewRepo2ServerRepoListItem.size()) {
			for (final ServerRepo serverRepo : modelRepos)
				viewRepo2ServerRepoListItem.remove(serverRepo);

			for (final ServerRepoListItem sli : viewRepo2ServerRepoListItem.values())
				tableView.getItems().remove(sli);
		}
	}

	private void addTableItemsViewCallback(final Collection<ServerRepo> serverRepos) {
		for (final ServerRepo serverRepo : serverRepos)
			tableView.getItems().add(new ServerRepoListItem(serverRepo));

		tableView.requestLayout();
	}

	private void updateEnabled() {
		final boolean selectionEmpty = tableView.getSelectionModel().getSelectedItems().isEmpty();
//		deleteButton.setDisable(selectionEmpty);
	}

	private void populateTableViewAsync() {
		new Service<Collection<ServerRepo>>() {
			@Override
			protected Task<Collection<ServerRepo>> createTask() {
				return new SsTask<Collection<ServerRepo>>() {
					@Override
					protected Collection<ServerRepo> call() throws Exception {
						return getServerRepoRegistry().getServerReposOfServer(server.getServerId());
					}

					@Override
					protected void succeeded() {
						final Collection<ServerRepo> serverRepos;
						try { serverRepos = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
						addTableItemsViewCallback(serverRepos);
					}
				};
			}
		}.start();
	}

	protected ServerRepoRegistry getServerRepoRegistry() {
		if (serverRepoRegistry == null) {
			serverRepoRegistry = ServerRepoRegistryLs.getServerRepoRegistry();
			serverRepoRegistry.addPropertyChangeListener(ServerRepoRegistry.PropertyEnum.serverRepos, serverReposPropertyChangeListener);
			serverRepoRegistry.addPropertyChangeListener(ServerRepoRegistry.PropertyEnum.serverRepos_serverRepo, serverRepoPropertyChangeListener);
		}
		return serverRepoRegistry;
	}


	@Override
	protected void finalize() throws Throwable {
		final ServerRepoRegistry serverRepoRegistry = this.serverRepoRegistry;
		if (serverRepoRegistry != null) {
			serverRepoRegistry.removePropertyChangeListener(ServerRepoRegistry.PropertyEnum.serverRepos, serverReposPropertyChangeListener);
			serverRepoRegistry.removePropertyChangeListener(ServerRepoRegistry.PropertyEnum.serverRepos_serverRepo, serverRepoPropertyChangeListener);
		}
		super.finalize();
	}

	@FXML
	private void createRepositoryButtonClicked(final ActionEvent event) {
		final File directory = selectLocalDirectory();
		if (directory == null)
			return;

		final Set<PgpKeyId> pgpKeyIds = PgpPrivateKeyPassphraseManagerLs.getPgpPrivateKeyPassphraseStore().getPgpKeyIdsHavingPassphrase();
		if (pgpKeyIds.isEmpty())
			throw new IllegalStateException("There is no PGP private key unlocked.");

		final UserRegistry userRegistry = UserRegistryLs.getUserRegistry();
		final Collection<User> users = userRegistry.getUsersByPgpKeyIds(pgpKeyIds);

		if (users.isEmpty())
			throw new IllegalStateException("There is no user for any of these PGP keys: " + pgpKeyIds);

		User user = users.iterator().next(); // TODO select via UI, if there's more than one!

		ServerRepoManagerLs.getServerRepoManager().createRepository(directory, server, user);

		// TODO really create the repo on the server!
		// TODO 2: need to verify, if server-URLs and repos really exist! Maybe show an error marker in the UI, if there's a problem (might be temporary!)
		// TODO 3: and maybe switch from UUID to Uid?!
	}

	private File selectLocalDirectory() {
		// TODO implement our own directory-selection-dialog which allows for showing some more information to the user.
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Select local directory to be shared.");
		final java.io.File directory = directoryChooser.showDialog(getScene().getWindow());
		return directory == null ? null : createFile(directory);
	}
}
