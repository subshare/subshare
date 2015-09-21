package org.subshare.gui.pgp.createkey;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.gui.pgp.createkey.passphrase.PassphraseWizardPage;
import org.subshare.gui.wizard.Wizard;

public abstract class CreatePgpKeyWizard extends Wizard {

	private final CreatePgpKeyParam createPgpKeyParam;

	public CreatePgpKeyWizard(final CreatePgpKeyParam createPgpKeyParam) {
		super(new PassphraseWizardPage(createPgpKeyParam));
		this.createPgpKeyParam = assertNotNull("createPgpKeyParam", createPgpKeyParam); //$NON-NLS-1$
	}

	@Override
	public String getTitle() {
		return Messages.getString("CreatePgpKeyWizard.title"); //$NON-NLS-1$
	}

	@Override
	protected void finishing() {
		createPgpKeyParam.getUserIds().removeIf(pgpUserId -> pgpUserId.isEmpty());
		super.finishing();
	}
}
