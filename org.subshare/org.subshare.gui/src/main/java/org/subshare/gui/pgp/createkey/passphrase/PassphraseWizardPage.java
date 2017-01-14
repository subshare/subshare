package org.subshare.gui.pgp.createkey.passphrase;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.gui.pgp.createkey.userid.UserIdWizardPage;
import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class PassphraseWizardPage extends WizardPage {

	private final CreatePgpKeyParam createPgpKeyParam;

	private PassphrasePane passphrasePane;

	public PassphraseWizardPage(final CreatePgpKeyParam createPgpKeyParam) {
		super("Passphrase");
		this.createPgpKeyParam = assertNotNull(createPgpKeyParam, "createPgpKeyParam");
//		setNextPage(new ValidityWizardPage(createPgpKeyParam));
		setNextPage(new UserIdWizardPage(createPgpKeyParam));
	}

	@Override
	protected Parent createContent() {
		passphrasePane = new PassphrasePane(createPgpKeyParam) {
			@Override
			protected void updateComplete() {
				PassphraseWizardPage.this.setComplete(isComplete());
			}
		};
		return passphrasePane;
	}
}
