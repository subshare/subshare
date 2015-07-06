package org.subshare.gui.welcome;

import org.subshare.gui.wizard.WizardPage;

public abstract class WelcomeWizardPage extends WizardPage {

	protected WelcomeWizardPage(String title) {
		super(title);
	}

	protected WelcomeData getWelcomeData() {
		return ((WelcomeWizard)getWizard()).getWelcomeData();
	}
}
