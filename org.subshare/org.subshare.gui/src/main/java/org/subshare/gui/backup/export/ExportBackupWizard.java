package org.subshare.gui.backup.export;

import org.subshare.gui.backup.export.destination.ExportBackupDestinationWizardPage;
import org.subshare.gui.backup.export.first.FirstWizardPage;
import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public class ExportBackupWizard extends Wizard {

	public ExportBackupWizard() {
		super(new FirstWizardPage(),
				new ExportBackupDestinationWizardPage());
	}

	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public String getTitle() {
		return "Backup NOW!";
	}
}
