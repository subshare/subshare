package org.subshare.gui.pgp.privatekeypassphrase;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import org.subshare.core.pgp.PgpKey;

public class PgpPrivateKeyPassphrasePromptDialog extends Stage {

	private final PgpPrivateKeyPassphrasePromptPane pgpPrivateKeyPassphrasePromptPane;

	private char[] passphrase;

	public PgpPrivateKeyPassphrasePromptDialog(final Window owner, final PgpKey pgpKey) {
		assertNotNull("owner", owner);
		assertNotNull("pgpKey", pgpKey);

		setTitle("PGP private key");
		setResizable(false);
        initStyle(StageStyle.UTILITY);
        initModality(Modality.WINDOW_MODAL);
        initOwner(owner);
        setIconified(false);

        pgpPrivateKeyPassphrasePromptPane = new PgpPrivateKeyPassphrasePromptPane(pgpKey) {
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
