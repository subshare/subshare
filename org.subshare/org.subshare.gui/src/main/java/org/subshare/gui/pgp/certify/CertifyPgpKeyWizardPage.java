package org.subshare.gui.pgp.certify;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class CertifyPgpKeyWizardPage extends WizardPage {

	private final CertifyPgpKeyData certifyPgpKeyData;

	public CertifyPgpKeyWizardPage(final CertifyPgpKeyData certifyPgpKeyData) {
		super(Messages.getString("CertifyPgpKeyWizardPage.title")); //$NON-NLS-1$
		this.certifyPgpKeyData = assertNotNull("certifyPgpKeyData", certifyPgpKeyData); //$NON-NLS-1$
	}

	@Override
	protected Parent createContent() {
		return new CertifyPgpKeyPane(certifyPgpKeyData);
	}

}
