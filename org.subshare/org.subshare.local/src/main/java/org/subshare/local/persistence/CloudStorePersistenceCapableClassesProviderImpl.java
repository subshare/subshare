package org.subshare.local.persistence;

import co.codewizards.cloudstore.local.persistence.AbstractCloudStorePersistenceCapableClassesProvider;

public class CloudStorePersistenceCapableClassesProviderImpl extends AbstractCloudStorePersistenceCapableClassesProvider {

	@Override
	public Class<?>[] getPersistenceCapableClasses() {
		return new Class<?>[] {
				// The various sub-classes of CloudStore-entities do not need to be enlisted here, because they are
				// already resolved using the ClassExtension mechanism. We thus only need CC-specific classes here.
				CryptoKey.class,
				CryptoLink.class,
				FileChunkPayload.class,
				HistoFileChunk.class,
				HistoFrame.class,
				InvitationUserRepoKeyPublicKey.class,
				LastCryptoKeySyncToRemoteRepo.class,
				Permission.class,
				PermissionSet.class,
				PermissionSetInheritance.class,
				PreliminaryCollision.class,
				PreliminaryDeletion.class,
				RepositoryOwner.class,
				TempFileChunk.class,
				UserIdentity.class,
				UserIdentityLink.class,
				UserRepoKeyPublicKey.class,
				UserRepoKeyPublicKeyReplacementRequest.class,
				UserRepoKeyPublicKeyReplacementRequestDeletion.class
		};
	}

}
