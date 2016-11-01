package org.subshare.gui.pgp.imp.fromserver;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.gui.wizard.WizardPage;

import javafx.beans.InvalidationListener;
import javafx.scene.Parent;

public class SearchResultWizardPage extends WizardPage {

	private final ImportPgpKeyFromServerData importPgpKeyFromServerData;
	private SearchResultPane searchResultPane;

	public SearchResultWizardPage(final ImportPgpKeyFromServerData importPgpKeyFromServerData) {
		super("Search result");
		this.importPgpKeyFromServerData = assertNotNull("importPgpKeyFromServerData", importPgpKeyFromServerData);

		// This wizard-page must be shown - even though the selection is done automatically!
		shownRequired.set(true);

		// And it must be shown again, if the query-string is modified (in order to trigger a new search).
		importPgpKeyFromServerData.queryStringProperty().addListener((InvalidationListener) observable -> shown.set(false));
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
