package org.subshare.gui.pgp.imp.fromserver;

import static java.util.Objects.*;

import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class SearchCriteriaWizardPage extends WizardPage {

	private final ImportPgpKeyFromServerData importPgpKeyFromServerData;

	public SearchCriteriaWizardPage(final ImportPgpKeyFromServerData importPgpKeyFromServerData) {
		super("Search criteria");
		this.importPgpKeyFromServerData = requireNonNull(importPgpKeyFromServerData, "importPgpKeyFromServerData");
		setNextPage(new SearchResultWizardPage(importPgpKeyFromServerData));
	}

	@Override
	protected Parent createContent() {
		return new SearchCriteriaPane(importPgpKeyFromServerData);
	}
}
