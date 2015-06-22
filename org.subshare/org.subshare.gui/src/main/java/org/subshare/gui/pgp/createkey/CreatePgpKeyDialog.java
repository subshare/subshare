package org.subshare.gui.pgp.createkey;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.core.pgp.PgpUserId;

public class CreatePgpKeyDialog extends Stage {

	private final CreatePgpKeyParam createPgpKeyParam;
	private boolean okPressed;

	private CreatePgpKeyPane createPgpKeyPane;

	public CreatePgpKeyDialog(final Window owner, final CreatePgpKeyParam createPgpKeyParam) {
		assertNotNull("owner", owner);
		this.createPgpKeyParam = assertNotNull("createPgpKeyParam", createPgpKeyParam);

		setTitle("Create PGP key pair");
		setResizable(true);
		initStyle(StageStyle.UTILITY);
		initModality(Modality.APPLICATION_MODAL);
		initOwner(owner);
		setIconified(false);

		createPgpKeyPane = new CreatePgpKeyPane(createPgpKeyParam) {
			@Override
			protected void okButtonClicked(ActionEvent event) {
				CreatePgpKeyDialog.this.okButtonClicked(event);
			}
			@Override
			protected void cancelButtonClicked(ActionEvent event) {
				CreatePgpKeyDialog.this.cancelButtonClicked(event);
			}
		};
		setScene(new Scene(createPgpKeyPane));

		setOnShown(event -> {
			// First, we must make this dialog request the focus. Otherwise, the focus
			// will stay with the owner-window. IMHO very strange, wrong default behaviour...
			CreatePgpKeyDialog.this.requestFocus();

			// Now, we must make sure the correct field is focused.
			createPgpKeyPane.requestFocus();
		});
	}

	public CreatePgpKeyParam getCreatePgpKeyParam() {
		return okPressed ? createPgpKeyParam : null;
	}

	protected void okButtonClicked(final ActionEvent event) {
		okPressed = true;
		removeEmptyUserIds();
		close();
	}

	private void removeEmptyUserIds() {
		List<PgpUserId> emptyUserIds = new ArrayList<>();
		for (PgpUserId pgpUserId : createPgpKeyParam.getUserIds()) {
			if (pgpUserId.isEmpty())
				emptyUserIds.add(pgpUserId);
		}

		createPgpKeyParam.getUserIds().removeAll(emptyUserIds);
	}

	protected void cancelButtonClicked(final ActionEvent event) {
		okPressed = false;
		close();
	}

}
