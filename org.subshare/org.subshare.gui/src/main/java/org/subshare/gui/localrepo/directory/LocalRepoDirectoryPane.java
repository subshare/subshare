package org.subshare.gui.localrepo.directory;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import static org.subshare.gui.util.PlatformUtil.*;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.subshare.core.dto.CollisionPrivateDto;
import org.subshare.core.repo.LocalRepo;
import org.subshare.gui.filetree.DirectoryFileTreeItem;
import org.subshare.gui.filetree.FileFileTreeItem;
import org.subshare.gui.filetree.FileTreeItem;
import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.filetree.repoaware.CollisionPrivateDtoSet;
import org.subshare.gui.filetree.repoaware.RepoAwareFileTreePane;
import org.subshare.gui.histo.HistoryPaneContainer;
import org.subshare.gui.histo.HistoryPaneSupport;
import org.subshare.gui.invitation.issue.IssueInvitationData;
import org.subshare.gui.invitation.issue.IssueInvitationWizard;
import org.subshare.gui.ls.RepoSyncDaemonLs;
import org.subshare.gui.resolvecollision.ResolveCollisionData;
import org.subshare.gui.resolvecollision.ResolveCollisionWizard;
import org.subshare.gui.wizard.WizardDialog;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableSet;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LocalRepoDirectoryPane extends VBox implements HistoryPaneContainer {

	private final LocalRepo localRepo;
	private final File file;

	@FXML
	private TabPane tabPane;

	@FXML
	private Tab generalTab;

	@FXML
	private Tab historyTab;

	@FXML
	private Tab securityTab;

	@FXML
	private TextField pathTextField;

	@FXML
	private RepoAwareFileTreePane fileTreePane;

	@FXML
	private Button resolveCollisionInFileTreeButton;

	@FXML
	private Button resolveCollisionInHistoryButton;

	@FXML
	private Button refreshButton;

	@FXML
	private Button exportFromHistoryButton;

	private WeakReference<SecurityPane> securityPaneRef;

	@SuppressWarnings("unused")
	private final HistoryPaneSupport historyPaneSupport;

	public LocalRepoDirectoryPane(final LocalRepo localRepo, final File file) {
		this.localRepo = assertNotNull(localRepo, "localRepo");
		this.file = assertNotNull(file, "file");
		loadDynamicComponentFxml(LocalRepoDirectoryPane.class, this);

		final String path = file.getAbsolutePath();
		pathTextField.setText(path);

		fileTreePane.setUseCase(String.format("localRepo:%s:%s", localRepo.getRepositoryId(), path)); //$NON-NLS-1$
		fileTreePane.setRootFileTreeItem(new RootDirectoryFileTreeItem(fileTreePane, file));
		fileTreePane.setLocalRepo(localRepo);
		fileTreePane.getSelectedFiles().addListener((InvalidationListener) observable -> updateResolveCollisionInFileTreeButtonDisable());
		updateResolveCollisionInFileTreeButtonDisable();

		historyPaneSupport = new HistoryPaneSupport(this);

		tabPane.getSelectionModel().selectedItemProperty().addListener((InvalidationListener) observable -> createOrForgetSecurityPane());
		createOrForgetSecurityPane();

		tabPane.getSelectionModel().selectedItemProperty().addListener((InvalidationListener) observable -> updateButtonVisible());
		updateButtonVisible();
	}

	private void createOrForgetSecurityPane() {
		assertFxApplicationThread();

		if (securityTab != tabPane.getSelectionModel().getSelectedItem()) {
			securityTab.setContent(null);
			return;
		}

		SecurityPane securityPane = securityPaneRef == null ? null : securityPaneRef.get();
		if (securityPane == null) {
			securityPane = new SecurityPane(localRepo, file) {
				@Override
				protected void startSync() {
					LocalRepoDirectoryPane.this.startSync();
				}
			};
			securityPaneRef = new WeakReference<>(securityPane);
		}

		if (securityTab.getContent() == null)
			securityTab.setContent(securityPane);
	}

	private void updateButtonVisible() {
		refreshButton.setVisible(tabPane.getSelectionModel().getSelectedItem() == generalTab);
		refreshButton.setManaged(refreshButton.isVisible());

		resolveCollisionInFileTreeButton.setVisible(tabPane.getSelectionModel().getSelectedItem() == generalTab);
		resolveCollisionInFileTreeButton.setManaged(resolveCollisionInFileTreeButton.isVisible());
	}

	private void updateResolveCollisionInFileTreeButtonDisable() {
		final Collection<CollisionPrivateDto> collisionPrivateDtos = getSelectedFileTreeCollisionPrivateDtos();
		resolveCollisionInFileTreeButton.setDisable(collisionPrivateDtos.isEmpty());
	}

	private Collection<CollisionPrivateDto> getSelectedFileTreeCollisionPrivateDtos() {
		final ObservableSet<File> selectedFiles = fileTreePane.getSelectedFiles();
		final List<CollisionPrivateDto> collisionPrivateDtos = new ArrayList<>();
		final Set<Uid> collisionIds = new HashSet<>();
		for (final File file : selectedFiles) {
			final FileTreeItem<?> treeItem = fileTreePane.getRootFileTreeItem().findFirst(file);
			if (treeItem != null) {
				final CollisionPrivateDtoSet collisionPrivateDtoSet = fileTreePane.getCollisionDtoSet(treeItem);
				if (collisionPrivateDtoSet != null) {
					for (CollisionPrivateDto collisionPrivateDto : collisionPrivateDtoSet.getAllCollisionPrivateDtos()) {
						if (collisionIds.add(collisionPrivateDto.getCollisionId()))
							collisionPrivateDtos.add(collisionPrivateDto);
					}
				}
			}
		}
		return collisionPrivateDtos;
	}

	@FXML
	private void syncButtonClicked(final ActionEvent event) {
		startSync();
	}

	private void startSync() {
		final RepoSyncDaemon repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
		repoSyncDaemon.startSync(localRepo.getLocalRoot());
	}

	@FXML
	private void inviteButtonClicked(final ActionEvent event) {
		final IssueInvitationWizard wizard = new IssueInvitationWizard(new IssueInvitationData(localRepo, file));
		final WizardDialog dialog = new WizardDialog(getScene().getWindow(), wizard);
		dialog.show(); // no need to wait ;-)
	}

	@FXML
	private void refreshButtonClicked(final ActionEvent event) {
		fileTreePane.refresh();
	}

	@FXML
	private void resolveCollisionInFileTreeButtonClicked(final ActionEvent event) {
		final Collection<CollisionPrivateDto> collisionPrivateDtos = getSelectedFileTreeCollisionPrivateDtos();
		final Set<Uid> collisionIds = new HashSet<>(collisionPrivateDtos.size());
		for (CollisionPrivateDto collisionPrivateDto : collisionPrivateDtos)
			collisionIds.add(collisionPrivateDto.getCollisionId());

		final ResolveCollisionData resolveCollisionData = new ResolveCollisionData(localRepo, collisionIds);
		final ResolveCollisionWizard wizard = new ResolveCollisionWizard(resolveCollisionData);
		final WizardDialog dialog = new WizardDialog(tabPane.getScene().getWindow(), wizard);
		dialog.show(); // no need to wait ;-)
	}

	private static class RootDirectoryFileTreeItem extends DirectoryFileTreeItem {
		private final FileTreePane fileTreePane;

		public RootDirectoryFileTreeItem(FileTreePane fileTreePane, File file) {
			super(file); // file is null-checked by super-constructor.
			this.fileTreePane = assertNotNull(fileTreePane, "fileTreePane");
			hookUpdateInvalidationListener(fileTreePane);
		}

		@Override
		protected FileTreePane getFileTreePane() {
			return fileTreePane;
		}

		@Override
		protected List<FileTreeItem<?>> loadChildren() {
			final List<FileTreeItem<?>> children = super.loadChildren();
			children.removeIf(fti
					-> (fti instanceof FileFileTreeItem)
					&& ((FileFileTreeItem) fti).getFile().getName().equals(LocalRepoManager.META_DIR_NAME));
			return children;
		}
	}

	@Override
	public LocalRepo getLocalRepo() {
		return localRepo;
	}

	@Override
	public String getLocalPath() {
		return localRepo.getLocalPath(file);
	}

	@Override
	public TabPane getTabPane() {
		return tabPane;
	}

	@Override
	public Tab getHistoryTab() {
		return historyTab;
	}

	@Override
	public Button getResolveCollisionInHistoryButton() {
		return resolveCollisionInHistoryButton;
	}

	@Override
	public Button getExportFromHistoryButton() {
		return exportFromHistoryButton;
	}
}
