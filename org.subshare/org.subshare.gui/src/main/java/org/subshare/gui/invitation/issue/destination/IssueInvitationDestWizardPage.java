package org.subshare.gui.invitation.issue.destination;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.gui.invitation.issue.IssueInvitationData;
import org.subshare.gui.wizard.WizardPage;

public class IssueInvitationDestWizardPage extends WizardPage {

	private final IssueInvitationData issueInvitationData;
	private IssueInvitationDestPane issueInvitationDestPane;
	private boolean shownAtLeastOnce;

	public IssueInvitationDestWizardPage(final IssueInvitationData issueInvitationData) {
		super("Destination directory");
		this.issueInvitationData = assertNotNull("issueInvitationData", issueInvitationData);
	}

	@Override
	protected Parent createContent() {
		issueInvitationDestPane = new IssueInvitationDestPane(issueInvitationData) {
			@Override
			protected void updateComplete() {
				IssueInvitationDestWizardPage.this.setComplete(shownAtLeastOnce && isComplete());
			}
		};
		return issueInvitationDestPane;
	}

	@Override
	protected void onShown() {
		super.onShown();
		shownAtLeastOnce = true;
		issueInvitationDestPane.updateComplete();
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (issueInvitationDestPane != null)
			issueInvitationDestPane.requestFocus();
	}
}
