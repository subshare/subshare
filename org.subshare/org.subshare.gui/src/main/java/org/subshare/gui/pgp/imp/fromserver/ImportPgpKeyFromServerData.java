package org.subshare.gui.pgp.imp.fromserver;

import org.subshare.core.pgp.ImportKeysResult;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

//public class ImportPgpKeyFromServerData extends AbstractBean<ImportPgpKeyFromServerData.Property> {
public class ImportPgpKeyFromServerData {

//	public static interface Property extends PropertyBase { }
//
//	private String queryString;
//
//	public static enum PropertyEnum implements Property {
//		queryString
//	}
//
//	public String getQueryString() {
//		return queryString;
//	}
//	public void setQueryString(String queryString) {
//		setPropertyValue(PropertyEnum.queryString, queryString);
//	}
//
//	@Override
//	public ImportPgpKeyFromServerData clone() {
//		return (ImportPgpKeyFromServerData) super.clone();
//	}

	private final StringProperty queryString = new SimpleStringProperty(this, "queryString");

	private ObjectProperty<ImportKeysResult> importKeysResult = new SimpleObjectProperty<>(this, "importKeysResult");

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

}
