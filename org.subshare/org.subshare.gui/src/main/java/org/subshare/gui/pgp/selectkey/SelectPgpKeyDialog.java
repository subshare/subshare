package org.subshare.gui.pgp.selectkey;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.subshare.core.pgp.PgpKey;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class SelectPgpKeyDialog extends Stage {

	private final SelectPgpKeyPane selectPgpKeyPane;

	private List<PgpKey> selectedPgpKeys;

	public SelectPgpKeyDialog(final Window owner, final List<PgpKey> pgpKeys, final Collection<PgpKey> selectedPgpKeys, final SelectionMode selectionMode, final String headerText) {
		assertNotNull(owner, "owner");
		assertNotNull(pgpKeys, "pgpKeys");
		// selectedPgpKeys may be null

		setResizable(false);
        initStyle(StageStyle.UTILITY);
        initModality(Modality.APPLICATION_MODAL);
        initOwner(owner);
        setIconified(false);

        selectPgpKeyPane = new SelectPgpKeyPane(pgpKeys, selectedPgpKeys, selectionMode, headerText) {
			@Override
			protected void okButtonClicked(ActionEvent event) {
				SelectPgpKeyDialog.this.okButtonClicked();
			}

			@Override
			protected void cancelButtonClicked(ActionEvent event) {
				SelectPgpKeyDialog.this.cancelButtonClicked();
			}
        };
        setScene(new Scene(selectPgpKeyPane));

		setOnShown(event -> {
			// First, we must make this dialog request the focus. Otherwise, the focus
			// will stay with the owner-window. IMHO very strange, wrong default behaviour...
			SelectPgpKeyDialog.this.requestFocus();

			// Now, we must make sure the correct field is focused. This causes the passphrase
			// text-field to be focused.
			selectPgpKeyPane.requestFocus();
		});

		getScene().addEventFilter(KeyEvent.ANY, event -> {
			switch (event.getCode()) {
				case ENTER:
					event.consume();

					if (event.getEventType() == KeyEvent.KEY_RELEASED)
						okButtonClicked();

					break;
				case ESCAPE:
					event.consume();

					if (event.getEventType() == KeyEvent.KEY_RELEASED)
						cancelButtonClicked();

					break;
				default:
					break;
			}
		});
	}

	protected void okButtonClicked() {
		selectedPgpKeys = new ArrayList<>(selectPgpKeyPane.getSelectedPgpKeys());
		close();
	}

	protected void cancelButtonClicked() {
		selectedPgpKeys = null;
		close();
	}

	public List<PgpKey> getSelectedPgpKeys() {
		return selectedPgpKeys;
	}
}
