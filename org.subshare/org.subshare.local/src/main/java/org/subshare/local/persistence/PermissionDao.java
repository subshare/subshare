package org.subshare.local.persistence;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.PermissionType;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;

public class PermissionDao extends Dao<Permission, PermissionDao> {

	private static final Logger logger = LoggerFactory.getLogger(PermissionDao.class);

	public Collection<Permission> getNonRevokedPermissions(final PermissionType permissionType, final Set<Uid> userRepoKeyIds) {
		requireNonNull(permissionType, "permissionType");
		requireNonNull(userRepoKeyIds, "userRepoKeyIds");
		final List<Permission> permissions = new ArrayList<Permission>();
		for (final Uid userRepoKeyId : userRepoKeyIds) {
			final Collection<Permission> c = getNonRevokedPermissions( permissionType, userRepoKeyId);
			permissions.addAll(c);
		}
		return permissions;
	}

	public Collection<Permission> getNonRevokedPermissions(final PermissionType permissionType, final Uid userRepoKeyId) {
		requireNonNull(permissionType, "permissionType");
		requireNonNull(userRepoKeyId, "userRepoKeyId");

		final Query query = pm().newNamedQuery(getEntityClass(), "getNonRevokedPermissions_permissionType_userRepoKeyId");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(2);
			params.put("permissionType", permissionType);
			params.put("userRepoKeyId", userRepoKeyId.toString());

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<Permission> permissions = (Collection<Permission>) query.executeWithMap(params);
			logger.debug("getNonRevokedPermissions: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			permissions = load(permissions);
			logger.debug("getNonRevokedPermissions: Loading result-set with {} elements took {} ms.", permissions.size(), System.currentTimeMillis() - startTimestamp);

			return permissions;
		} finally {
			query.closeAll();
		}
	}

	public Collection<Permission> getNonRevokedPermissions(PermissionType permissionType) {
		requireNonNull(permissionType, "permissionType");

		final Query query = pm().newNamedQuery(getEntityClass(), "getNonRevokedPermissions_permissionType");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(1);
			params.put("permissionType", permissionType);

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<Permission> permissions = (Collection<Permission>) query.executeWithMap(params);
			logger.debug("getNonRevokedPermissions: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			permissions = load(permissions);
			logger.debug("getNonRevokedPermissions: Loading result-set with {} elements took {} ms.", permissions.size(), System.currentTimeMillis() - startTimestamp);

			return permissions;
		} finally {
			query.closeAll();
		}
	}

	public Collection<Permission> getNonRevokedPermissions(final PermissionSet permissionSet, final PermissionType permissionType, final UserRepoKeyPublicKey userRepoKeyPublicKey) {
		requireNonNull(permissionSet, "permissionSet");
		requireNonNull(permissionType, "permissionType");
		requireNonNull(userRepoKeyPublicKey, "userRepoKeyPublicKey");
		return getNonRevokedPermissions(permissionSet, permissionType, userRepoKeyPublicKey.getUserRepoKeyId());
	}

	public Collection<Permission> getNonRevokedPermissions(final PermissionSet permissionSet, final PermissionType permissionType, final Set<Uid> userRepoKeyIds) {
		requireNonNull(permissionSet, "permissionSet");
		requireNonNull(permissionType, "permissionType");
		requireNonNull(userRepoKeyIds, "userRepoKeyIds");

		final List<Permission> permissions = new ArrayList<Permission>();
		for (final Uid userRepoKeyId : userRepoKeyIds) {
			final Collection<Permission> c = getNonRevokedPermissions(permissionSet, permissionType, userRepoKeyId);
			permissions.addAll(c);
		}
		return permissions;
	}

	public Collection<Permission> getNonRevokedPermissions(final PermissionSet permissionSet, final PermissionType permissionType, final Uid userRepoKeyId) {
		requireNonNull(permissionSet, "permissionSet");
		requireNonNull(permissionType, "permissionType");
		requireNonNull(userRepoKeyId, "userRepoKeyId");

		final Query query = pm().newNamedQuery(getEntityClass(), "getNonRevokedPermissions_permissionSet_permissionType_userRepoKeyId");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(3);
			params.put("permissionSet", permissionSet);
			params.put("permissionType", permissionType);
			params.put("userRepoKeyId", userRepoKeyId.toString());

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<Permission> permissions = (Collection<Permission>) query.executeWithMap(params);
			logger.debug("getNonRevokedPermissions: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			permissions = load(permissions);
			logger.debug("getNonRevokedPermissions: Loading result-set with {} elements took {} ms.", permissions.size(), System.currentTimeMillis() - startTimestamp);

			return permissions;
		} finally {
			query.closeAll();
		}
	}

	public Collection<Permission> getValidPermissions(
			final PermissionType permissionType, final Uid userRepoKeyId, final Date timestamp) {
		requireNonNull(permissionType, "permissionType");
		requireNonNull(userRepoKeyId, "userRepoKeyId");
		requireNonNull(timestamp, "timestamp");

		final Query query = pm().newNamedQuery(getEntityClass(), "getValidPermissions_permissionType_userRepoKeyId_timestamp");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(3);
			params.put("permissionType", permissionType);
			params.put("userRepoKeyId", userRepoKeyId.toString());
			params.put("timestamp", timestamp);

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<Permission> permissions = (Collection<Permission>) query.executeWithMap(params);
			logger.debug("getValidPermissions: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			permissions = load(permissions);
			logger.debug("getValidPermissions: Loading result-set with {} elements took {} ms.", permissions.size(), System.currentTimeMillis() - startTimestamp);

			return permissions;
		} finally {
			query.closeAll();
		}
	}

