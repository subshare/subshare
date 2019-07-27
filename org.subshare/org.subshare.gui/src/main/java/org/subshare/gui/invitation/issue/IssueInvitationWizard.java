package org.subshare.gui.invitation.issue;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static java.util.Objects.*;
import static org.subshare.core.file.FileConst.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.subshare.core.dto.PermissionType;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.repo.LocalRepo;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRepoInvitationManager;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.gui.invitation.issue.selectuser.SelectUserWizardPage;
import org.subshare.gui.ls.LocalRepoManagerFactoryLs;
import org.subshare.gui.ls.RepoSyncDaemonLs;
import org.subshare.gui.ls.UserRegistryLs;
import org.subshare.gui.ls.UserRepoInvitationManagerLs;
import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class IssueInvitationWizard extends Wizard {

	private final IssueInvitationData issueInvitationData;
	private final List<String> fileNames = new ArrayList<String>();

	public IssueInvitationWizard(final IssueInvitationData issueInvitationData) {
		this.issueInvitationData = requireNonNull(issueInvitationData, "issueInvitationData");
		setFirstPage(new SelectUserWizardPage(issueInvitationData));
	}

	@Override
	public void init() {
		super.init();
		setPrefSize(600, 500);
	}


	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
		final Set<User> invitees = issueInvitationData.getInvitees();
		final File invitationTokenDirectory = issueInvitationData.getInvitationTokenDirectory();
		requireNonNull(invitationTokenDirectory, "issueInvitationData.invitationTokenDirectory");

		final UserRegistry userRegistry = UserRegistryLs.getUserRegistry();
		final File localRoot = issueInvitationData.getLocalRepo().getLocalRoot();
		final PermissionType permissionType = issueInvitationData.getPermissionType();
		final long validityDurationMillis = issueInvitationData.getValidityDurationMillis();
		final String localPath = getLocalPath();

		fileNames.clear();
		try (final LocalRepoManager localRepoManager = LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRoot);) {
			final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManagerLs.getUserRepoInvitationManager(userRegistry, localRepoManager);

			for (final User invitee : invitees) {
				final Set<PgpKey> inviteePgpKeys = null; // TODO allow the user to specify the keys being used!
				final UserRepoInvitationToken userRepoInvitationToken = userRepoInvitationManager.createUserRepoInvitationToken(
						localPath, invitee, inviteePgpKeys, permissionType, validityDurationMillis);
				final byte[] data = userRepoInvitationToken.getSignedEncryptedUserRepoInvitationData();
				final String fileName = getFileName(invitee);
				fileNames.add(fileName);
				final File file = createFile(invitationTokenDirectory, fileName);
				try {
					try (final OutputStream out = castStream(file.createOutputStream())) {
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
		// TODO we should wait for the sync to finish! and it would be cool if we could up-sync the meta-data only (skip huge files ;-)
	}

	@Override
	protected void preFinished() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setHeaderText("The invitation files were successfully created!");

		final VBox contentContainer = new VBox();
		contentContainer.setSpacing(8);

		final HBox directoryBox = new HBox();
		directoryBox.setSpacing(8);
		directoryBox.getChildren().add(new Label("Directory:"));

		final TextField directoryTextField = new TextField();
		directoryTextField.setEditable(false);
		directoryTextField.setText(issueInvitationData.getInvitationTokenDirectory().getPath());
		HBox.setHgrow(directoryTextField, Priority.ALWAYS);
		directoryBox.getChildren().add(directoryTextField);
		contentContainer.getChildren().add(directoryBox);

		contentContainer.getChildren().add(new Label("Generated files:"));
		ListView<String> fileNameListView = new ListView<>();
		fileNameListView.getItems().addAll(fileNames);
		fileNameListView.setPrefSize(400, 200);
		contentContainer.getChildren().add(fileNameListView);

		final Text contentText = new Text("Please give these invitation files to the invited users. You can send them by e-mail, pass them via a thumb drive, upload them to a web-server or use whatever communication channel you prefer.\n\nThe files are encrypted and can only be used by the intended receipients.\n\nImportant: The invitations can only be used *after* the next upward synchronisation of this repository completed!");
		contentText.setWrappingWidth(600);
		contentContainer.getChildren().add(contentText);

		alert.getDialogPane().setContent(contentContainer);

		alert.showAndWait();
		super.preFinished();
	}

	private String getLocalPath() {
		final LocalRepo localRepo = requireNonNull(issueInvitationData.getLocalRepo(), "issueInvitationData.localRepo");
		return localRepo.getLocalPath(requireNonNull(issueInvitationData.getInvitationTargetFile(), "issueInvitationData.invitationTargetFile"));
	}

	private String getFileName(final User invitee) {
		requireNonNull(invitee, "invitee");
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

	@Override
	public String getTitle() {
		return "Invite user(s)";
	}

}
