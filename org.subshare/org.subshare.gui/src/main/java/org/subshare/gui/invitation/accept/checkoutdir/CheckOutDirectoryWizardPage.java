package org.subshare.gui.invitation.accept.checkoutdir;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.gui.invitation.accept.AcceptInvitationData;
import org.subshare.gui.wizard.WizardPage;

public class CheckOutDirectoryWizardPage extends WizardPage {

	private final AcceptInvitationData acceptInvitationData;
	private CheckOutDirectoryPane checkOutDirectoryPane;
	private boolean shownAtLeastOnce;

	public CheckOutDirectoryWizardPage(final AcceptInvitationData acceptInvitationData) {
		super("Check-out directory");
		this.acceptInvitationData = assertNotNull("acceptInvitationData", acceptInvitationData);
	}

	@Override
	protected Parent createContent() {
		checkOutDirectoryPane = new CheckOutDirectoryPane(acceptInvitationData) {
			@Override
			protected void updateComplete() {
				CheckOutDirectoryWizardPage.this.setComplete(shownAtLeastOnce && isComplete());
			}
		};
		return checkOutDirectoryPane;
	}

	@Override
	protected void onShown() {
		super.onShown();
		shownAtLeastOnce = true;
		checkOutDirectoryPane.updateComplete();
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (checkOutDirectoryPane != null)
			checkOutDirectoryPane.requestFocus();
	}
}
