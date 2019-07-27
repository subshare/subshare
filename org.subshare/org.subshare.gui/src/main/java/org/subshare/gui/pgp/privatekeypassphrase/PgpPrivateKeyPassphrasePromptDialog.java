package org.subshare.gui.pgp.privatekeypassphrase;

import static java.util.Objects.*;

import org.subshare.core.pgp.PgpKey;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class PgpPrivateKeyPassphrasePromptDialog extends Stage {

	private final PgpPrivateKeyPassphrasePromptPane pgpPrivateKeyPassphrasePromptPane;

	private char[] passphrase;

	public PgpPrivateKeyPassphrasePromptDialog(final Window owner, final PgpKey pgpKey, final String errorMessage) {
		requireNonNull(owner, "owner");
		requireNonNull(pgpKey, "pgpKey");

		setTitle("Unlock PGP private key");
		setResizable(false);
		initStyle(StageStyle.UTILITY);
		initModality(Modality.APPLICATION_MODAL);
		initOwner(owner);
		setIconified(false);

		pgpPrivateKeyPassphrasePromptPane = new PgpPrivateKeyPassphrasePromptPane(pgpKey, errorMessage) {
			@Override
			protected void okButtonClicked(ActionEvent event) {
				PgpPrivateKeyPassphrasePromptDialog.this.okButtonClicked(event);
			}
			@Override
			protected void cancelButtonClicked(ActionEvent event) {
				PgpPrivateKeyPassphrasePromptDialog.this.cancelButtonClicked(event);
			}
		};
		setScene(new Scene(pgpPrivateKeyPassphrasePromptPane));

		setOnShown(event -> {
			// First, we must make this dialog request the focus. Otherwise, the focus
			// will stay with the owner-window. IMHO very strange, wrong default behaviour...
			PgpPrivateKeyPassphrasePromptDialog.this.requestFocus();

			// Now, we must make sure the correct field is focused. This causes the passphrase
			// text-field to be focused.
			pgpPrivateKeyPassphrasePromptPane.requestFocus();
		});
	}

	protected void okButtonClicked(ActionEvent event) {
		passphrase = pgpPrivateKeyPassphrasePromptPane.getPassphrase();
		close();
	}

	protected void cancelButtonClicked(ActionEvent event) {
		passphrase = null;
		close();
	}

	public char[] getPassphrase() {
		return passphrase;
	}
}
