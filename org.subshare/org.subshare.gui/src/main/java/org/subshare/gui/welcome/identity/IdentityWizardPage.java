package org.subshare.gui.welcome.identity;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Parent;

import org.subshare.gui.welcome.IdentityData;
import org.subshare.gui.welcome.importbackup.ImportBackupWizardPage;
import org.subshare.gui.wizard.Wizard;
import org.subshare.gui.wizard.WizardPage;

public class IdentityWizardPage extends WizardPage {
	private final IdentityData identityData;

	private IdentityPane identityPane;

	private final InvalidationListener _updateCompleteInvalidationListener = observable -> updateComplete();
	private WeakInvalidationListener updateCompleteInvalidationListener = new WeakInvalidationListener(_updateCompleteInvalidationListener);

	public IdentityWizardPage(final IdentityData identityData) {
		super("Identity");
		this.identityData = assertNotNull("identityData", identityData);
	}

	@Override
	protected void onAdded(Wizard wizard) {
		super.onAdded(wizard);
		identityData.getPgpUserId().nameProperty().addListener(updateCompleteInvalidationListener);
		identityData.importBackupProperty().addListener(updateCompleteInvalidationListener);
		updateComplete();
	}

	@Override
	protected void onRemoved(Wizard wizard) {
		identityData.getPgpUserId().nameProperty().removeListener(updateCompleteInvalidationListener);
		identityData.importBackupProperty().removeListener(updateCompleteInvalidationListener);
		super.onRemoved(wizard);
	}

	@Override
	protected Parent createContent() {
		identityPane = new IdentityPane(identityData);
		return identityPane;
	}

	private void updateComplete() {
		final String name = identityData.getPgpUserId().nameProperty().get();
		final boolean importBackup = identityData.importBackupProperty().get();
		completeProperty().set(importBackup || ! isEmpty(name));
	}

	@Override
	public WizardPage getNextPage() {
		if (identityData.importBackupProperty().get())
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
