package org.subshare.gui.invitation.accept;

import org.subshare.gui.invitation.accept.source.AcceptInvitationSourceWizardPage;
import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public class AcceptInvitationWizard extends Wizard {
	private final AcceptInvitationData acceptInvitationData = new AcceptInvitationData();

	public AcceptInvitationWizard() {
		setFirstPage(new AcceptInvitationSourceWizardPage(acceptInvitationData));
	}

	@Override
	public void init() {
		super.init();
		setPrefSize(500, 500);
	}

	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
		new AcceptInvitationManager().acceptInvitation(acceptInvitationData);
	}

	@Override
	public String getTitle() {
		return "Accept invitation";
	}
}
