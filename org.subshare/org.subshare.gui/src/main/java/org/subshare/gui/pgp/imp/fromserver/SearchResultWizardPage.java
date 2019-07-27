package org.subshare.gui.pgp.imp.fromserver;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.TempImportKeysResult;
import org.subshare.gui.pgp.certify.CertifyPgpKeyData;
import org.subshare.gui.wizard.WizardPage;

import javafx.beans.InvalidationListener;
import javafx.collections.ObservableMap;
import javafx.scene.Parent;

public class SearchResultWizardPage extends WizardPage {

	private final ImportPgpKeyFromServerData importPgpKeyFromServerData;
	private final Map<PgpKeyId, CertifyPgpKeyWizardPage> pgpKeyId2CertifyPgpKeyWizardPage = new HashMap<>();
	private final ObservableMap<PgpKeyId, CertifyPgpKeyData> pgpKeyId2CertifyPgpKeyData;
	private SearchResultPane searchResultPane;

	public SearchResultWizardPage(final ImportPgpKeyFromServerData importPgpKeyFromServerData) {
		super("Search result");
		this.importPgpKeyFromServerData = requireNonNull(importPgpKeyFromServerData, "importPgpKeyFromServerData");
		this.pgpKeyId2CertifyPgpKeyData = importPgpKeyFromServerData.getPgpKeyId2CertifyPgpKeyData();

		// This wizard-page must be shown - even though the selection is done automatically!
		shownRequired.set(true);

		// And it must be shown again, if the query-string is modified (in order to trigger a new search).
		importPgpKeyFromServerData.queryStringProperty().addListener((InvalidationListener) observable -> shown.set(false) );
		importPgpKeyFromServerData.tempImportKeysResultProperty().addListener((InvalidationListener) observable -> updateCertifyPgpKeyWizardPages() );
		importPgpKeyFromServerData.getSelectedPgpKeyIds().addListener((InvalidationListener) observable -> updateCertifyPgpKeyWizardPages());
	}

	@Override
	protected void onShown() {
		super.onShown();
		searchResultPane.searchAsync();
	}

	@Override
	protected Parent createContent() {
		return searchResultPane = new SearchResultPane(importPgpKeyFromServerData);
	}

	protected void updateCertifyPgpKeyWizardPages() {
		for (final CertifyPgpKeyData certifyPgpKeyData : pgpKeyId2CertifyPgpKeyData.values()) {
			certifyPgpKeyData.setPgp(null);
			certifyPgpKeyData.setPgpKey(null);
		}

		final TempImportKeysResult tempImportKeysResult = importPgpKeyFromServerData.getTempImportKeysResult();
		final Pgp tempPgp = tempImportKeysResult == null ? null : tempImportKeysResult.getTempPgp();
		if (tempPgp == null) {
			setNextPage(null);
			return;
		}

		final List<CertifyPgpKeyWizardPage> certifyPgpKeyWizardPages = new ArrayList<>();
		for (final PgpKeyId pgpKeyId : importPgpKeyFromServerData.getSelectedPgpKeyIds()) {
			final PgpKey pgpKey = tempPgp.getPgpKey(pgpKeyId);
			requireNonNull(pgpKey, "tempPgp.getPgpKey(" + pgpKeyId + ")");

			CertifyPgpKeyData certifyPgpKeyData = pgpKeyId2CertifyPgpKeyData.get(pgpKeyId);
			if (certifyPgpKeyData == null) {
				certifyPgpKeyData = new CertifyPgpKeyData();
				certifyPgpKeyData.setSkip(true);
				pgpKeyId2CertifyPgpKeyData.put(pgpKeyId, certifyPgpKeyData);
			}
			certifyPgpKeyData.setPgp(tempPgp);
			certifyPgpKeyData.setPgpKey(pgpKey);

			CertifyPgpKeyWizardPage cpkwp = pgpKeyId2CertifyPgpKeyWizardPage.get(pgpKeyId);
			if (cpkwp == null) {
				cpkwp = new CertifyPgpKeyWizardPage(certifyPgpKeyData);
				pgpKeyId2CertifyPgpKeyWizardPage.put(pgpKeyId, cpkwp);
			}
			certifyPgpKeyWizardPages.add(cpkwp);
		}

		WizardPage lastWizardPage = this;
		for (final CertifyPgpKeyWizardPage cpkwp : certifyPgpKeyWizardPages) {
			lastWizardPage.setNextPage(cpkwp);
			lastWizardPage = cpkwp;
		}
		lastWizardPage.setNextPage(null);
	}
}
