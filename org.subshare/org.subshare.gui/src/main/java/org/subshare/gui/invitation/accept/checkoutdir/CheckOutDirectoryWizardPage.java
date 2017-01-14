package org.subshare.gui.invitation.accept.checkoutdir;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.gui.invitation.accept.AcceptInvitationData;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class CheckOutDirectoryWizardPage extends WizardPage {

	private final AcceptInvitationData acceptInvitationData;
	private CheckOutDirectoryPane checkOutDirectoryPane;

	public CheckOutDirectoryWizardPage(final AcceptInvitationData acceptInvitationData) {
		super("Check-out directory");
		this.acceptInvitationData = assertNotNull(acceptInvitationData, "acceptInvitationData");
		shownRequired.set(true);
	}

	@Override
	protected Parent createContent() {
		checkOutDirectoryPane = new CheckOutDirectoryPane(acceptInvitationData);
		return checkOutDirectoryPane;
	}
}
