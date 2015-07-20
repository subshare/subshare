package org.subshare.gui.invitation.issue.selectuser;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Parent;
import javafx.scene.control.SelectionMode;

import org.subshare.core.user.User;
import org.subshare.gui.invitation.issue.IssueInvitationData;
import org.subshare.gui.invitation.issue.destination.IssueInvitationDestWizardPage;
import org.subshare.gui.ls.UserRegistryLs;
import org.subshare.gui.selectuser.SelectUserPane;
import org.subshare.gui.wizard.WizardPage;

public class SelectUserWizardPage extends WizardPage {

	private final IssueInvitationData issueInvitationData;
	private SelectUserPane selectUserPane;

	public SelectUserWizardPage(final IssueInvitationData issueInvitationData) {
		super(Messages.getString("SelectUserWizardPage.title")); //$NON-NLS-1$
		this.issueInvitationData = assertNotNull("issueInvitationData", issueInvitationData); //$NON-NLS-1$
	}

	@Override
	protected void init() {
		super.init();
		setNextPage(new IssueInvitationDestWizardPage(issueInvitationData));
	}

	@Override
	protected Parent createContent() {
		List<User> users = new ArrayList<>(UserRegistryLs.getUserRegistry().getUsers());
		selectUserPane = new SelectUserPane(
				users, issueInvitationData.getInvitees(), SelectionMode.MULTIPLE,
				Messages.getString("SelectUserWizardPage.selectUserPane.headerText")) { //$NON-NLS-1$
			@Override
			protected void updateDisable() {
				SelectUserWizardPage.this.setComplete(! issueInvitationData.getInvitees().isEmpty());
			}
		};
		return selectUserPane;
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (selectUserPane != null)
			selectUserPane.requestFocus();
	}
}
