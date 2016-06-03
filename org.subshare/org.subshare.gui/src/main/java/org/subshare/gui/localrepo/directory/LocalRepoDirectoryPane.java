package org.subshare.gui.localrepo.directory;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;
import static org.subshare.gui.util.PlatformUtil.*;

import java.lang.ref.WeakReference;
import java.util.List;

import org.subshare.core.repo.LocalRepo;
import org.subshare.gui.filetree.DirectoryFileTreeItem;
import org.subshare.gui.filetree.FileFileTreeItem;
import org.subshare.gui.filetree.FileTreeItem;
import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.filetree.repoaware.RepoAwareFileTreePane;
import org.subshare.gui.histo.HistoryPaneContainer;
import org.subshare.gui.histo.HistoryPaneSupport;
import org.subshare.gui.invitation.issue.IssueInvitationData;
import org.subshare.gui.invitation.issue.IssueInvitationWizard;
import org.subshare.gui.ls.RepoSyncDaemonLs;
import org.subshare.gui.wizard.WizardDialog;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;
import javafx.beans.InvalidationListener;
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
	private Button exportFromHistoryButton;

	private WeakReference<SecurityPane> securityPaneRef;

	@SuppressWarnings("unused")
	private final HistoryPaneSupport historyPaneSupport;

	public LocalRepoDirectoryPane(final LocalRepo localRepo, final File file) {
		this.localRepo = assertNotNull("localRepo", localRepo);
		this.file = assertNotNull("file", file);
		loadDynamicComponentFxml(LocalRepoDirectoryPane.class, this);

		final String path = file.getAbsolutePath();
		pathTextField.setText(path);

		fileTreePane.setUseCase(String.format("localRepo:%s:%s", localRepo.getRepositoryId(), path)); //$NON-NLS-1$
		fileTreePane.setRootFileTreeItem(new RootDirectoryFileTreeItem(fileTreePane, file));
		fileTreePane.setLocalRepo(localRepo);

		historyPaneSupport = new HistoryPaneSupport(this);

		tabPane.getSelectionModel().selectedItemProperty().addListener((InvalidationListener) observable -> createOrForgetSecurityPane());
		createOrForgetSecurityPane();
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

	private static class RootDirectoryFileTreeItem extends DirectoryFileTreeItem {
		private final FileTreePane fileTreePane;

		public RootDirectoryFileTreeItem(FileTreePane fileTreePane, File file) {
			super(file); // file is null-checked by super-constructor.
			this.fileTreePane = assertNotNull("fileTreePane", fileTreePane);
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
	public Button getExportFromHistoryButton() {
		return exportFromHistoryButton;
	}
}
