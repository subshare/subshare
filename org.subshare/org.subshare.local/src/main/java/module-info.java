open module org.subshare.local {

	requires transitive co.codewizards.cloudstore.local;

	requires transitive org.subshare.core;
	
	provides co.codewizards.cloudstore.core.objectfactory.ClassExtension
		with
			org.subshare.local.SsLocalRepoSyncClassExtension,
			org.subshare.local.SsLocalRepoMetaDataImplClassExtension,
			org.subshare.local.db.SsDatabaseMigraterClassExtension,
			org.subshare.local.dto.SsFileChunkDtoConverterClassExtension,
			org.subshare.local.dto.SsRepoFileDtoConverterClassExtension,
			org.subshare.local.persistence.SsDirectoryClassExtension,
			org.subshare.local.persistence.SsLocalRepositoryClassExtension,
			org.subshare.local.persistence.SsRemoteRepositoryClassExtension,
			org.subshare.local.persistence.SsNormalFileClassExtension,
			org.subshare.local.persistence.SsSymlinkClassExtension,
			org.subshare.local.persistence.SsFileChunkClassExtension;
	
	provides co.codewizards.cloudstore.core.repo.local.LocalRepoTransactionListener
		with
			org.subshare.local.persistence.AssignCryptoRepoFileRepoFileListener,
			org.subshare.local.persistence.VerifySignableAndWriteProtectedEntityListener,
			org.subshare.local.persistence.DeleteRepoFileListener,
			org.subshare.local.persistence.DeleteFileChunkListener,
			org.subshare.local.persistence.DeleteTempFileChunkListener,
			org.subshare.local.persistence.LocalRepoCommitEventManagerNotifyingListener;

	provides co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory
		with org.subshare.local.transport.CryptreeFileRepoTransportFactoryImpl;
	
	provides co.codewizards.cloudstore.local.persistence.CloudStorePersistenceCapableClassesProvider
		with org.subshare.local.persistence.CloudStorePersistenceCapableClassesProviderImpl;
	
	provides org.subshare.core.CryptreeFactory
		with org.subshare.local.CryptreeFactoryImpl;
	
	provides org.subshare.core.LocalRepoStorageFactory
		with org.subshare.local.LocalRepoStorageFactoryImpl;
	
	provides org.subshare.core.user.UserRepoInvitationManager
		with org.subshare.local.UserRepoInvitationManagerImpl;

	exports org.subshare.local;
	exports org.subshare.local.dto;
	exports org.subshare.local.persistence;
	exports org.subshare.local.transport;
}