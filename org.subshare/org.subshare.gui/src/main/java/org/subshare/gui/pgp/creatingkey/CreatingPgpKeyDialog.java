package org.subshare.gui.pgp.creatingkey;

import static java.util.Objects.*;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.core.pgp.PgpUserId;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class CreatingPgpKeyDialog extends Stage {

	private final CreatingPgpKeyPane creatingPgpKeyPane;

	public CreatingPgpKeyDialog(final Window owner, final CreatePgpKeyParam createPgpKeyParam) {
		requireNonNull(owner, "owner");
		requireNonNull(createPgpKeyParam, "createPgpKeyParam");

		PgpUserId pgpUserId = createPgpKeyParam.getUserIds().get(0);

		setTitle(String.format("Creating PGP key pair for %s ...", pgpUserId));
		setResizable(false);
		initStyle(StageStyle.UTILITY);
		initModality(Modality.NONE);
		initOwner(owner);
		setIconified(false);

		creatingPgpKeyPane = new CreatingPgpKeyPane(createPgpKeyParam) {
			@Override
			protected void closeButtonClicked(ActionEvent event) {
				close();
			}
		};
		setScene(new Scene(creatingPgpKeyPane));
	}

}
