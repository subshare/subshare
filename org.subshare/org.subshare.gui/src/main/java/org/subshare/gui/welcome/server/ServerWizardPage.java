package org.subshare.gui.welcome.server;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Parent;

import org.subshare.gui.invitation.accept.source.AcceptInvitationSourceWizardPage;
import org.subshare.gui.welcome.ServerData;
import org.subshare.gui.wizard.WizardPage;

public class ServerWizardPage extends WizardPage {

	private ServerData serverData; // must not be final - otherwise getting compilation error with javac, while Eclipse works fine :-(
	private ServerPane serverPane;
	private AcceptInvitationSourceWizardPage acceptInvitationSourceWizardPage; // must not be final - otherwise getting compilation error with javac, while Eclipse works fine :-(
	private final InvalidationListener acceptInvitationInvalidationListener = observable -> {
		if (serverData.acceptInvitationProperty().get())
			nextPageProperty().set(acceptInvitationSourceWizardPage);
		else
			nextPageProperty().set(null);
	};

	public ServerWizardPage(ServerData serverData) {
		super("Server");
		this.serverData = assertNotNull("serverData", serverData);
		acceptInvitationSourceWizardPage = new AcceptInvitationSourceWizardPage(serverData.getAcceptInvitationData());
		serverData.acceptInvitationProperty().addListener(new WeakInvalidationListener(acceptInvitationInvalidationListener));
	}

	@Override
	protected Parent createContent() {
		serverPane = new ServerPane(serverData) {
			@Override
			protected void updateComplete() {
				ServerWizardPage.this.completeProperty().set(isComplete());
			}
		};
		return serverPane;
	}
}
