package org.subshare.gui.welcome.pgp.privatekeypassphrase;

import org.subshare.gui.welcome.pgp.privatekeypassphrase.first.FirstWizardPage;
import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public class IntroWizard extends Wizard {

	public IntroWizard() {
		super(new FirstWizardPage());
	}

	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
		// nothing to do
	}

	@Override
	public String getTitle() {
		return Messages.getString("IntroWizard.title"); //$NON-NLS-1$
	}

}
