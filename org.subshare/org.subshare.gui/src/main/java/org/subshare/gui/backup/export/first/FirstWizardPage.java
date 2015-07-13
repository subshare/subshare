package org.subshare.gui.backup.export.first;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.gui.backup.export.ExportBackupData;
import org.subshare.gui.backup.export.destination.ExportBackupDestinationWizardPage;
import org.subshare.gui.wizard.WizardPage;

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
