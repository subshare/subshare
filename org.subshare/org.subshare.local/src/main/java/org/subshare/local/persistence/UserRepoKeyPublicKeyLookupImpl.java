package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.core.user.UserRepoKeyPublicKeyLookup;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class UserRepoKeyPublicKeyLookupImpl implements UserRepoKeyPublicKeyLookup {

	private final LocalRepoTransaction transaction;

	public UserRepoKeyPublicKeyLookupImpl(final LocalRepoTransaction transaction) {
		this.transaction = assertNotNull("", transaction);
	}

	@Override
	public PublicKey getUserRepoKeyPublicKey(final Uid userRepoKeyId) {
		final UserRepoKeyPublicKeyDao userRepoKeyPublicKeyDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);
		final UserRepoKeyPublicKey userRepoKeyPublicKey = userRepoKeyPublicKeyDao.getUserRepoKeyPublicKey(userRepoKeyId);
		if (userRepoKeyPublicKey == null)
			return null;

		return userRepoKeyPublicKey.getPublicKey();
	}
}
