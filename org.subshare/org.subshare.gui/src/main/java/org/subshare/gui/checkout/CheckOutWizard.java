package org.subshare.gui.checkout;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static java.util.Objects.*;

import org.subshare.core.repo.ServerRepo;
import org.subshare.core.repo.metaonly.ServerRepoFile;
import org.subshare.core.server.Server;
import org.subshare.gui.ls.RepoSyncDaemonLs;
import org.subshare.gui.ls.ServerRepoManagerLs;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

public class CheckOutWizard { // TODO sub-class Wizard!!!

	private final Server server;
	private final ServerRepo serverRepo;
	private final ServerRepoFile serverRepoFile;

	public CheckOutWizard(final Server server, final ServerRepo serverRepo) {
		this.server = requireNonNull(server, "server");
		this.serverRepo = requireNonNull(serverRepo, "serverRepo");
		this.serverRepoFile = null;
	}

	public CheckOutWizard(final ServerRepoFile serverRepoFile) {
		this.server = serverRepoFile.getServer();
		this.serverRepo = serverRepoFile.getServerRepo();
		this.serverRepoFile = serverRepoFile;
	}

	public void checkOut(final Window owner) {
		// TODO Auto-generated method stub

		final File directory = selectLocalDirectory(owner, "Select local directory for check-out (download).");
		if (directory == null)
			return;

		// TODO do this in the background! Maybe simply use a Wizard?! Its finish(...) is done on a worker-thread...
		String serverPath = serverRepoFile == null ? "" : serverRepoFile.getServerPath();
		ServerRepoManagerLs.getServerRepoManager().checkOutRepository(server, serverRepo, serverPath, directory);

		// ...immediately sync after check-out. This happens in the background (this method is non-blocking).
		final RepoSyncDaemon repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
		repoSyncDaemon.startSync(directory);
	}

	private File selectLocalDirectory(final Window owner, final String title) {
		// TODO implement our own directory-selection-dialog which allows for showing some more information to the user.
		final DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle(title);
		final java.io.File directory = directoryChooser.showDialog(owner);
		return directory == null ? null : createFile(directory).getAbsoluteFile();
	}

}
