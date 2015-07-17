package org.subshare.gui.welcome.identity;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.Parent;

import org.subshare.gui.backup.imp.source.ImportBackupSourceWizardPage;
import org.subshare.gui.pgp.createkey.passphrase.PassphraseWizardPage;
import org.subshare.gui.welcome.IdentityData;
import org.subshare.gui.wizard.WizardPage;

public class IdentityWizardPage extends WizardPage {
	private final IdentityData identityData;

	private IdentityPane identityPane;

	private final InvalidationListener _updateCompleteInvalidationListener = observable -> updateComplete();
	private WeakInvalidationListener updateCompleteInvalidationListener = new WeakInvalidationListener(_updateCompleteInvalidationListener);

	private final ImportBackupSourceWizardPage importBackupSourceWizardPage;
	private final PassphraseWizardPage passphraseWizardPage;

	public IdentityWizardPage(final IdentityData identityData) {
		super("Identity");
		this.identityData = assertNotNull("identityData", identityData);

		importBackupSourceWizardPage = new ImportBackupSourceWizardPage(identityData.getImportBackupData());
		passphraseWizardPage = new PassphraseWizardPage(identityData.getCreatePgpKeyParam());

		identityData.importBackupProperty().addListener((InvalidationListener) observable -> updateNextPage());
		updateNextPage();
	}

	protected void updateNextPage() {
		if (identityData.importBackupProperty().get())
			setNextPage(importBackupSourceWizardPage);
		else
			setNextPage(passphraseWizardPage);
	}

	@Override
	protected void init() {
		super.init();
		identityData.getPgpUserId().nameProperty().addListener(updateCompleteInvalidationListener);
		identityData.importBackupProperty().addListener(updateCompleteInvalidationListener);
		updateComplete();

		importBackupSourceWizardPage.setWizard(getWizard());
		passphraseWizardPage.setWizard(getWizard());
	}

	@Override
	protected Parent createContent() {
		identityPane = new IdentityPane(identityData);
		return identityPane;
	}

	private void updateComplete() {
		final String name = identityData.getPgpUserId().nameProperty().get();
		final boolean importBackup = identityData.importBackupProperty().get();
		setComplete(importBackup || ! isEmpty(name));
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (identityPane != null)
			identityPane.firstNameTextField.requestFocus();
	}
}
