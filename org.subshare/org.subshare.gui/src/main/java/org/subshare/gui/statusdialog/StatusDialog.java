package org.subshare.gui.statusdialog;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class StatusDialog extends Stage {

	private final StatusPane statusPane;

	public StatusDialog(final Window owner, final Modality modality, final String title, final String message) {
		assertNotNull("owner", owner);
		assertNotNull("modality", modality);

		setTitle(title == null ? Messages.getString("StatusDialog.title") : title);
		setResizable(false);
		initStyle(StageStyle.UTILITY);
		initModality(modality);
		initOwner(owner);
		setIconified(false);

		statusPane = new StatusPane(message);
		setScene(new Scene(statusPane));
	}
}
