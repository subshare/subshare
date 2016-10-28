package org.subshare.gui.welcome.pgp.privatekeypassphrase.first;

import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class FirstWizardPage extends WizardPage {

	public FirstWizardPage() {
		super("Welcome to Subshare!");
	}

	@Override
	protected Parent createContent() {
		return new FirstPane();
	}

	@Override
	public void requestFocus() {
		super.requestFocus();
		nextButton.requestFocus();
	}
}
