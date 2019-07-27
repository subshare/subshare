package org.subshare.gui.backup.imp.source;

import static java.util.Objects.*;

import org.subshare.gui.backup.imp.ImportBackupData;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class ImportBackupSourceWizardPage extends WizardPage {

	private final ImportBackupData importBackupData;
	private ImportBackupSourcePane importBackupSourcePane;

	public ImportBackupSourceWizardPage(final ImportBackupData importBackupData) {
		super("Import data from backup");
		this.importBackupData = requireNonNull(importBackupData, "importBackupData");
	}

	@Override
	protected Parent createContent() {
		importBackupSourcePane = new ImportBackupSourcePane(importBackupData);
		return importBackupSourcePane;
	}
}
