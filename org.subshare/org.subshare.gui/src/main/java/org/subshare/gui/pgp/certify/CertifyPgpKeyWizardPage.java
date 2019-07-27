package org.subshare.gui.pgp.certify;

import static java.util.Objects.*;

import org.subshare.gui.wizard.WizardPage;

import javafx.scene.Parent;

public class CertifyPgpKeyWizardPage extends WizardPage {

	protected final CertifyPgpKeyData certifyPgpKeyData;

	public CertifyPgpKeyWizardPage(final CertifyPgpKeyData certifyPgpKeyData) {
		super(Messages.getString("CertifyPgpKeyWizardPage.title")); //$NON-NLS-1$
		this.certifyPgpKeyData = requireNonNull(certifyPgpKeyData, "certifyPgpKeyData"); //$NON-NLS-1$
	}

//	@Override
//	protected void onShown() {
//		super.onShown();
//		runLater(new Runnable() {
//			@Override
//			public void run() {
//			}
//		});
//		getWizard().getScene().getWindow().sizeToScene();
//	}

	@Override
	protected Parent createContent() {
		return new CertifyPgpKeyPane(certifyPgpKeyData);
	}

	public CertifyPgpKeyData getCertifyPgpKeyData() {
		return certifyPgpKeyData;
	}

}
