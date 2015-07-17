package org.subshare.gui.localrepo.directory;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static org.subshare.core.file.FileConst.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;

import org.subshare.core.dto.PermissionType;
import org.subshare.core.repo.LocalRepo;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRepoInvitationManager;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.gui.filetree.DirectoryFileTreeItem;
import org.subshare.gui.filetree.FileFileTreeItem;
import org.subshare.gui.filetree.FileTreeItem;
import org.subshare.gui.filetree.FileTreePane;
import org.subshare.gui.ls.LocalRepoManagerFactoryLs;
import org.subshare.gui.ls.RepoSyncDaemonLs;
import org.subshare.gui.ls.UserRegistryLs;
import org.subshare.gui.ls.UserRepoInvitationManagerLs;
import org.subshare.gui.selectuser.SelectUserDialog;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;
import co.codewizards.cloudstore.core.util.IOUtil;

public class LocalRepoDirectoryPane extends GridPane {

	private final LocalRepo localRepo;
	private final File file;

	@FXML
	private TextField pathTextField;
	@FXML
	private FileTreePane fileTreePane;

	public LocalRepoDirectoryPane(final LocalRepo localRepo, final File file) {
		this.localRepo = assertNotNull("localRepo", localRepo);
		this.file = assertNotNull("file", file);
		loadDynamicComponentFxml(LocalRepoDirectoryPane.class, this);

		final String path = file.getAbsolutePath();
		pathTextField.setText(path);

		fileTreePane.setUseCase(String.format("localRepo:%s:%s", localRepo.getRepositoryId(), path)); //$NON-NLS-1$
		fileTreePane.setRootFileTreeItem(new DirectoryFileTreeItem(file) {
			{
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
		});
	}

	@FXML
	private void syncButtonClicked(final ActionEvent event) {
		final RepoSyncDaemon repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
		repoSyncDaemon.startSync(localRepo.getLocalRoot());
	}

	// TODO BEGIN replace this code which is (mostly) redundant with the one in LocalRepoDirectoryPane!
	@FXML
	private void inviteButtonClicked(final ActionEvent event) {
		final List<User> invitees = selectInvitees();
		if (invitees == null || invitees.isEmpty())
			return;

		final File directory = selectDirectory();
		if (directory == null)
			return;

		final File localRoot = localRepo.getLocalRoot();
		try (final LocalRepoManager localRepoManager = LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRoot);) {
			final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManagerLs.getUserRepoInvitationManager(getUserRegistry(), localRepoManager);

			final PermissionType permissionType = PermissionType.write; // TODO select in UI!
			final long validityDurationMillis = 5L * 24L * 3600L; // TODO UI!

			final String localPath;
			try {
				localPath = IOUtil.getRelativePath(localRoot, file).replace(java.io.File.separatorChar, '/');
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
			for (final User invitee : invitees) {
				final UserRepoInvitationToken userRepoInvitationToken = userRepoInvitationManager.createUserRepoInvitationToken(localPath, invitee, permissionType, validityDurationMillis);
				final byte[] data = userRepoInvitationToken.getSignedEncryptedUserRepoInvitationData();
				final String fileName = getFileName(invitee);
				final File file = createFile(directory, fileName);
				try {
					try (final OutputStream out = file.createOutputStream();) {
						out.write(data);
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		// We *must* sync immediately to make sure that our invitation-user-repo-keys are known to the server!
		// Otherwise the invited users cannot connect the repositories (their signature wouldn't be known).
		final RepoSyncDaemon repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
		repoSyncDaemon.startSync(localRoot);
	}

	private static String getFileName(final User invitee) {
		assertNotNull("invitee", invitee);
		final StringBuilder sb = new StringBuilder();

		final String firstName = invitee.getFirstName();
		if (! isEmpty(firstName))
			sb.append(firstName);

		if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_')
			sb.append('_');

		final String lastName = invitee.getLastName();
		if (! isEmpty(lastName))
			sb.append(lastName);

		if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_')
			sb.append('_');

		if (! invitee.getEmails().isEmpty())
			sb.append(invitee.getEmails().get(0));

		if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_')
			sb.append('_');

		sb.append(invitee.getUserId());
		sb.append('_');
		sb.append(new Uid());

		sb.append(SUBSHARE_FILE_EXTENSION);
		return sb.toString();
	}

	private UserRegistry getUserRegistry() {
		return UserRegistryLs.getUserRegistry();
	}

	private List<User> selectInvitees() {
		final List<User> users = new ArrayList<>(getUserRegistry().getUsers());
		// TODO we should filter the repository's owner out! It makes no sense to invite yourself.

		SelectUserDialog dialog = new SelectUserDialog(getScene().getWindow(), users, null, SelectionMode.MULTIPLE,
				"Please select one or more users you want to invite.\n\nA separate invitation token is created for each of them.");
		dialog.showAndWait();
		final List<User> selectedUsers = dialog.getSelectedUsers();
		return selectedUsers;
	}

	private File selectDirectory() {
		// TODO implement our own directory-selection-dialog which allows for showing some more information to the user.
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Where should we put the invitation tokens?");
		final java.io.File directory = directoryChooser.showDialog(getScene().getWindow());
		return directory == null ? null : createFile(directory).getAbsoluteFile();
	}
	// TODO END replace this code which is (mostly) redundant.
}
