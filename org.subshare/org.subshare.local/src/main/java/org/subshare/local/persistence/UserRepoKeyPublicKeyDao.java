package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Collection;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.local.persistence.Dao;

public class UserRepoKeyPublicKeyDao extends Dao<UserRepoKeyPublicKey, UserRepoKeyPublicKeyDao> {

	private static final Logger logger = LoggerFactory.getLogger(UserRepoKeyPublicKeyDao.class);


	public UserRepoKeyPublicKey getUserRepoKeyPublicKeyOrFail(final Uid userRepoKeyId) {
		final UserRepoKeyPublicKey key = getUserRepoKeyPublicKey(userRepoKeyId);
		if (key == null)
			throw new IllegalArgumentException("There is no UserRepoKeyPublicKey with this userRepoKeyId: " + userRepoKeyId);

		return key;
	}

	public UserRepoKeyPublicKey getUserRepoKeyPublicKey(final Uid userRepoKeyId) {
		assertNotNull("userRepoKeyId", userRepoKeyId);
		final Query query = pm().newNamedQuery(getEntityClass(), "getUserRepoKeyPublicKey_userRepoKeyId");
		try {
			final UserRepoKeyPublicKey key = (UserRepoKeyPublicKey) query.execute(userRepoKeyId.toString());
			return key;
		} finally {
			query.closeAll();
		}
	}

	/**
	 * Get those {@link UserRepoKeyPublicKey}s whose {@link UserRepoKeyPublicKey#getLocalRevision() localRevision} is greater
	 * than the given {@code localRevision}.
	 * <p>
	 * TODO We should solve <a href="https://github.com/cloudstore/cloudstore/issues/25">issue 25</a> for this
	 * situation here, too, but taking the new TO-DO-notes there into account as well. Note, that this is only
	 * necessary, if UserRepoKeyPublicKey instances are mutable. Currently, they seem never to be changed again.
	 * @param localRevision the {@link UserRepoKeyPublicKey#getLocalRevision() localRevision}, after which the files
	 * to be queried where modified.
	 * @return those {@link UserRepoKeyPublicKey}s which were modified after the given {@code localRevision}. Never
	 * <code>null</code>, but maybe empty.
	 */
	public Collection<UserRepoKeyPublicKey> getUserRepoKeyPublicKeysChangedAfter(final long localRevision) {
		final Query query = pm().newNamedQuery(getEntityClass(), "getUserRepoKeyPublicKeysChangedAfter_localRevision");
		try {
			long startTimestamp = System.currentTimeMillis();
			@SuppressWarnings("unchecked")
			Collection<UserRepoKeyPublicKey> keys = (Collection<UserRepoKeyPublicKey>) query.execute(localRevision);
			logger.debug("getUserRepoKeyPublicKeysChangedAfter: query.execute(...) took {} ms.", System.currentTimeMillis() - startTimestamp);

			startTimestamp = System.currentTimeMillis();
			keys = load(keys);
			logger.debug("getUserRepoKeyPublicKeysChangedAfter: Loading result-set with {} elements took {} ms.", keys.size(), System.currentTimeMillis() - startTimestamp);

			return keys;
		} finally {
			query.closeAll();
		}
	}



}
