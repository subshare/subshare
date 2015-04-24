package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;

public class UserIdentityLinkDao extends Dao<UserIdentityLink, UserIdentityLinkDao> {

	private static final Logger logger = LoggerFactory.getLogger(UserIdentityLinkDao.class);

	public UserIdentityLink getUserIdentityLinkOrFail(final Uid userIdentityLinkId) {
		final UserIdentityLink userIdentityLink = getUserIdentityLink(userIdentityLinkId);
		if (userIdentityLink == null)
			throw new IllegalArgumentException("There is no UserIdentityLink with this userIdentityLinkId: " + userIdentityLinkId);

		return userIdentityLink;
	}

	public UserIdentityLink getUserIdentityLink(final Uid userIdentityLinkId) {
		assertNotNull("userIdentityLinkId", userIdentityLinkId);
		final Query query = pm().newNamedQuery(getEntityClass(), "getUserIdentityLink_userIdentityLinkId");
		try {
			final UserIdentityLink userIdentityLink = (UserIdentityLink) query.execute(userIdentityLinkId.toString());
			return userIdentityLink;
		} finally {
			query.closeAll();
		}
	}

	public Collection<UserIdentityLink> getUserIdentityLinksOf(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey) {
		assertNotNull("ofUserRepoKeyPublicKey", ofUserRepoKeyPublicKey);

		final Query query = pm().newNamedQuery(getEntityClass(), "getUserIdentityLinks_ofUserRepoKeyPublicKey");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(1);
			params.put("ofUserRepoKeyPublicKey", ofUserRepoKeyPublicKey);

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<UserIdentityLink> result = (Collection<UserIdentityLink>) query.executeWithMap(params);
			logger.debug("getUserIdentityLinksOf: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getUserIdentityLinksOf: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<UserIdentityLink> getUserIdentityLinksOf(final UserIdentity userIdentity) {
		assertNotNull("userIdentity", userIdentity);

		final Query query = pm().newNamedQuery(getEntityClass(), "getUserIdentityLinks_userIdentity");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(1);
			params.put("userIdentity", userIdentity);

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<UserIdentityLink> result = (Collection<UserIdentityLink>) query.executeWithMap(params);
			logger.debug("getUserIdentityLinksOf: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getUserIdentityLinksOf: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<UserIdentityLink> getUserIdentityLinksFor(final UserRepoKeyPublicKey forUserRepoKeyPublicKey) {
		assertNotNull("forUserRepoKeyPublicKey", forUserRepoKeyPublicKey);

		final Query query = pm().newNamedQuery(getEntityClass(), "getUserIdentityLinks_forUserRepoKeyPublicKey");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(1);
			params.put("forUserRepoKeyPublicKey", forUserRepoKeyPublicKey);

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<UserIdentityLink> result = (Collection<UserIdentityLink>) query.executeWithMap(params);
			logger.debug("getUserIdentityLinksFor: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getUserIdentityLinksFor: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<UserIdentityLink> getUserIdentityLinks(final UserRepoKeyPublicKey ofUserRepoKeyPublicKey, final UserRepoKeyPublicKey forUserRepoKeyPublicKey) {
		assertNotNull("ofUserRepoKeyPublicKey", ofUserRepoKeyPublicKey);
		assertNotNull("forUserRepoKeyPublicKey", forUserRepoKeyPublicKey);

		final Query query = pm().newNamedQuery(getEntityClass(), "getUserIdentityLinks_ofUserRepoKeyPublicKey_forUserRepoKeyPublicKey");
		try {
			final Map<String, Object> params = new HashMap<String, Object>(2);
			params.put("ofUserRepoKeyPublicKey", ofUserRepoKeyPublicKey);
			params.put("forUserRepoKeyPublicKey", forUserRepoKeyPublicKey);

			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<UserIdentityLink> result = (Collection<UserIdentityLink>) query.executeWithMap(params);
			logger.debug("getUserIdentities: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getUserIdentities: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	public Collection<UserIdentityLink> getUserIdentityLinksChangedAfter(final long localRevision) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getUserIdentityLinksChangedAfter_localRevision");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<UserIdentityLink> result = (Collection<UserIdentityLink>) query.execute(localRevision);
			logger.debug("getUserIdentityLinksChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			result = load(result);
			logger.debug("getUserIdentityLinksChangedAfter: Loading result-set with {} elements took {} ms.", result.size(), System.currentTimeMillis() - startTimestamp);

			return result;
		} finally {
			query.closeAll();
		}
	}

	@Override
	public void deletePersistent(final UserIdentityLink entity) {
		final UserIdentity userIdentity = entity.getUserIdentity();

		super.deletePersistent(entity);

		pm().flush();

		deleteIfOrphan(userIdentity);
	}

	@Override
	public void deletePersistentAll(final Collection<? extends UserIdentityLink> entities) {
		final Set<UserIdentity> userIdentities = new HashSet<>();
		for (final UserIdentityLink userIdentityLink : entities)
			userIdentities.add(userIdentityLink.getUserIdentity());

		super.deletePersistentAll(entities);

		pm().flush();

		deleteIfOrphan(userIdentities);
	}

	private void deleteIfOrphan(final Set<UserIdentity> userIdentities) {
		for (UserIdentity userIdentity : userIdentities)
			deleteIfOrphan(userIdentity);
	}

	private void deleteIfOrphan(final UserIdentity userIdentity) {
		final Collection<UserIdentityLink> userIdentityLinks = getUserIdentityLinksOf(userIdentity);
		if (userIdentityLinks.isEmpty())
			getDao(UserIdentityDao.class).deletePersistent(userIdentity);
	}
}
