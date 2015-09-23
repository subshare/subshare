package org.subshare.gui.server;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static javafx.application.Platform.*;
import static org.subshare.gui.util.FxmlUtil.*;
import static org.subshare.gui.util.PlatformUtil.*;

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

import javafx.collections.ListChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.ServerRepoRegistry;
import org.subshare.core.server.Server;
import org.subshare.gui.checkout.CheckOutWizard;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.createrepo.CreateRepoData;
import org.subshare.gui.createrepo.CreateRepoWizard;
import org.subshare.gui.ls.ServerRepoRegistryLs;
import org.subshare.gui.wizard.WizardDialog;

public class ServerPane extends GridPane {

	private ServerRepoRegistry serverRepoRegistry;

	private final Server server;

	@FXML
	private Button createRepositoryButton;

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

			runLater(() -> addOrRemoveItemTablesViewCallback(serverRepos));
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
//				tc.setOnEditCommit(new EventHandler<CellEditEvent<ServerRepoListItem, String>>() { // not needed, anymore, because we use a StringProperty, now - only needed with plain getters+setters.
//					@Override
//					public void handle(CellEditEvent<ServerRepoListItem, String> event) {
//						event.getRowValue().setName(event.getNewValue());
//						getServerRepoRegistry().writeIfNeeded();
//					}
//				});
			}
		}
		populateTableViewAsync();
		updateEnabled();
	}

	private void addOrRemoveItemTablesViewCallback(final Collection<ServerRepo> serverRepos) {
		assertFxApplicationThread();
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
			addWeakPropertyChangeListener(serverRepoRegistry, ServerRepoRegistry.PropertyEnum.serverRepos, serverReposPropertyChangeListener);
		}
		return serverRepoRegistry;
	}

	@FXML
	private void createRepositoryButtonClicked(final ActionEvent event) {
		final CreateRepoData createRepoData = new CreateRepoData(server);
		final CreateRepoWizard wizard = new CreateRepoWizard(createRepoData);
		final WizardDialog dialog = new WizardDialog(getScene().getWindow(), wizard);
		dialog.show();
	}

	@FXML
	private void checkOutButtonClicked(final ActionEvent event) {
		final ServerRepoListItem serverRepoListItem = tableView.getSelectionModel().getSelectedItems().get(0);
		final ServerRepo serverRepo = serverRepoListItem.getServerRepo();
		new CheckOutWizard(server, serverRepo).checkOut(getScene().getWindow());
	}

	@FXML
	private void syncButtonClicked(final ActionEvent event) {
		// TODO find out all local repositories that are connected to the server repositories and sync them!
		for (ServerRepoListItem item : tableView.getSelectionModel().getSelectedItems())
			item.getServerRepo().getRepositoryId();
	}
}
