package org.subshare.local.persistence;

import co.codewizards.cloudstore.local.persistence.AbstractCloudStorePersistenceCapableClassesProvider;

public class CloudStorePersistenceCapableClassesProviderImpl extends AbstractCloudStorePersistenceCapableClassesProvider {

	@Override
	public Class<?>[] getPersistenceCapableClasses() {
		return new Class<?>[] {
				CryptoKey.class,
				CryptoLink.class,
				InvitationUserRepoKeyPublicKey.class,
				UserRepoKeyPublicKey.class
		};
	}

}
