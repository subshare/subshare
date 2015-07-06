package org.subshare.gui.welcome.createpgpkey.identity;

import static co.codewizards.cloudstore.core.util.StringUtil.*;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Parent;

import org.subshare.gui.welcome.WelcomeWizardPage;
import org.subshare.gui.welcome.importbackup.ImportBackupWizardPage;
import org.subshare.gui.wizard.Wizard;
import org.subshare.gui.wizard.WizardPage;

public class IdentityWizardPage extends WelcomeWizardPage {
	private final InvalidationListener _updateCompleteInvalidationListener = observable -> updateComplete();
	private WeakInvalidationListener updateCompleteInvalidationListener = new WeakInvalidationListener(_updateCompleteInvalidationListener);

	public IdentityWizardPage() {
		super("Identity");
	}

	@Override
	protected void onAdded(Wizard wizard) {
		super.onAdded(wizard);
		getWelcomeData().getPgpUserId().nameProperty().addListener(updateCompleteInvalidationListener);
		getWelcomeData().importBackupProperty().addListener(updateCompleteInvalidationListener);
		updateComplete();
	}

	@Override
	protected void onRemoved(Wizard wizard) {
		getWelcomeData().getPgpUserId().nameProperty().removeListener(updateCompleteInvalidationListener);
		getWelcomeData().importBackupProperty().removeListener(updateCompleteInvalidationListener);
		super.onRemoved(wizard);
	}

	@Override
	protected Parent getContent() {
		return new IdentityPane(getWelcomeData());
	}

	private void updateComplete() {
		final String name = getWelcomeData().getPgpUserId().nameProperty().get();
		final boolean importBackup = getWelcomeData().importBackupProperty().get();
		IdentityWizardPage.this.completeProperty().set(importBackup || ! isEmpty(name));
	}

	@Override
	public WizardPage getNextPage() {
		if (getWelcomeData().importBackupProperty().get())
			return getWizard().getPageOrFail(ImportBackupWizardPage.class);

		return getWizard().getPageOrFail(ImportBackupWizardPage.class).getNextPage();
	}
}
