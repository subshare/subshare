package org.subshare.gui.backup.imp.source;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.gui.backup.imp.ImportBackupData;
import org.subshare.gui.wizard.WizardPage;

public class ImportBackupSourceWizardPage extends WizardPage {

	private final ImportBackupData importBackupData;
	private ImportBackupSourcePane importBackupSourcePane;

	public ImportBackupSourceWizardPage(final ImportBackupData importBackupData) {
		super("Import data from backup");
		this.importBackupData = assertNotNull("importBackupData", importBackupData);
	}

	@Override
	protected Parent createContent() {
		importBackupSourcePane = new ImportBackupSourcePane(importBackupData) {
			@Override
			protected void updateComplete() {
				ImportBackupSourceWizardPage.this.completeProperty().set(isComplete());
			}
		};
		return importBackupSourcePane;
	}
}
