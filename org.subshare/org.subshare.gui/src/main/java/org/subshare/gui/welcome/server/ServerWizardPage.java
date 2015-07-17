package org.subshare.gui.welcome.server;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Parent;

import org.subshare.gui.invitation.accept.source.AcceptInvitationSourceWizardPage;
import org.subshare.gui.welcome.ServerData;
import org.subshare.gui.wizard.WizardPage;

public class ServerWizardPage extends WizardPage {

	private final ServerData serverData;
	private ServerPane serverPane;
	private final AcceptInvitationSourceWizardPage acceptInvitationSourceWizardPage;
	private final InvalidationListener acceptInvitationInvalidationListener;

	public ServerWizardPage(final ServerData serverData) {
		super("Server");
		this.serverData = assertNotNull("serverData", serverData);
		acceptInvitationSourceWizardPage = new AcceptInvitationSourceWizardPage(serverData.getAcceptInvitationData());
		acceptInvitationInvalidationListener = observable -> {
			nextPageProperty().set(serverData.acceptInvitationProperty().get() ? acceptInvitationSourceWizardPage : null);
		};
		serverData.acceptInvitationProperty().addListener(new WeakInvalidationListener(acceptInvitationInvalidationListener));
	}

	@Override
	protected Parent createContent() {
		serverPane = new ServerPane(serverData) {
			@Override
			protected void updateComplete() {
				ServerWizardPage.this.setComplete(isComplete());
			}
		};
		return serverPane;
	}
}
