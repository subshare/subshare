package org.subshare.gui.welcome;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import org.subshare.core.server.Server;
import org.subshare.core.server.ServerRegistry;
import org.subshare.core.sync.Sync;
import org.subshare.gui.invitation.accept.AcceptInvitationManager;
import org.subshare.gui.ls.LockerSyncLs;
import org.subshare.gui.ls.PgpSyncLs;
import org.subshare.gui.ls.ServerRegistryLs;
import org.subshare.gui.welcome.first.FirstWizardPage;
import org.subshare.gui.welcome.server.ServerWizardPage;
import org.subshare.gui.wizard.DefaultFinishingPage;
import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public class ServerWizard extends Wizard {

	private final ServerData serverData = new ServerData();
	private final boolean syncLocker;

	private final ServerRegistry serverRegistry;

	private boolean needed;

	public ServerWizard(boolean showFirstPage, boolean syncLocker) {
		this.syncLocker = syncLocker;

		if (showFirstPage)
			setFirstPage(new FirstWizardPage());

		serverRegistry = ServerRegistryLs.getServerRegistry();
		if (serverRegistry.getServers().isEmpty()) {
			needed = true;
			serverData.setServer(serverRegistry.createServer());
			if (getFirstPage() == null)
				setFirstPage(new ServerWizardPage(serverData));
			else
				getFirstPage().setNextPage(new ServerWizardPage(serverData));
		}
	}

	@Override
	public void init() {
		super.init();
		setPrefSize(500, 500);
//		getScene().getWindow().setOnShown(event -> {
//			getScene().getWindow().setWidth(500);
//			getScene().getWindow().setHeight(500);
//		});
	}

	public ServerData getServerData() {
		return serverData;
	}

	@Override
	public String getTitle() {
		return Messages.getString("ServerWizard.title"); //$NON-NLS-1$
	}

	public boolean isNeeded() {
		return needed;
	}

	@Override
	protected void finishing() {
		if (serverData.acceptInvitationProperty().get()) {
			((DefaultFinishingPage) getFinishingPage()).getHeaderText().setText(
					Messages.getString("ServerWizard.finishingPage.headerText.text[acceptInvitation]")); //$NON-NLS-1$
		}
		else {
			if (isEmpty(trim(serverData.getServer().getName())))
				serverData.getServer().setName(serverData.getServer().getUrl().getHost());

			((DefaultFinishingPage) getFinishingPage()).getHeaderText().setText(
					Messages.getString("ServerWizard.finishingPage.headerText.text[registerServer]")); //$NON-NLS-1$
		}
		super.finishing();
	}

	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
		if (serverData.acceptInvitationProperty().get())
			finish_acceptInvitation(monitor);
		else
			finish_registerServer(monitor);
	}

	protected void finish_acceptInvitation(ProgressMonitor monitor) throws Exception {
		new AcceptInvitationManager().acceptInvitation(serverData.getAcceptInvitationData());
	}

	protected void finish_registerServer(ProgressMonitor monitor) throws Exception {
		final Server server = assertNotNull("serverData.server", serverData.getServer());
		if (syncLocker) {
			// We must first sync the PGP keys, because the server doesn't accept a locker that's signed
			// by an unknown key.
			try (Sync pgpSync = PgpSyncLs.createPgpSync(server);) {
				pgpSync.sync();
			}

			// We must not (yet) add the server to the ServerRegistry, because we maybe already
			// used this server before, in this case, a Server entry will be down-synced (=> Locker),
			// anyway.
			try (Sync lockerSync = LockerSyncLs.createLockerSync(server);) {
				lockerSync.sync();
			}

			// And now we register it, if the server's URL hasn't become known by the lockerSync's down-sync, before.
			Server server2 = serverRegistry.getServerForRemoteRoot(server.getUrl());
			if (server2 == null)
				serverRegistry.getServers().add(server);
		}
		else
			serverRegistry.getServers().add(server);
	}
}
