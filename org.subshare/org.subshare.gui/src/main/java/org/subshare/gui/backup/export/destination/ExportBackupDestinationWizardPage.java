package org.subshare.gui.backup.export.destination;

import javafx.scene.Parent;

import org.subshare.gui.wizard.WizardPage;

public class ExportBackupDestinationWizardPage extends WizardPage {

	private ExportBackupDestinationPane exportBackupDestinationPane;
	private boolean shownAtLeastOnce;

	public ExportBackupDestinationWizardPage() {
		super("Export backup");
		setMinSize(550, 550);
//		setPrefSize(600, 600);
	}

	@Override
	protected Parent createContent() {
		exportBackupDestinationPane = new ExportBackupDestinationPane() {
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
}
