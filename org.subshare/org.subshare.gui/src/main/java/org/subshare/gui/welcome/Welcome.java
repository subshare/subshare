package org.subshare.gui.welcome;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import javafx.stage.Window;

import org.subshare.gui.util.PlatformUtil;
import org.subshare.gui.wizard.WizardDialog;
import org.subshare.gui.wizard.WizardState;

public class Welcome {

	private final WelcomeData welcomeData = new WelcomeData();
	private final Window owner;

	public Welcome(Window owner) {
		this.owner = assertNotNull("owner", owner);
	}

	public boolean welcome() {
		final boolean[] result = new boolean[1];
		PlatformUtil.runAndWait(new Runnable() {
			@Override
			public void run() {
				WelcomeWizard welcomeWizard = new WelcomeWizard(welcomeData);
				if (welcomeWizard.isNeeded()) {
					WizardDialog wizardDialog = new WizardDialog(owner, welcomeWizard);
					wizardDialog.showAndWait();
					result[0] = WizardState.FINISHED == welcomeWizard.stateProperty().get();
				}
				else
					result[0] = true;
			}
		});
		return result[0];
	}
}
