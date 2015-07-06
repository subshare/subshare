package org.subshare.gui.welcome.createpgpkey.passphrase;

import javafx.scene.Parent;

import org.subshare.gui.welcome.WelcomeWizardPage;

public class PassphraseWizardPage extends WelcomeWizardPage {

	public PassphraseWizardPage() {
		super("Passphrase");
	}

	@Override
	protected Parent getContent() {
		return new PassphrasePane(getWelcomeData()) {
			@Override
			protected void updateComplete() {
				PassphraseWizardPage.this.completeProperty().set(isComplete());
			}
		};
	}

}
