package org.subshare.gui.pgp.createkey;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.gui.pgp.createkey.advanced.AdvancedWizardPage;
import org.subshare.gui.pgp.createkey.passphrase.PassphraseWizardPage;
import org.subshare.gui.pgp.createkey.userid.UserIdWizardPage;
import org.subshare.gui.pgp.createkey.validity.ValidityWizardPage;
import org.subshare.gui.wizard.Wizard;

public abstract class CreatePgpKeyWizard extends Wizard {

	private final CreatePgpKeyParam createPgpKeyParam;

	public CreatePgpKeyWizard(final CreatePgpKeyParam createPgpKeyParam) {
		super(
				new PassphraseWizardPage(createPgpKeyParam),
				new ValidityWizardPage(createPgpKeyParam),
				new UserIdWizardPage(createPgpKeyParam),
				new AdvancedWizardPage(createPgpKeyParam));

		this.createPgpKeyParam = assertNotNull("createPgpKeyParam", createPgpKeyParam);
	}

	@Override
	public String getTitle() {
		return "Create OpenPGP key";
	}

	@Override
	protected void preFinish() {
		createPgpKeyParam.getUserIds().removeIf(pgpUserId -> pgpUserId.isEmpty());
	}
}
