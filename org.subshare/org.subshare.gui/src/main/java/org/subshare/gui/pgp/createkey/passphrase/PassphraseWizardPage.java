package org.subshare.gui.pgp.createkey.passphrase;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.scene.Parent;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.gui.wizard.WizardPage;

public class PassphraseWizardPage extends WizardPage {

	private final CreatePgpKeyParam createPgpKeyParam;

	private PassphrasePane passphrasePane;

	public PassphraseWizardPage(final CreatePgpKeyParam createPgpKeyParam) {
		super("Passphrase");
		this.createPgpKeyParam = assertNotNull("createPgpKeyParam", createPgpKeyParam);
	}

	@Override
	protected Parent createContent() {
		passphrasePane = new PassphrasePane(createPgpKeyParam) {
			@Override
			protected void updateComplete() {
				PassphraseWizardPage.this.completeProperty().set(isComplete());
			}
		};
		return passphrasePane;
	}

	@Override
	public void requestFocus() {
		super.requestFocus();

		if (passphrasePane != null)
			passphrasePane.requestFocus();
	}
}
