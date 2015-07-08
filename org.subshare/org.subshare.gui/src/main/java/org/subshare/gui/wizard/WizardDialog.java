package org.subshare.gui.wizard;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.beans.InvalidationListener;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class WizardDialog extends Stage {

	public WizardDialog(final Window owner, final Wizard wizard) {
		assertNotNull("owner", owner);
		assertNotNull("wizard", wizard);

		setTitle(wizard.getTitle());
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

		setOnShown(event -> {
			// First, we must make this dialog request the focus. Otherwise, the focus
			// will stay with the owner-window. IMHO very strange, wrong default behaviour...
			WizardDialog.this.requestFocus();

			// Now, we must make sure the correct field is focused. This causes the passphrase
			// text-field to be focused.
			wizard.getCurrentPage().requestFocus();
		});
	}
}
