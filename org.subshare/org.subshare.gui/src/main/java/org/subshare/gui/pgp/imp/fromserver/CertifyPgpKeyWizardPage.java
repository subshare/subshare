package org.subshare.gui.pgp.imp.fromserver;

import org.subshare.gui.pgp.certify.CertifyPgpKeyData;

import javafx.scene.Parent;

public class CertifyPgpKeyWizardPage extends org.subshare.gui.pgp.certify.CertifyPgpKeyWizardPage {

	public CertifyPgpKeyWizardPage(CertifyPgpKeyData certifyPgpKeyData) {
		super(certifyPgpKeyData);
	}

	@Override
	protected Parent createContent() {
		return new CertifyPgpKeyPane(certifyPgpKeyData);
	}
}
