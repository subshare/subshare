package org.subshare.gui.localrepo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.util.function.UnaryOperator;

import javafx.beans.binding.Bindings;
import javafx.beans.property.adapter.JavaBeanObjectProperty;
import javafx.beans.property.adapter.JavaBeanObjectPropertyBuilder;
import javafx.beans.property.adapter.JavaBeanStringProperty;
import javafx.beans.property.adapter.JavaBeanStringPropertyBuilder;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.layout.GridPane;

import org.subshare.core.repo.LocalRepo;
import org.subshare.gui.invitation.issue.IssueInvitationData;
import org.subshare.gui.invitation.issue.IssueInvitationWizard;
import org.subshare.gui.ls.RepoSyncDaemonLs;
import org.subshare.gui.util.FileStringConverter;
import org.subshare.gui.wizard.WizardDialog;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;

public class LocalRepoPane extends GridPane {
	private final LocalRepo localRepo;

	@FXML
	private Button syncButton;

	@FXML
	private Button inviteButton;

	@FXML
	private TextField nameTextField;

	@FXML
	private TextField localRootTextField;

	private JavaBeanStringProperty nameProperty;

	private JavaBeanObjectProperty<File> localRootProperty;

	public LocalRepoPane(final LocalRepo localRepo) {
		this.localRepo = assertNotNull("localRepo", localRepo);
		loadDynamicComponentFxml(LocalRepoPane.class, this);
		nameTextField.setTextFormatter(new TextFormatter<String>(new UnaryOperator<Change>() {
			@Override
			public Change apply(Change change) {
				String text = change.getText();
				if (text.startsWith("_") && change.getRangeStart() == 0)
					return null;

				if (text.indexOf('/') >= 0)
					return null;

				return change;
			}
		}));
		bind();
	}

	private void bind() {
		try {
			// nameProperty must be kept as field to prevent garbage-collection!
			nameProperty = JavaBeanStringPropertyBuilder.create()
				    .bean(localRepo)
				    .name(LocalRepo.PropertyEnum.name.name())
				    .build();
			nameTextField.textProperty().bindBidirectional(nameProperty);

			localRootProperty = JavaBeanObjectPropertyBuilder.create()
					.bean(localRepo)
					.name(LocalRepo.PropertyEnum.localRoot.name())
					.build();

			Bindings.bindBidirectional(localRootTextField.textProperty(), localRootProperty, new FileStringConverter());
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@FXML
	private void syncButtonClicked(final ActionEvent event) {
		final RepoSyncDaemon repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
		repoSyncDaemon.startSync(localRepo.getLocalRoot());
	}

	@FXML
	private void inviteButtonClicked(final ActionEvent event) {
		final IssueInvitationWizard wizard = new IssueInvitationWizard(new IssueInvitationData(localRepo, localRepo.getLocalRoot()));
		final WizardDialog dialog = new WizardDialog(getScene().getWindow(), wizard);
		dialog.show(); // no need to wait ;-)
	}

//	// TODO BEGIN replace this code which is (mostly) redundant with the one in LocalRepoDirectoryPane!
//	@FXML
//	private void inviteButtonClicked(final ActionEvent event) {
//		final List<User> invitees = selectInvitees();
//		if (invitees == null || invitees.isEmpty())
//			return;
//
//		final File directory = selectDirectory();
//		if (directory == null)
//			return;
//
//		final File localRoot = localRepo.getLocalRoot();
//		try (final LocalRepoManager localRepoManager = LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRoot);) {
//			final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManagerLs.getUserRepoInvitationManager(getUserRegistry(), localRepoManager);
//
//			final PermissionType permissionType = PermissionType.write; // TODO select in UI!
//			final long validityDurationMillis = 5L * 24L * 3600L; // TODO UI!
//
//			final String localPath = "";
//			for (final User invitee : invitees) {
//				final UserRepoInvitationToken userRepoInvitationToken = userRepoInvitationManager.createUserRepoInvitationToken(localPath, invitee, permissionType, validityDurationMillis);
//				final byte[] data = userRepoInvitationToken.getSignedEncryptedUserRepoInvitationData();
//				final String fileName = getFileName(invitee);
//				final File file = createFile(directory, fileName);
//				try {
//					try (final OutputStream out = file.createOutputStream();) {
//						out.write(data);
//					}
//				} catch (IOException e) {
//					throw new RuntimeException(e);
//				}
//			}
//		}
//
//		// We *must* sync immediately to make sure that our invitation-user-repo-keys are known to the server!
//		// Otherwise the invited users cannot connect the repositories (their signature wouldn't be known).
//		final RepoSyncDaemon repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
//		repoSyncDaemon.startSync(localRoot);
//	}
//
//	private static String getFileName(final User invitee) {
//		assertNotNull("invitee", invitee);
//		final StringBuilder sb = new StringBuilder();
//
//		final String firstName = invitee.getFirstName();
//		if (! isEmpty(firstName))
//			sb.append(firstName);
//
//		if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_')
//			sb.append('_');
//
//		final String lastName = invitee.getLastName();
//		if (! isEmpty(lastName))
//			sb.append(lastName);
//
//		if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_')
//			sb.append('_');
//
//		if (! invitee.getEmails().isEmpty())
//			sb.append(invitee.getEmails().get(0));
//
//		if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_')
//			sb.append('_');
//
//		sb.append(invitee.getUserId());
//		sb.append('_');
//		sb.append(new Uid());
//
//		sb.append(SUBSHARE_FILE_EXTENSION);
//		return sb.toString();
//	}
//
//	private UserRegistry getUserRegistry() {
//		return UserRegistryLs.getUserRegistry();
//	}
//
//	private List<User> selectInvitees() {
//		final List<User> users = new ArrayList<>(getUserRegistry().getUsers());
//		// TODO we should filter the repository's owner out! It makes no sense to invite yourself.
//
//		SelectUserDialog dialog = new SelectUserDialog(getScene().getWindow(), users, null, SelectionMode.MULTIPLE,
//				"Please select one or more users you want to invite.\n\nA separate invitation token is created for each of them.");
//		dialog.showAndWait();
//		final List<User> selectedUsers = dialog.getSelectedUsers();
//		return selectedUsers;
//	}
//
//	private File selectDirectory() {
//		// TODO implement our own directory-selection-dialog which allows for showing some more information to the user.
//		final DirectoryChooser directoryChooser = new DirectoryChooser();
//		directoryChooser.setTitle("Where should we put the invitation tokens?");
//		final java.io.File directory = directoryChooser.showDialog(getScene().getWindow());
//		return directory == null ? null : createFile(directory).getAbsoluteFile();
//	}
//	// TODO END replace this code which is (mostly) redundant.
}
