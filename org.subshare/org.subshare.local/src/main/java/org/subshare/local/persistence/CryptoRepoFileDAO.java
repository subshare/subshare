package org.subshare.local.persistence;

import javax.jdo.Query;

import co.codewizards.cloudstore.local.persistence.DAO;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class CryptoRepoFileDAO extends DAO<CryptoRepoFile, CryptoRepoFileDAO> {

	public CryptoRepoFile getCryptoRepoFile(final RepoFile repoFile) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getCryptoRepoFile_repoFile");
		try {
			final CryptoRepoFile cryptoRepoFile = (CryptoRepoFile) query.execute(repoFile);
			return cryptoRepoFile;
		} finally {
			query.closeAll(); // probably not needed for a UNIQUE query, but it shouldn't harm ;-)
		}
	}

}
