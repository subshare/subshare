package org.subshare.gui.createrepo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.List;

import org.subshare.core.server.Server;
import org.subshare.core.user.User;
import org.subshare.gui.createrepo.selectlocaldir.CreateRepoSelectLocalDirWizardPage;
import org.subshare.gui.createrepo.selectserver.CreateRepoSelectServerWizardPage;
import org.subshare.gui.ls.RepoSyncDaemonLs;
import org.subshare.gui.ls.ServerRegistryLs;
import org.subshare.gui.ls.ServerRepoManagerLs;
import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.repo.sync.RepoSyncDaemon;

public class CreateRepoWizard extends Wizard {

	private final CreateRepoData createRepoData;

	public CreateRepoWizard(final CreateRepoData createRepoData) {
		this.createRepoData = assertNotNull("createRepoData", createRepoData);

		if (createRepoData.getServer() == null) {
			List<Server> servers = ServerRegistryLs.getServerRegistry().getServers();
			if (servers.size() == 1)
				createRepoData.setServer(servers.get(0));
			else
				setFirstPage(new CreateRepoSelectServerWizardPage(createRepoData));
		}

		if (createRepoData.getServer() != null)
			setFirstPage(new CreateRepoSelectLocalDirWizardPage(createRepoData));
	}

	@Override
	public void init() {
		super.init();
		setPrefSize(500, 500);
	}

	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
		final File directory = createRepoData.getLocalDirectory();
		final Server server = createRepoData.getServer();
		final User owner = createRepoData.getOwners().iterator().next();

		// This is a long-running blocking operation!
		ServerRepoManagerLs.getServerRepoManager().createRepository(directory, server, owner);

		// ...immediately sync after creation. This happens in the background (this method is non-blocking).
		final RepoSyncDaemon repoSyncDaemon = RepoSyncDaemonLs.getRepoSyncDaemon();
		repoSyncDaemon.startSync(directory);
	}

	@Override
	public String getTitle() {
		return "Create repository / upload and share directory";
	}
}
