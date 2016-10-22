package org.subshare.gui.pgp.imp.fromserver;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import org.subshare.gui.wizard.WizardPageContentGridPane;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class SearchCriteriaPane extends WizardPageContentGridPane {

	private final ImportPgpKeyFromServerData importPgpKeyFromServerData;
	private final InvalidationListener updateCompleteInvalidationListener = (InvalidationListener) observable -> updateComplete();

	@FXML
	private TextField queryStringTextField;

	public SearchCriteriaPane(final ImportPgpKeyFromServerData importPgpKeyFromServerData) {
		loadDynamicComponentFxml(SearchCriteriaPane.class, this);
		this.importPgpKeyFromServerData = assertNotNull("importPgpKeyFromServerData", importPgpKeyFromServerData);
		queryStringTextField.textProperty().bindBidirectional(importPgpKeyFromServerData.queryStringProperty());
		importPgpKeyFromServerData.queryStringProperty().addListener(new WeakInvalidationListener(updateCompleteInvalidationListener));
		updateComplete();
	}

	@Override
	protected boolean isComplete() {
		final String queryString = importPgpKeyFromServerData.queryStringProperty().get();
		boolean result = ! isEmpty(trim(queryString));
		System.out.println("isComplete: " + result);
		return result;
	}
}
