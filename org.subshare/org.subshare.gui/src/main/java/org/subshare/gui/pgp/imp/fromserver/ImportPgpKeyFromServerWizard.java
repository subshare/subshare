package org.subshare.gui.pgp.imp.fromserver;

import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public class ImportPgpKeyFromServerWizard extends Wizard {

	private final ImportPgpKeyFromServerData importPgpKeyFromServerData = new ImportPgpKeyFromServerData();

	public ImportPgpKeyFromServerWizard() {
		setFirstPage(new SearchCriteriaWizardPage(importPgpKeyFromServerData));
	}

	public ImportPgpKeyFromServerData getImportPgpKeyFromServerData() {
		return importPgpKeyFromServerData;
	}

	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public String getTitle() {
		return "Import PGP keys from the server";
	}

}
