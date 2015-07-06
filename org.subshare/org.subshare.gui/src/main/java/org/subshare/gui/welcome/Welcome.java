package org.subshare.gui.welcome;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.stage.Window;

import org.subshare.gui.util.PlatformUtil;
import org.subshare.gui.wizard.WizardDialog;

public class Welcome {

	private final WelcomeData welcomeData = new WelcomeData();
	private final Window owner;

	public Welcome(Window owner) {
		this.owner = assertNotNull("owner", owner);
	}

	public void welcome() {
		PlatformUtil.runAndWait(new Runnable() {
			@Override
			public void run() {
				WelcomeWizard welcomeWizard = new WelcomeWizard(welcomeData);
				WizardDialog wizardDialog = new WizardDialog(owner, welcomeWizard);
				wizardDialog.showAndWait();
			}
		});
	}
}
