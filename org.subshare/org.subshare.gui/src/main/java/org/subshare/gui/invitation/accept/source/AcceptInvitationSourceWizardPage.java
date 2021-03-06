package org.subshare.gui.invitation.accept.source;

import static java.util.Objects.*;

import org.subshare.gui.invitation.accept.AcceptInvitationData;
import org.subshare.gui.invitation.accept.checkoutdir.CheckOutDirectoryWizardPage;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class AcceptInvitationSourceWizardPage extends WizardPage {

	private final AcceptInvitationData acceptInvitationData;
	private AcceptInvitationSourcePane acceptInvitationSourcePane;

	public AcceptInvitationSourceWizardPage(final AcceptInvitationData acceptInvitationData) {
		super("Which invitation?");
		this.acceptInvitationData = requireNonNull(acceptInvitationData, "acceptInvitationData");
	}

	@Override
	protected void init() {
		super.init();
		setNextPage(new CheckOutDirectoryWizardPage(acceptInvitationData));
	}

	@Override
	protected Parent createContent() {
		acceptInvitationSourcePane = new AcceptInvitationSourcePane(acceptInvitationData);
		return acceptInvitationSourcePane;
	}
}
