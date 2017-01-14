package org.subshare.gui.pgp.creatingkey;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.util.FxmlUtil.*;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.core.pgp.PgpUserId;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

public abstract class CreatingPgpKeyPane extends GridPane {

	@FXML
	private Text messageText;

	public CreatingPgpKeyPane(CreatePgpKeyParam createPgpKeyParam) {
		assertNotNull(createPgpKeyParam, "createPgpKeyParam"); //$NON-NLS-1$
		loadDynamicComponentFxml(CreatingPgpKeyPane.class, this);

		final PgpUserId pgpUserId = createPgpKeyParam.getUserIds().get(0);
		messageText.setText(String.format(
				Messages.getString("CreatingPgpKeyPane.messageText.text"), pgpUserId)); //$NON-NLS-1$
	}

	@FXML
	protected abstract void closeButtonClicked(final ActionEvent event);

}
