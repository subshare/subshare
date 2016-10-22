package org.subshare.gui.pgp.imp.fromserver;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class SearchCriteriaWizardPage extends WizardPage {

	private final ImportPgpKeyFromServerData importPgpKeyFromServerData;

	public SearchCriteriaWizardPage(final ImportPgpKeyFromServerData importPgpKeyFromServerData) {
		super("Search");
		this.importPgpKeyFromServerData = assertNotNull("importPgpKeyFromServerData", importPgpKeyFromServerData);
	}

	@Override
	protected Parent createContent() {
		return new SearchCriteriaPane(importPgpKeyFromServerData);
	}
}
