package org.subshare.gui.resolvecollision.loading;

import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class LoadingWizardPage extends WizardPage {

	public LoadingWizardPage() {
		super("Loading data...");
	}

	@Override
	protected Parent createContent() {
		return new LoadingPane();
	}

}
