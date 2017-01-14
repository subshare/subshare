package org.subshare.gui.wizard;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import javafx.beans.InvalidationListener;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class WizardDialog extends Stage {
	private final Wizard wizard;

	public WizardDialog(final Window owner, final Wizard wizard) {
		assertNotNull(owner, "owner");
		this.wizard = assertNotNull(wizard, "wizard");

		setResizable(true);
		initStyle(StageStyle.UTILITY);
		initModality(Modality.APPLICATION_MODAL);
		initOwner(owner);
		setIconified(false);

		wizard.stateProperty().addListener((InvalidationListener) observable -> {
			switch (wizard.stateProperty().get()) {
				case FINISHED:
				case CANCELLED:
					close();
					break;
				default:
					; // nothing
			}
		});
		wizard.init();

		setScene(new Scene(wizard));

		wizard.currentPageProperty().addListener((InvalidationListener) observable -> updateTitle());
		updateTitle();

		setOnShown(event -> {
			// First, we must make this dialog request the focus. Otherwise, the focus
			// will stay with the owner-window. IMHO very strange, wrong default behaviour...
			WizardDialog.this.requestFocus();

			// Now, we must make sure the correct field is focused. This causes the passphrase
			// text-field - or whatever is on the page - to be focused.
			wizard.getCurrentPage().requestFocus();
		});
	}

	private void updateTitle() {
		final WizardPage currentPage = wizard.currentPageProperty().get();
		final String pageTitle = currentPage == null ? null : currentPage.getTitle();
		if (pageTitle == null || equal(wizard.getTitle(), pageTitle))
			setTitle(wizard.getTitle());
		else
			setTitle(String.format("%s – %s", wizard.getTitle(), pageTitle));
	}
}
