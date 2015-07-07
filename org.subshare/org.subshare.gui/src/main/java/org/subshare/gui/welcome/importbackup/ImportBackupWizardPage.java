package org.subshare.gui.welcome.importbackup;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.gui.welcome.WelcomeData;
import org.subshare.gui.wizard.WizardPage;

public class ImportBackupWizardPage extends WizardPage {

	private final WelcomeData welcomeData;

	public ImportBackupWizardPage(final WelcomeData welcomeData) {
		super("Import OpenPGP key from backup");
		this.welcomeData = assertNotNull("welcomeData", welcomeData);
		completeProperty().set(false);
	}

	@Override
	protected Parent createContent() {
		// TODO Auto-generated method stub
		return null;
	}

}
