package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.Collection;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;

public class PermissionSetInheritanceDao extends Dao<PermissionSetInheritance, PermissionSetInheritanceDao> {

	private static final Logger logger = LoggerFactory.getLogger(PermissionSetInheritanceDao.class);

	/**
	 * Get those {@link PermissionSetInheritance}s whose {@link PermissionSetInheritance#getLocalRevision() localRevision} is greater
	 * than the given {@code localRevision}.
	 * <p>
	 * TODO We should solve <a href="https://github.com/cloudstore/cloudstore/issues/25">issue 25</a> for this
	 * situation here, too, but taking the new TO-DO-notes there into account as well.
	 * @param localRevision the {@link PermissionSetInheritance#getLocalRevision() localRevision}, after which the files
	 * to be queried where modified.
	 * @return those {@link PermissionSetInheritance}s which were modified after the given {@code localRevision}. Never
	 * <code>null</code>, but maybe empty.
	 */
	public Collection<PermissionSetInheritance> getPermissionSetInheritancesChangedAfter(final long localRevision) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getPermissionSetInheritancesChangedAfter_localRevision");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<PermissionSetInheritance> result = (Collection<PermissionSetInheritance>) query.execute(localRevision);
			logger.debug("getPermissionSetInheritancesChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getPermissionSetInheritancesChangedAfter: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public PermissionSetInheritance getPermissionSetInheritance(final Uid permissionSetInheritanceId) {
		assertNotNull("permissionSetInheritanceId", permissionSetInheritanceId);
		final Query query = pm().newNamedQuery(getEntityClass(), "getPermissionSetInheritance_permissionSetInheritanceId");
		try {
			final PermissionSetInheritance result = (PermissionSetInheritance) query.execute(permissionSetInheritanceId.toString());
			return result;
		} finally {
			query.closeAll();
		}
	}

}
