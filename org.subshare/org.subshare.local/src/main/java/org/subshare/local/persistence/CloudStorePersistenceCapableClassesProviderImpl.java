package org.subshare.local.persistence;

import co.codewizards.cloudstore.local.persistence.AbstractCloudStorePersistenceCapableClassesProvider;

public class CloudStorePersistenceCapableClassesProviderImpl extends AbstractCloudStorePersistenceCapableClassesProvider {

	@Override
	public Class<?>[] getPersistenceCapableClasses() {
		return new Class<?>[] {
				// The various sub-classes of CloudStore-entities do not need to be enlisted here, because they are already
				// resolved using the ClassExtension mechanism. We thus only need entities newly introduced by Subshare here.
				Collision.class,
				CollisionPrivate.class,
				CryptoConfigPropSet.class,
				CryptoKey.class,
				CryptoKeyDeactivation.class,
				CryptoLink.class,
				CurrentHistoCryptoRepoFile.class,
				DeletedCollision.class,
				FileChunkPayload.class,
				HistoCryptoRepoFile.class,
				HistoFileChunk.class,
				HistoFrame.class,
				InvitationUserRepoKeyPublicKey.class,
				LastCryptoKeySyncFromRemoteRepo.class,
				LastCryptoKeySyncToRemoteRepo.class,
				Permission.class,
				PermissionSet.class,
				PermissionSetInheritance.class,
				PlainHistoCryptoRepoFile.class,
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
