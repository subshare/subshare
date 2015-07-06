package org.subshare.gui.wizard;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class WizardDialog extends Stage {

	public WizardDialog(final Window owner, final Wizard wizard) {
		assertNotNull("owner", owner);

		setTitle(wizard.getTitle());
		setResizable(true);
		initStyle(StageStyle.UTILITY);
		initModality(Modality.APPLICATION_MODAL);
		initOwner(owner);
		setIconified(false);

		wizard.setOnCancel(event -> close());
		wizard.setOnFinish(event -> close());

		setScene(new Scene(wizard));
	}

}
