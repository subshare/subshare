package org.subshare.gui.invitation.accept.source;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.gui.invitation.accept.AcceptInvitationData;
import org.subshare.gui.wizard.WizardPage;

public class AcceptInvitationSourceWizardPage extends WizardPage {

	private final AcceptInvitationData acceptInvitationData;
	private AcceptInvitationSourcePane acceptInvitationSourcePane;

	public AcceptInvitationSourceWizardPage(final AcceptInvitationData acceptInvitationData) {
		super("Which invitation?");
		this.acceptInvitationData = assertNotNull("acceptInvitationData", acceptInvitationData);
	}

	@Override
	protected Parent createContent() {
		acceptInvitationSourcePane = new AcceptInvitationSourcePane(acceptInvitationData);

		return acceptInvitationSourcePane;
	}

}
