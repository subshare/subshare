package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.local.persistence.Dao;

public class UserIdentityDao extends Dao<UserIdentity, UserIdentityDao> {

	private static final Logger logger = LoggerFactory.getLogger(UserIdentityDao.class);

	public Collection<UserIdentity> getUserIdentitiesOf(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		assertNotNull("ofUserRepoKeyPublicKey", ofUserRepoKeyPublicKey);

		final Query query = pm().newNamedQuery(getEntityClass(), "getUserIdentities_ofUserRepoKeyPublicKey");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(1);
			params.put("ofUserRepoKeyPublicKey", ofUserRepoKeyPublicKey);

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<UserIdentity> result = (Collection<UserIdentity>) query.executeWithMap(params);
			logger.debug("getUserIdentitiesOf: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getUserIdentitiesOf: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<UserIdentity> getUserIdentitiesFor(final UserRepoKeyPublicKey forUserRepoKeyPublicKey) {
		assertNotNull("forUserRepoKeyPublicKey", forUserRepoKeyPublicKey);

		final Query query = pm().newNamedQuery(getEntityClass(), "getUserIdentities_forUserRepoKeyPublicKey");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(1);
			params.put("forUserRepoKeyPublicKey", forUserRepoKeyPublicKey);

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<UserIdentity> result = (Collection<UserIdentity>) query.executeWithMap(params);
			logger.debug("getUserIdentitiesFor: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getUserIdentitiesFor: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<UserIdentity> getUserIdentities(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey, final UserRepoKeyPublicKey forUserRepoKeyPublicKey) {
		assertNotNull("ofUserRepoKeyPublicKey", ofUserRepoKeyPublicKey);
		assertNotNull("forUserRepoKeyPublicKey", forUserRepoKeyPublicKey);

		final Query query = pm().newNamedQuery(getEntityClass(), "getUserIdentities_ofUserRepoKeyPublicKey_forUserRepoKeyPublicKey");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(2);
			params.put("ofUserRepoKeyPublicKey", ofUserRepoKeyPublicKey);
			params.put("forUserRepoKeyPublicKey", forUserRepoKeyPublicKey);

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<UserIdentity> result = (Collection<UserIdentity>) query.executeWithMap(params);
			logger.debug("getUserIdentities: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getUserIdentities: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}
}
