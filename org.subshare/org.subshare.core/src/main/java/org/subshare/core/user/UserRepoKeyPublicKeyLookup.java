package org.subshare.core.user;

import co.codewizards.cloudstore.core.Uid;

public interface UserRepoKeyPublicKeyLookup {

	UserRepoKey.PublicKey getUserRepoKeyPublicKey(Uid userRepoKeyId);

}
