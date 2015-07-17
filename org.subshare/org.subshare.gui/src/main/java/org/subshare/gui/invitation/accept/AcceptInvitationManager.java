package org.subshare.gui.invitation.accept;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.util.io.Streams;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.gui.ls.RepoSyncDaemonLs;
import org.subshare.gui.ls.ServerRepoManagerLs;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;

public class AcceptInvitationManager {

	public void acceptInvitation(final AcceptInvitationData acceptInvitationData) {
		assertNotNull("acceptInvitationData", acceptInvitationData);

		final File invitationFile = assertNotNull("acceptInvitationData.invitationFile", acceptInvitationData.getInvitationFile());
		final UserRepoInvitationToken userRepoInvitationToken = readUserRepoInvitationToken(invitationFile);

		// TODO we need to verify the userRepoInvitationToken, before we continue!
		// 1) Can we decrypt it?
		// 2) Is it signed by someone who we know? We *must* have the signing PGP key in our key-ring - otherwise this fails!
		//    Maybe automatically download it? Or maybe include it in the token? Hmmm... not sure yet.
		// 3) Do we trust the signing PGP key? What, if not? Shall we show a warning?

		final File directory = assertNotNull("acceptInvitationData.checkOutDirectory", acceptInvitationData.getCheckOutDirectory());

		ServerRepoManagerLs.getServerRepoManager().checkOutRepository(directory, userRepoInvitationToken);

		// ...immediately sync after creation. This happens in the background (this method is non-blocking).
		final RepoSyncDaemon repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
		repoSyncDaemon.startSync(directory);
	}

	private UserRepoInvitationToken readUserRepoInvitationToken(File file) {
		try {
			final ByteArrayOutputStream bout = new ByteArrayOutputStream((int) file.length());
			try (InputStream in = file.createInputStream();) {
				Streams.pipeAll(in, bout);
			}
			final UserRepoInvitationToken result = new UserRepoInvitationToken(bout.toByteArray());
			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
