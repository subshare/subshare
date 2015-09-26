package org.subshare.gui.welcome.first;

import javafx.scene.Parent;

import org.subshare.gui.wizard.WizardPage;

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
