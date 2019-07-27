package org.subshare.gui.invitation.issue.destination;

import static java.util.Objects.*;

import org.subshare.gui.invitation.issue.IssueInvitationData;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class IssueInvitationDestWizardPage extends WizardPage {

	private final IssueInvitationData issueInvitationData;
	private IssueInvitationDestPane issueInvitationDestPane;

	public IssueInvitationDestWizardPage(final IssueInvitationData issueInvitationData) {
		super("Destination directory");
		this.issueInvitationData = requireNonNull(issueInvitationData, "issueInvitationData");
		shownRequired.set(true);
	}

	@Override
	protected Parent createContent() {
		issueInvitationDestPane = new IssueInvitationDestPane(issueInvitationData);
		return issueInvitationDestPane;
	}
}
