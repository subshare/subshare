package org.subshare.gui.welcome.createpgpkey.identity;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Parent;

import org.subshare.gui.welcome.WelcomeData;
import org.subshare.gui.welcome.importbackup.ImportBackupWizardPage;
import org.subshare.gui.wizard.Wizard;
import org.subshare.gui.wizard.WizardPage;

public class IdentityWizardPage extends WizardPage {
	private final WelcomeData welcomeData;

	private IdentityPane identityPane;

	private final InvalidationListener _updateCompleteInvalidationListener = observable -> updateComplete();
	private WeakInvalidationListener updateCompleteInvalidationListener = new WeakInvalidationListener(_updateCompleteInvalidationListener);

	public IdentityWizardPage(final WelcomeData welcomeData) {
		super("Identity");
		this.welcomeData = assertNotNull("welcomeData", welcomeData);
	}

	@Override
	protected void onAdded(Wizard wizard) {
		super.onAdded(wizard);
		welcomeData.getPgpUserId().nameProperty().addListener(updateCompleteInvalidationListener);
		welcomeData.importBackupProperty().addListener(updateCompleteInvalidationListener);
		updateComplete();
	}

	@Override
	protected void onRemoved(Wizard wizard) {
		welcomeData.getPgpUserId().nameProperty().removeListener(updateCompleteInvalidationListener);
		welcomeData.importBackupProperty().removeListener(updateCompleteInvalidationListener);
		super.onRemoved(wizard);
	}

	@Override
	protected Parent createContent() {
		identityPane = new IdentityPane(welcomeData);
		return identityPane;
	}

	private void updateComplete() {
		final String name = welcomeData.getPgpUserId().nameProperty().get();
		final boolean importBackup = welcomeData.importBackupProperty().get();
		IdentityWizardPage.this.completeProperty().set(importBackup || ! isEmpty(name));
	}

	@Override
	public WizardPage getNextPage() {
		if (welcomeData.importBackupProperty().get())
			return getWizard().getPageOrFail(ImportBackupWizardPage.class);

		return getWizard().getPageOrFail(ImportBackupWizardPage.class).getNextPage();
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (identityPane != null)
			identityPane.firstNameTextField.requestFocus();
	}
}
