package org.subshare.core.pgp;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

public class TempImportKeysResult {

	private final Pgp tempPgp;
	private final ImportKeysResult importKeysResult;

	public TempImportKeysResult(Pgp tempPgp, ImportKeysResult importKeysResult) {
		this.tempPgp = assertNotNull("tempPgp", tempPgp);
		this.importKeysResult = assertNotNull("importKeysResult", importKeysResult);
	}

	public Pgp getTempPgp() {
		return tempPgp;
	}

	public ImportKeysResult getImportKeysResult() {
		return importKeysResult;
	}
}
