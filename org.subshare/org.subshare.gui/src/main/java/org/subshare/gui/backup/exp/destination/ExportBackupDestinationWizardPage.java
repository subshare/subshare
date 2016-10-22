package org.subshare.gui.backup.exp.destination;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.gui.backup.exp.ExportBackupData;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class ExportBackupDestinationWizardPage extends WizardPage {

	private final ExportBackupData exportBackupData;
	private ExportBackupDestinationPane exportBackupDestinationPane;

	public ExportBackupDestinationWizardPage(ExportBackupData exportBackupData) {
		super("Export backup");
		this.exportBackupData = assertNotNull("exportBackupData", exportBackupData);
		shownRequired.set(true);
		setMinSize(550, 550);
//		setPrefSize(600, 600);
	}

	@Override
	protected Parent createContent() {
		exportBackupDestinationPane = new ExportBackupDestinationPane(exportBackupData);
		return exportBackupDestinationPane;
	}
}
