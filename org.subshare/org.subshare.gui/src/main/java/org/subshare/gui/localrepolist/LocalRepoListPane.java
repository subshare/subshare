package org.subshare.gui.localrepolist;

import static co.codewizards.cloudstore.core.bean.PropertyChangeListenerUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static javafx.application.Platform.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.LocalRepoRegistry;
import org.subshare.gui.IconSize;
import org.subshare.gui.concurrent.SsTask;
import org.subshare.gui.createrepo.CreateRepoData;
import org.subshare.gui.createrepo.CreateRepoWizard;
import org.subshare.gui.invitation.accept.AcceptInvitationWizard;
import org.subshare.gui.ls.LocalRepoRegistryLs;
import org.subshare.gui.ls.RepoSyncDaemonLs;
import org.subshare.gui.severity.SeverityImageRegistry;
import org.subshare.gui.wizard.WizardDialog;

import co.codewizards.cloudstore.core.Severity;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncActivity;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncState;
import co.codewizards.cloudstore.core.util.StringUtil;
import javafx.beans.InvalidationListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

public class LocalRepoListPane extends GridPane {
	private static final Image syncIcon = new Image(LocalRepoListPane.class.getResource("sync_16x16.png").toExternalForm());

	@FXML
	private Button syncAllButton;

	@FXML
	private Button syncButton;

	@FXML
	private TableColumn<LocalRepoListItem, Set<RepoSyncActivity>> repoSyncActivityIconColumn;

	@FXML
	private TableColumn<LocalRepoListItem, Severity> severityIconColumn;

	@FXML
	private TableView<LocalRepoListItem> tableView;

	private LocalRepoRegistry localRepoRegistry;
	private RepoSyncDaemon repoSyncDaemon;

