package org.subshare.gui.pgp.imp.fromserver;

import org.subshare.core.pgp.ImportKeysResult;
import org.subshare.core.user.ImportUsersFromPgpKeysResult;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ImportPgpKeyFromServerData {

	private final StringProperty queryString = new SimpleStringProperty(this, "queryString");

	private ObjectProperty<ImportKeysResult> importKeysResult = new SimpleObjectProperty<>(this, "importKeysResult");

	private ObjectProperty<ImportUsersFromPgpKeysResult> importUsersResult = new SimpleObjectProperty<>(this, "importUsersResult");

	public String getQueryString() {
		return queryString.get();
	}
	public void setQueryString(String queryString) {
		this.queryString.set(queryString);
	}
	public StringProperty queryStringProperty() {
		return queryString;
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

}
