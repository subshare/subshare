package org.subshare.gui.pgp.imp.fromserver;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class SearchResultWizardPage extends WizardPage {

	private final ImportPgpKeyFromServerData importPgpKeyFromServerData;
	private SearchResultPane searchResultPane;

	public SearchResultWizardPage(final ImportPgpKeyFromServerData importPgpKeyFromServerData) {
		super("Search result");
		this.importPgpKeyFromServerData = assertNotNull("importPgpKeyFromServerData", importPgpKeyFromServerData);
	}

	@Override
	protected void onShown() {
		super.onShown();
		searchResultPane.searchAsync();
	}

	@Override
	protected Parent createContent() {
		return searchResultPane = new SearchResultPane(importPgpKeyFromServerData);
	}
}
