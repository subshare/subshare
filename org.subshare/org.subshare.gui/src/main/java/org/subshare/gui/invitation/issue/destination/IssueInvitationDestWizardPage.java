package org.subshare.gui.invitation.issue.destination;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.gui.invitation.issue.IssueInvitationData;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class IssueInvitationDestWizardPage extends WizardPage {

	private final IssueInvitationData issueInvitationData;
	private IssueInvitationDestPane issueInvitationDestPane;

	public IssueInvitationDestWizardPage(final IssueInvitationData issueInvitationData) {
		super("Destination directory");
		this.issueInvitationData = assertNotNull(issueInvitationData, "issueInvitationData");
		shownRequired.set(true);
	}

	@Override
	protected Parent createContent() {
		issueInvitationDestPane = new IssueInvitationDestPane(issueInvitationData);
		return issueInvitationDestPane;
	}
}
