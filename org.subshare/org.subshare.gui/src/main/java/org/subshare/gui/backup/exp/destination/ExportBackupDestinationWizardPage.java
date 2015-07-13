package org.subshare.gui.backup.exp.destination;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.gui.backup.exp.ExportBackupData;
import org.subshare.gui.wizard.WizardPage;

public class ExportBackupDestinationWizardPage extends WizardPage {

	private final ExportBackupData exportBackupData;
	private ExportBackupDestinationPane exportBackupDestinationPane;
	private boolean shownAtLeastOnce;

	public ExportBackupDestinationWizardPage(ExportBackupData exportBackupData) {
		super("Export backup");
		this.exportBackupData = assertNotNull("exportBackupData", exportBackupData);
		setMinSize(550, 550);
//		setPrefSize(600, 600);
	}

	@Override
	protected Parent createContent() {
		exportBackupDestinationPane = new ExportBackupDestinationPane(exportBackupData) {
			@Override
			protected void updateComplete() {
				ExportBackupDestinationWizardPage.this.completeProperty().set(shownAtLeastOnce && isComplete());
			}
		};
		return exportBackupDestinationPane;
	}

	@Override
	protected void onShown() {
		super.onShown();
		shownAtLeastOnce = true;
		exportBackupDestinationPane.updateComplete();
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (exportBackupDestinationPane != null)
			exportBackupDestinationPane.requestFocus();
	}
}
