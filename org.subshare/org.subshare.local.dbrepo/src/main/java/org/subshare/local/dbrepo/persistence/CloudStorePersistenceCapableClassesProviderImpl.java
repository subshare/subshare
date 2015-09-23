package org.subshare.local.dbrepo.persistence;

import co.codewizards.cloudstore.local.persistence.AbstractCloudStorePersistenceCapableClassesProvider;

public class CloudStorePersistenceCapableClassesProviderImpl extends AbstractCloudStorePersistenceCapableClassesProvider {

	@Override
	public Class<?>[] getPersistenceCapableClasses() {
		return new Class<?>[] {
				FileChunkPayload.class
		};
	}

}
