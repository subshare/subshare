package org.subshare.gui.welcome.server;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.gui.invitation.accept.source.AcceptInvitationSourceWizardPage;
import org.subshare.gui.welcome.ServerData;
import org.subshare.gui.wizard.WizardPage;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Parent;

public class ServerWizardPage extends WizardPage {

	private final ServerData serverData;
	private final AcceptInvitationSourceWizardPage acceptInvitationSourceWizardPage;
	private final InvalidationListener acceptInvitationInvalidationListener;

	public ServerWizardPage(final ServerData serverData) {
		super("Server");
		this.serverData = assertNotNull(serverData, "serverData");
		acceptInvitationSourceWizardPage = new AcceptInvitationSourceWizardPage(serverData.getAcceptInvitationData());
		acceptInvitationInvalidationListener = observable -> {
			nextPageProperty().set(serverData.acceptInvitationProperty().get() ? acceptInvitationSourceWizardPage : null);
		};
		serverData.acceptInvitationProperty().addListener(new WeakInvalidationListener(acceptInvitationInvalidationListener));
	}

	@Override
	protected Parent createContent() {
		return new ServerPane(serverData);
	}
}
