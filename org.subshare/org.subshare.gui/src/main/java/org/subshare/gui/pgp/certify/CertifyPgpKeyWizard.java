package org.subshare.gui.pgp.certify;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.pgp.CertifyPgpKeyParam;
import org.subshare.core.pgp.Pgp;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public class CertifyPgpKeyWizard extends Wizard {

	private final CertifyPgpKeyData certifyPgpKeyData;
	private CertifyPgpKeyParam certifyPgpKeyParam;

	public CertifyPgpKeyWizard(final CertifyPgpKeyData certifyPgpKeyData) {
		this.certifyPgpKeyData = assertNotNull(certifyPgpKeyData, "certifyPgpKeyData"); //$NON-NLS-1$
		assertNotNull(certifyPgpKeyData.getPgpKey(), "certifyPgpKeyData.pgpKey"); //$NON-NLS-1$
		setFirstPage(new CertifyPgpKeyWizardPage(certifyPgpKeyData));
	}

	@Override
	public String getTitle() {
		return Messages.getString("CertifyPgpKeyWizard.title"); //$NON-NLS-1$
	}

	@Override
	protected void finishing() {
		super.finishing();
		certifyPgpKeyParam = new CertifyPgpKeyParam();
		certifyPgpKeyParam.setPgpKey(certifyPgpKeyData.getPgpKey());
		certifyPgpKeyParam.setCertificationLevel(certifyPgpKeyData.getCertificationLevel());
		certifyPgpKeyParam.setSignPgpKey(certifyPgpKeyData.getSignPgpKey());
	}

	@Override
	protected void finish(ProgressMonitor monitor) throws Exception {
		final Pgp pgp = PgpLs.getPgpOrFail();
		pgp.certify(certifyPgpKeyParam);
		pgp.updateTrustDb();
	}
}
