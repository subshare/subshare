package org.subshare.gui.pgp.imp.fromserver;

import java.util.HashSet;
import java.util.Set;

import org.subshare.core.pgp.ImportKeysResult;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.TempImportKeysResult;
import org.subshare.core.user.ImportUsersFromPgpKeysResult;
import org.subshare.gui.pgp.certify.CertifyPgpKeyData;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;

public class ImportPgpKeyFromServerData {

	private final StringProperty queryString = new SimpleStringProperty(this, "queryString");

	private ObjectProperty<TempImportKeysResult> tempImportKeysResult = new SimpleObjectProperty<TempImportKeysResult>(this, "tempImportKeysResult") {
		@Override
		public void set(TempImportKeysResult newValue) {
			super.set(newValue);
			if (newValue != null) {
				final Set<PgpKeyId> pgpKeyIds = newValue.getImportKeysResult().getPgpKeyId2ImportedMasterKey().keySet();
				selectedPgpKeyIds.retainAll(pgpKeyIds);
				selectedPgpKeyIds.addAll(pgpKeyIds);
			}
			else
				selectedPgpKeyIds.clear();
		}
	};

	private final ObservableSet<PgpKeyId> selectedPgpKeyIds = FXCollections.observableSet(new HashSet<>());

	private final ObjectProperty<ImportKeysResult> importKeysResult = new SimpleObjectProperty<>(this, "importKeysResult");

	private final ObjectProperty<ImportUsersFromPgpKeysResult> importUsersResult = new SimpleObjectProperty<>(this, "importUsersResult");

	private final ObservableMap<PgpKeyId, CertifyPgpKeyData> pgpKeyId2CertifyPgpKeyData = FXCollections.observableHashMap();

	public String getQueryString() {
		return queryString.get();
	}
	public void setQueryString(String queryString) {
		this.queryString.set(queryString);
	}
	public StringProperty queryStringProperty() {
		return queryString;
	}

	public TempImportKeysResult getTempImportKeysResult() {
		return tempImportKeysResult.get();
	}
	public void setTempImportKeysResult(TempImportKeysResult tempImportKeysResult) {
		this.tempImportKeysResult.set(tempImportKeysResult);
	}
	public ObjectProperty<TempImportKeysResult> tempImportKeysResultProperty() {
		return tempImportKeysResult;
	}

	public ObservableSet<PgpKeyId> getSelectedPgpKeyIds() {
		return selectedPgpKeyIds;
	}

	public ImportKeysResult getImportKeysResult() {
		return importKeysResult.get();
	}
	public void setImportKeysResult(ImportKeysResult importKeysResult) {
		this.importKeysResult.set(importKeysResult);
	}
	public ObjectProperty<ImportKeysResult> importKeysResultProperty() {
		return importKeysResult;
	}

	public ImportUsersFromPgpKeysResult getImportUsersResult() {
		return importUsersResult.get();
	}
	public void setImportUsersResult(ImportUsersFromPgpKeysResult importUsersResult) {
		this.importUsersResult.set(importUsersResult);
	}
	public ObjectProperty<ImportUsersFromPgpKeysResult> importUsersResultProperty() {
		return importUsersResult;
	}

	public ObservableMap<PgpKeyId, CertifyPgpKeyData> getPgpKeyId2CertifyPgpKeyData() {
		return pgpKeyId2CertifyPgpKeyData;
	}
}