	private final PropertyChangeListener localReposPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			@SuppressWarnings("unchecked")
			final Set<LocalRepo> localRepos = new LinkedHashSet<LocalRepo>((List<LocalRepo>) evt.getNewValue());
			runLater(() -> addOrRemoveTableItemsViewCallback(localRepos));
		}
	};

	private final PropertyChangeListener repoSyncDaemonPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(final PropertyChangeEvent evt) {
			runLater(() -> {
				for (LocalRepoListItem localRepoListItem : tableView.getItems())
					updateSyncStates(localRepoListItem);
			});
		}
	};

	public LocalRepoListPane() {
		loadDynamicComponentFxml(LocalRepoListPane.class, this);

		repoSyncActivityIconColumn.setCellFactory(l -> new TableCell<LocalRepoListItem, Set<RepoSyncActivity>>() {
			@Override
			public void updateItem(final Set<RepoSyncActivity> activities, final boolean empty) {
				ImageView imageView = null;
				Tooltip tooltip = null;
				if (activities != null && ! activities.isEmpty()) {
					final LocalRepoListItem listItem = (LocalRepoListItem) getTableRow().getItem();
					if (listItem == null)
						return;

					final String tooltipText = listItem.getTooltipText();

					imageView = new ImageView(syncIcon);

					if (!StringUtil.isEmpty(tooltipText))
						tooltip = new Tooltip(tooltipText);
				}
				setGraphic(imageView);
				setTooltip(tooltip);
			}
		});

		severityIconColumn.setCellFactory(l -> new TableCell<LocalRepoListItem, Severity>() {
			@Override
			public void updateItem(final Severity severity, final boolean empty) {
				ImageView imageView = null;
				Tooltip tooltip = null;
				if (severity != null) {
					final LocalRepoListItem listItem = (LocalRepoListItem) getTableRow().getItem();
					if (listItem == null)
						return;

					final String tooltipText = listItem.getTooltipText();

					imageView = new ImageView(SeverityImageRegistry.getInstance().getImage(severity, IconSize._16x16));

					if (!StringUtil.isEmpty(tooltipText))
						tooltip = new Tooltip(tooltipText);
				}
				setGraphic(imageView);
				setTooltip(tooltip);
			}
		});

		tableView.getSelectionModel().getSelectedItems().addListener((InvalidationListener) observable -> updateEnabled());

		populateTableViewAsync();
		updateEnabled();
	}

	private void updateEnabled() {
		final boolean selectionEmpty = tableView.getSelectionModel().getSelectedItems().isEmpty();
		syncButton.setDisable(selectionEmpty);
	}

	private void populateTableViewAsync() {
		new Service<Collection<LocalRepo>>() {
			@Override
			protected Task<Collection<LocalRepo>> createTask() {
				return new SsTask<Collection<LocalRepo>>() {
					@Override
					protected Collection<LocalRepo> call() throws Exception {
						getRepoSyncDaemon(); // hook listener
						return getLocalRepoRegistry().getLocalRepos();
					}

					@Override
					protected void succeeded() {
						final Collection<LocalRepo> localRepos;
						try { localRepos = get(); } catch (InterruptedException | ExecutionException e) { throw new RuntimeException(e); }
						addOrRemoveTableItemsViewCallback(localRepos);
					}
				};
			}
		}.start();
	}

	@FXML
	private void createRepositoryButtonClicked(final ActionEvent event) {
		final CreateRepoData createRepoData = new CreateRepoData();
		final CreateRepoWizard wizard = new CreateRepoWizard(createRepoData);
		final WizardDialog dialog = new WizardDialog(getScene().getWindow(), wizard);
		dialog.show();
	}

	@FXML
	private void syncAllButtonClicked(final ActionEvent event) {
		new Service<Void>() {
			@Override
			protected Task<Void> createTask() {
				return new SsTask<Void>() {
					@Override
					protected Void call() throws Exception {
						for (LocalRepo localRepo : getLocalRepoRegistry().getLocalRepos())
							getRepoSyncDaemon().startSync(localRepo.getLocalRoot());

						return null;
					}
				};
			}
		}.start();
	}

	@FXML
	private void syncButtonClicked(final ActionEvent event) {
		final List<File> selectedLocalRoots = new ArrayList<>();
		for (LocalRepoListItem li : tableView.getSelectionModel().getSelectedItems())
			selectedLocalRoots.add(li.getLocalRoot());

		new Service<Void>() {
			@Override
			protected Task<Void> createTask() {
				return new SsTask<Void>() {
					@Override
					protected Void call() throws Exception {
						for (File localRoot : selectedLocalRoots)
							getRepoSyncDaemon().startSync(localRoot);

						return null;
					}
				};
			}
		}.start();
	}

	@FXML
	private void acceptInvitationButtonClicked(final ActionEvent event) {
		AcceptInvitationWizard wizard = new AcceptInvitationWizard();
		WizardDialog dialog = new WizardDialog(getScene().getWindow(), wizard);
		dialog.show();
	}

	protected void updateSyncStates(LocalRepoListItem localRepoListItem) {
		final UUID localRepositoryId = localRepoListItem.getLocalRepo().getRepositoryId();

		final List<RepoSyncState> states = getRepoSyncDaemon().getStates(localRepositoryId);
		final RepoSyncState state = states.isEmpty() ? null : states.get(states.size() - 1); // last one!
		localRepoListItem.setRepoSyncState(state);

		final Set<RepoSyncActivity> activities = getRepoSyncDaemon().getActivities(localRepositoryId);
		System.err.println(activities);
		localRepoListItem.setRepoSyncActivities(activities);
	}

	protected LocalRepoRegistry getLocalRepoRegistry() {
		if (localRepoRegistry == null) {
			localRepoRegistry = LocalRepoRegistryLs.getLocalRepoRegistry();
			addWeakPropertyChangeListener(localRepoRegistry, LocalRepoRegistry.PropertyEnum.localRepos, localReposPropertyChangeListener);
		}
		return localRepoRegistry;
	}

	protected RepoSyncDaemon getRepoSyncDaemon() {
		if (repoSyncDaemon == null) {
			repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
			addWeakPropertyChangeListener(repoSyncDaemon, repoSyncDaemonPropertyChangeListener);
		}
		return repoSyncDaemon;
	}

	private void addOrRemoveTableItemsViewCallback(final Collection<LocalRepo> localRepos) {
		assertNotNull(localRepos, "localRepos");
		final Map<LocalRepo, LocalRepoListItem> viewLocalRepo2LocalRepoListItem = new HashMap<>();
		for (final LocalRepoListItem li : tableView.getItems())
			viewLocalRepo2LocalRepoListItem.put(li.getLocalRepo(), li);

		for (final LocalRepo localRepo : localRepos) {
			if (! viewLocalRepo2LocalRepoListItem.containsKey(localRepo)) {
				final LocalRepoListItem li = new LocalRepoListItem(localRepo);
				updateSyncStates(li);
				viewLocalRepo2LocalRepoListItem.put(localRepo, li);
				tableView.getItems().add(li);
			}
		}

		if (localRepos.size() < viewLocalRepo2LocalRepoListItem.size()) {
			for (final LocalRepo localRepo : localRepos)
				viewLocalRepo2LocalRepoListItem.remove(localRepo);

			for (final LocalRepoListItem li : viewLocalRepo2LocalRepoListItem.values())
				tableView.getItems().remove(li);
		}

//		tableView.requestLayout();
	}
}
