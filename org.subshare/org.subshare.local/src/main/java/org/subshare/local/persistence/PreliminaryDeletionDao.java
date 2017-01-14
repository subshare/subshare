package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.jdo.Query;

import co.codewizards.cloudstore.local.persistence.Dao;

public class PreliminaryDeletionDao extends Dao<PreliminaryDeletion, PreliminaryDeletionDao> {
	public PreliminaryDeletion getPreliminaryDeletion(final CryptoRepoFile cryptoRepoFile) {
		assertNotNull(cryptoRepoFile, "cryptoRepoFile");
		final Query query = pm().newNamedQuery(getEntityClass(), "getPreliminaryDeletion_cryptoRepoFile");
		try {
			final PreliminaryDeletion result = (PreliminaryDeletion) query.execute(cryptoRepoFile);
			return result;
		} finally {
			query.closeAll();
		}
	}
}
