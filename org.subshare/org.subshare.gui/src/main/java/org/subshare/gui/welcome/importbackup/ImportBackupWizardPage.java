package org.subshare.gui.welcome.importbackup;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.gui.welcome.IdentityData;
import org.subshare.gui.wizard.WizardPage;

public class ImportBackupWizardPage extends WizardPage {

	private final IdentityData identityData;

	public ImportBackupWizardPage(final IdentityData identityData) {
		super("Import OpenPGP key from backup");
		this.identityData = assertNotNull("identityData", identityData);
		completeProperty().set(false);
	}

	@Override
	protected Parent createContent() {
		// TODO Auto-generated method stub
		return null;
	}

}
