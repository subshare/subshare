package org.subshare.gui.welcome;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.pgp.Pgp;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.welcome.createpgpkey.identity.IdentityWizardPage;
import org.subshare.gui.welcome.createpgpkey.passphrase.PassphraseWizardPage;
import org.subshare.gui.welcome.importbackup.ImportBackupWizardPage;
import org.subshare.gui.wizard.Wizard;

public class WelcomeWizard extends Wizard {

	private final WelcomeData welcomeData;

	public WelcomeWizard(final WelcomeData welcomeData) {
		this.welcomeData = assertNotNull("welcomeData", welcomeData);

		pages.add(new FirstWizardPage());

		final Pgp pgp = PgpLs.getPgpOrFail();
		if (pgp.getMasterKeysWithPrivateKey().isEmpty()) {
			pages.addAll(
					new IdentityWizardPage(),
					new ImportBackupWizardPage(),
					new PassphraseWizardPage()
//					new CreatePgpKeyWizardPage()
					);
		}
	}

	public WelcomeData getWelcomeData() {
		return welcomeData;
	}

	@Override
	protected void doFinish() {
		welcomeData.getCreatePgpKeyParam().getUserIds().removeIf(pgpUserId -> pgpUserId.isEmpty());

		// TODO Auto-generated method stub
	}

	@Override
	public String getTitle() {
		return "Welcome to CSX!";
	}

}
