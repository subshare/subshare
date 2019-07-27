package org.subshare.local.persistence;

import static java.util.Objects.*;

import javax.jdo.Query;

import co.codewizards.cloudstore.local.persistence.Dao;

public class PlainHistoCryptoRepoFileDao extends Dao<PlainHistoCryptoRepoFile, PlainHistoCryptoRepoFileDao> {

	public PlainHistoCryptoRepoFile getPlainHistoCryptoRepoFile(final HistoCryptoRepoFile histoCryptoRepoFile) {
		requireNonNull(histoCryptoRepoFile, "histoCryptoRepoFile");
		final Query query = pm().newNamedQuery(getEntityClass(), "getPlainHistoCryptoRepoFile_histoCryptoRepoFile");
		try {
			final PlainHistoCryptoRepoFile result = (PlainHistoCryptoRepoFile) query.execute(histoCryptoRepoFile);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public PlainHistoCryptoRepoFile getPlainHistoCryptoRepoFileOrFail(final HistoCryptoRepoFile histoCryptoRepoFile) {
		requireNonNull(histoCryptoRepoFile, "histoCryptoRepoFile");
		final PlainHistoCryptoRepoFile plainHistoCryptoRepoFile = getPlainHistoCryptoRepoFile(histoCryptoRepoFile);
		if (plainHistoCryptoRepoFile == null)
			throw new IllegalArgumentException("There is no PlainHistoCryptoRepoFile for " + histoCryptoRepoFile);

		return plainHistoCryptoRepoFile;
	}
}
