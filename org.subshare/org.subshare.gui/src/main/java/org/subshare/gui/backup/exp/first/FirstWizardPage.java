package org.subshare.gui.backup.exp.first;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.gui.backup.exp.ExportBackupData;
import org.subshare.gui.backup.exp.destination.ExportBackupDestinationWizardPage;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class FirstWizardPage extends WizardPage {

	public FirstWizardPage(final ExportBackupData exportBackupData) {
		super("Backup needed!");
		assertNotNull("exportBackupData", exportBackupData);
		setNextPage(new ExportBackupDestinationWizardPage(exportBackupData));
	}

	@Override
	protected Parent createContent() {
		return new FirstPane();
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		nextButton.requestFocus();
	}
}