	public Collection<Permission> getValidPermissions(
			final PermissionSet permissionSet, final PermissionType permissionType, final Uid userRepoKeyId, final Date timestamp) {
		requireNonNull(permissionSet, "permissionSet");
		requireNonNull(permissionType, "permissionType");
		requireNonNull(userRepoKeyId, "userRepoKeyId");
		requireNonNull(timestamp, "timestamp");

		final Query query = pm().newNamedQuery(getEntityClass(), "getValidPermissions_permissionSet_permissionType_userRepoKeyId_timestamp");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(3);
			params.put("permissionSet", permissionSet);
			params.put("permissionType", permissionType);
			params.put("userRepoKeyId", userRepoKeyId.toString());
			params.put("timestamp", timestamp);

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<Permission> permissions = (Collection<Permission>) query.executeWithMap(params);
			logger.debug("getValidPermissions: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			permissions = load(permissions);
			logger.debug("getValidPermissions: Loading result-set with {} elements took {} ms.", permissions.size(), System.currentTimeMillis() - startTimestamp);

			return permissions;
		} finally {
			query.closeAll();
		}
	}

	public Collection<Permission> getPermissions(final UserRepoKeyPublicKey userRepoKeyPublicKey) {
		requireNonNull(userRepoKeyPublicKey, "userRepoKeyPublicKey");

		final Query query = pm().newNamedQuery(getEntityClass(), "getPermissions_userRepoKeyPublicKey");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(3);
			params.put("userRepoKeyPublicKey", userRepoKeyPublicKey);

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<Permission> permissions = (Collection<Permission>) query.executeWithMap(params);
			logger.debug("getPermissions: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			permissions = load(permissions);
			logger.debug("getPermissions: Loading result-set with {} elements took {} ms.", permissions.size(), System.currentTimeMillis() - startTimestamp);

			return permissions;
		} finally {
			query.closeAll();
		}
	}

	public long getPermissionCountOfDirectChildCryptoRepoFiles(final CryptoRepoFile parentCryptoRepoFile, final PermissionType permissionType) {
		requireNonNull(parentCryptoRepoFile, "parentCryptoRepoFile");
		requireNonNull(permissionType, "permissionType");

		final Query query = pm().newNamedQuery(getEntityClass(), "PermissionCountOfDirectChildCryptoRepoFiles_parentCryptoRepoFile_permissionType");
		try {
			final Long count = (Long) query.execute(parentCryptoRepoFile, permissionType);
			return count;
		} finally {
			query.closeAll();
		}
	}

	/**
	 * Get those {@link Permission}s whose {@link Permission#getLocalRevision() localRevision} is greater
	 * than the given {@code localRevision}.
	 * <p>
	 * TODO We should solve <a href="https://github.com/cloudstore/cloudstore/issues/25">issue 25</a> for this
	 * situation here, too, but taking the new TO-DO-notes there into account as well.
	 * @param localRevision the {@link Permission#getLocalRevision() localRevision}, after which the files
	 * to be queried where modified.
	 * @return those {@link Permission}s which were modified after the given {@code localRevision}. Never
	 * <code>null</code>, but maybe empty.
	 */
	public Collection<Permission> getPermissionsChangedAfter(final long localRevision) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getPermissionsChangedAfter_localRevision");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<Permission> permissions = (Collection<Permission>) query.execute(localRevision);
			logger.debug("getPermissionsChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			permissions = load(permissions);
			logger.debug("getPermissionsChangedAfter: Loading result-set with {} elements took {} ms.", permissions.size(), System.currentTimeMillis() - startTimestamp);

			return permissions;
		} finally {
			query.closeAll();
		}
	}

	public Permission getPermission(final Uid permissionId) {
		requireNonNull(permissionId, "permissionId");
		final Query query = pm().newNamedQuery(getEntityClass(), "getPermission_permissionId");
		try {
			final Permission permission = (Permission) query.execute(permissionId.toString());
			return permission;
		} finally {
			query.closeAll();
		}
	}

	public Collection<Permission> getPermissionsSignedBy(final Uid signingUserRepoKeyId) {
		requireNonNull(signingUserRepoKeyId, "signingUserRepoKeyId");
		final Query query = pm().newNamedQuery(getEntityClass(), "getPermissions_signingUserRepoKeyId");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<Permission> permissions = (Collection<Permission>) query.execute(signingUserRepoKeyId.toString());
			logger.debug("getPermissionsSignedBy: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			permissions = load(permissions);
			logger.debug("getPermissionsSignedBy: Loading result-set with {} elements took {} ms.", permissions.size(), System.currentTimeMillis() - startTimestamp);

			return permissions;
		} finally {
			query.closeAll();
		}
	}
}
