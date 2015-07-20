package org.subshare.gui.invitation.issue;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static org.subshare.core.file.FileConst.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.subshare.core.dto.PermissionType;
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

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;
import co.codewizards.cloudstore.core.util.IOUtil;

public class IssueInvitationWizard extends Wizard {

	private final IssueInvitationData issueInvitationData;

	public IssueInvitationWizard(final IssueInvitationData issueInvitationData) {
		this.issueInvitationData = assertNotNull("issueInvitationData", issueInvitationData);
		setFirstPage(new SelectUserWizardPage(issueInvitationData));
	}

	@Override
	public void init() {
		super.init();
		setPrefSize(500, 500);
	}

	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
		final Set<User> invitees = issueInvitationData.getInvitees();
		final File invitationTokenDirectory = issueInvitationData.getInvitationTokenDirectory();
		assertNotNull("issueInvitationData.invitationTokenDirectory", invitationTokenDirectory);

		final UserRegistry userRegistry = UserRegistryLs.getUserRegistry();
		final File localRoot = issueInvitationData.getLocalRepo().getLocalRoot();
		final PermissionType permissionType = issueInvitationData.getPermissionType();
		final long validityDurationMillis = issueInvitationData.getValidityDurationMillis();
		final String localPath = getLocalPath();

		try (final LocalRepoManager localRepoManager = LocalRepoManagerFactoryLs.getLocalRepoManagerFactory().createLocalRepoManagerForExistingRepository(localRoot);) {
			final UserRepoInvitationManager userRepoInvitationManager = UserRepoInvitationManagerLs.getUserRepoInvitationManager(userRegistry, localRepoManager);

			for (final User invitee : invitees) {
				final UserRepoInvitationToken userRepoInvitationToken = userRepoInvitationManager.createUserRepoInvitationToken(
						localPath, invitee, permissionType, validityDurationMillis);
				final byte[] data = userRepoInvitationToken.getSignedEncryptedUserRepoInvitationData();
				final String fileName = getFileName(invitee);
				final File file = createFile(invitationTokenDirectory, fileName);
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
		// TODO we should wait for the sync to finish! and it would be cool if we could up-sync the meta-data only (skip huge files ;-)
	}

	private String getLocalPath() {
		final LocalRepo localRepo = assertNotNull("issueInvitationData.localRepo", issueInvitationData.getLocalRepo());
		final File localRoot = assertNotNull("issueInvitationData.localRepo.localRoot", localRepo.getLocalRoot());
		final File invitationTargetFile = assertNotNull("issueInvitationData.invitationTargetFile", issueInvitationData.getInvitationTargetFile());
		if (localRoot.equals(invitationTargetFile))
			return "";

		final String localPath;
		try {
			localPath = IOUtil.getRelativePath(localRoot, invitationTargetFile).replace(java.io.File.separatorChar, '/');
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
		return localPath;
	}

	private String getFileName(final User invitee) {
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

	@Override
	public String getTitle() {
		return "Invite user(s)";
	}

}
