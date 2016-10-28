package org.subshare.gui.invitation.issue.selectkey;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.gui.invitation.issue.IssueInvitationData;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class SelectKeyWizardPage extends WizardPage {
	private final IssueInvitationData issueInvitationData;

	public SelectKeyWizardPage(final IssueInvitationData issueInvitationData) {
		super("Select PGP keys");
		this.issueInvitationData = assertNotNull("issueInvitationData", issueInvitationData);
	}

	@Override
	protected Parent createContent() {
		// TODO Auto-generated method stub
		return null;
	}

}
