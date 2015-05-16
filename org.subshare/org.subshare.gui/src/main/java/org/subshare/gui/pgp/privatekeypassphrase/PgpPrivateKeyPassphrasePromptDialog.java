package org.subshare.gui.pgp.privatekeypassphrase;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.subshare.core.pgp.PgpKey;

public class PgpPrivateKeyPassphrasePromptDialog extends Stage {

	private final PgpPrivateKeyPassphrasePromptPane pgpPrivateKeyPassphrasePromptPane;

	public PgpPrivateKeyPassphrasePromptDialog(final PgpKey pgpKey) {
		assertNotNull("pgpKey", pgpKey);

		setResizable(false);
        initStyle(StageStyle.UTILITY);
        initModality(Modality.APPLICATION_MODAL);
        setIconified(false);

        pgpPrivateKeyPassphrasePromptPane = new PgpPrivateKeyPassphrasePromptPane(pgpKey);
        setScene(new Scene(pgpPrivateKeyPassphrasePromptPane));
	}

}
