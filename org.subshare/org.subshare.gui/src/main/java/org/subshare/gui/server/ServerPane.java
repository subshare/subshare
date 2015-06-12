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
import org.subshare.gui.ls.RepoSyncDaemonLs;
import org.subshare.gui.ls.ServerRepoManagerLs;
import org.subshare.gui.ls.ServerRepoRegistryLs;
import org.subshare.gui.ls.UserRegistryLs;
import org.subshare.gui.selectuser.SelectUserDialog;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;

public class ServerPane extends BorderPane /* GridPane */ {

	private ServerRepoRegistry serverRepoRegistry;

	private final Server server;

	@FXML
	private Button createRepositoryButton;

//	@FXML
//	private Button syncButton;

	@FXML
	private Button checkOutButton;

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
		final int selectionCount = tableView.getSelectionModel().getSelectedItems().size();

		checkOutButton.setDisable(selectionCount != 1);

//		// TODO find out whether local repositories are connected to the server repositories (otherwise there's nothing to sync)!
//		syncButton.setDisable(selectionCount == 0);
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
		final File directory = selectLocalDirectory("Select local directory to be shared (upload).");
		if (directory == null)
			return;

		final Set<PgpKeyId> pgpKeyIds = PgpPrivateKeyPassphraseManagerLs.getPgpPrivateKeyPassphraseStore().getPgpKeyIdsHavingPassphrase();
		if (pgpKeyIds.isEmpty())
			throw new IllegalStateException("There is no PGP private key unlocked."); // TODO show nice message and ask the user, if he would like to unlock.

		final UserRegistry userRegistry = UserRegistryLs.getUserRegistry();
		final Collection<User> users = userRegistry.getUsersByPgpKeyIds(pgpKeyIds);

		if (users.isEmpty())
			throw new IllegalStateException("There is no user for any of these PGP keys: " + pgpKeyIds); // TODO should we ask to unlock (further) PGP keys?

		final User owner;
		if (users.size() == 1)
			owner = users.iterator().next();
		else {
			owner = selectOwner(new ArrayList<>(users));
			if (owner == null)
				return; // user cancelled the selection dialog.
		}

		// TODO do this in the background!
		ServerRepoManagerLs.getServerRepoManager().createRepository(directory, server, owner);

		// ...immediately sync after creation. This happens in the background (this method is non-blocking).
		final RepoSyncDaemon repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
		repoSyncDaemon.startSync(directory);

		// TO DO really create the repo on the server! => DONE!
		// TODO 2: need to verify, if server-URLs and repos really exist! Maybe show an error marker in the UI, if there's a problem (might be temporary!)
		// TODO 3: and maybe switch from UUID to Uid?!
	}

	@FXML
	private void checkOutButtonClicked(final ActionEvent event) {
		final ServerRepoListItem serverRepoListItem = tableView.getSelectionModel().getSelectedItems().get(0);
		final ServerRepo serverRepo = serverRepoListItem.getServerRepo();

		final File directory = selectLocalDirectory("Select local directory for check-out (download).");
		if (directory == null)
			return;

		// TODO do this in the background!
		ServerRepoManagerLs.getServerRepoManager().checkOutRepository(server, serverRepo, directory);

		// ...immediately sync after check-out. This happens in the background (this method is non-blocking).
		final RepoSyncDaemon repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
		repoSyncDaemon.startSync(directory);
	}

	@FXML
	private void syncButtonClicked(final ActionEvent event) {
		// TODO find out all local repositories that are connected to the server repositories and sync them!
		for (ServerRepoListItem item : tableView.getSelectionModel().getSelectedItems())
			item.getServerRepo().getRepositoryId();
	}

	private User selectOwner(final List<User> users) {
		SelectUserDialog dialog = new SelectUserDialog(getScene().getWindow(), users, null, SelectionMode.SINGLE,
				"There are multiple users in the user database having an unlocked private PGP key.\n\nPlease select the user who should become the owner of the new repository.");
		dialog.showAndWait();
		final List<User> selectedUsers = dialog.getSelectedUsers();
		return selectedUsers == null || selectedUsers.isEmpty() ? null : selectedUsers.get(0);
	}

	private File selectLocalDirectory(final String title) {
		// TODO implement our own directory-selection-dialog which allows for showing some more information to the user.
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(title);
		final java.io.File directory = directoryChooser.showDialog(getScene().getWindow());
		return directory == null ? null : createFile(directory).getAbsoluteFile();
	}
}
