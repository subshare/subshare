package org.subshare.local.persistence;

import static java.util.Objects.*;

import java.util.Collection;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.local.persistence.Dao;

public class PermissionSetDao extends Dao<PermissionSet, PermissionSetDao> {

	private static final Logger logger = LoggerFactory.getLogger(PermissionSetDao.class);

	public PermissionSet getPermissionSet(final CryptoRepoFile cryptoRepoFile) {
		requireNonNull(cryptoRepoFile, "cryptoRepoFile");
		final Query query = pm().newNamedQuery(getEntityClass(), "getPermissionSet_cryptoRepoFile");
		try {
			final PermissionSet permissionSet = (PermissionSet) query.execute(cryptoRepoFile);
			return permissionSet;
		} finally {
			query.closeAll();
		}
	}

	public PermissionSet getPermissionSetOrFail(final CryptoRepoFile cryptoRepoFile) {
		final PermissionSet permissionSet = getPermissionSet(cryptoRepoFile);
		if (permissionSet == null)
			throw new IllegalArgumentException("There is no PermissionSet for this CryptoRepoFile: " + cryptoRepoFile);

		return permissionSet;
	}

	/**
	 * Get those {@link PermissionSet}s whose {@link PermissionSet#getLocalRevision() localRevision} is greater
	 * than the given {@code localRevision}.
	 * <p>
	 * TODO We should solve <a href="https://github.com/cloudstore/cloudstore/issues/25">issue 25</a> for this
	 * situation here, too, but taking the new TO-DO-notes there into account as well.
	 * @param localRevision the {@link PermissionSet#getLocalRevision() localRevision}, after which the files
	 * to be queried where modified.
	 * @return those {@link PermissionSet}s which were modified after the given {@code localRevision}. Never
	 * <code>null</code>, but maybe empty.
	 */
	public Collection<PermissionSet> getPermissionSetsChangedAfter(final long localRevision) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getPermissionSetsChangedAfter_localRevision");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<PermissionSet> permissionSets = (Collection<PermissionSet>) query.execute(localRevision);
			logger.debug("getPermissionSetsChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			permissionSets = load(permissionSets);
			logger.debug("getPermissionSetsChangedAfter: Loading result-set with {} elements took {} ms.", permissionSets.size(), System.currentTimeMillis() - startTimestamp);

			return permissionSets;
		} finally {
			query.closeAll();
		}
	}

}
