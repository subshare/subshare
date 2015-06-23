package org.subshare.gui.localrepolist;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.subshare.gui.util.FxmlUtil.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import org.bouncycastle.util.io.Streams;
import org.subshare.core.user.UserRepoInvitationToken;
import org.subshare.gui.ls.RepoSyncDaemonLs;
import org.subshare.gui.ls.ServerRepoManagerLs;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;

public class LocalRepoListPane extends BorderPane {

	public LocalRepoListPane() {
		loadDynamicComponentFxml(LocalRepoListPane.class, this);
	}

	@FXML
	private void acceptInvitationButtonClicked(final ActionEvent event) {
		final File invitationTokenFile = showOpenFileDialog("Choose the invitation token file to be imported.");
		if (invitationTokenFile == null)
			return;

		final UserRepoInvitationToken userRepoInvitationToken;
		try {
			userRepoInvitationToken = readUserRepoInvitationToken(invitationTokenFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		// TODO we need to verify the userRepoInvitationToken, before we continue!
		// 1) Can we decrypt it?
		// 2) Is it signed by someone who we know? We *must* have the signing PGP key in our key-ring - otherwise this fails!
		//    Maybe automatically download it? Or maybe include it in the token? Hmmm... not sure yet.
		// 3) Do we trust the signing PGP key? What, if not? Shall we show a warning?

		final File directory = selectLocalDirectory("Choose the local directory to check-out into (download).");
		if (directory == null)
			return;

		// TODO do this in the background!
		ServerRepoManagerLs.getServerRepoManager().checkOutRepository(directory, userRepoInvitationToken);

		// ...immediately sync after creation. This happens in the background (this method is non-blocking).
		final RepoSyncDaemon repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
		repoSyncDaemon.startSync(directory);
	}

	private UserRepoInvitationToken readUserRepoInvitationToken(File file) throws IOException {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream((int) file.length());
		try (InputStream in = file.createInputStream();) {
			Streams.pipeAll(in, bout);
		}
		final UserRepoInvitationToken result = new UserRepoInvitationToken(bout.toByteArray());
		return result;
	}

	private File showOpenFileDialog(final String title) {
		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		final java.io.File file = fileChooser.showOpenDialog(getScene().getWindow());
		return file == null ? null : createFile(file).getAbsoluteFile();
	}

	private File selectLocalDirectory(final String title) {
		// TODO implement our own directory-selection-dialog which allows for showing some more information to the user.
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(title);
		final java.io.File directory = directoryChooser.showDialog(getScene().getWindow());
		return directory == null ? null : createFile(directory).getAbsoluteFile();
	}
}
