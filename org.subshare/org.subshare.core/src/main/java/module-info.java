open module org.subshare.core {

	requires transitive co.codewizards.cloudstore.core;

	requires transitive org.subshare.crypto;
	
	provides co.codewizards.cloudstore.core.appid.AppId 
		with org.subshare.core.appid.SubShareAppId;
	
	provides co.codewizards.cloudstore.core.dto.jaxb.CloudStoreJaxbContextProvider
		with org.subshare.core.dto.jaxb.CloudStoreJaxbContextProviderImpl;

	provides co.codewizards.cloudstore.core.objectfactory.ClassExtension
		with
			org.subshare.core.dto.SsDeleteModificationDtoClassExtension,
			org.subshare.core.dto.SsDirectoryDtoClassExtension,
			org.subshare.core.dto.SsFileChunkDtoClassExtension,
			org.subshare.core.dto.SsNormalFileDtoClassExtension,
			org.subshare.core.dto.SsSymlinkDtoClassExtension,
			org.subshare.core.repo.sync.SsRepoToRepoSyncClassExtension,
			org.subshare.core.version.SsLocalVersionInIdeHelperClassExtension,
			org.subshare.core.version.SsVersionInfoProviderClassExtension;

	uses org.subshare.core.CryptreeFactory;
	uses org.subshare.core.LocalRepoStorageFactory;
	uses org.subshare.core.locker.LockerContent;
	uses org.subshare.core.locker.transport.LockerTransportFactory;
	uses org.subshare.core.pgp.Pgp;
	uses org.subshare.core.pgp.transport.PgpTransportFactory;
	uses org.subshare.core.user.UserRepoInvitationManager;

	provides org.subshare.core.locker.LockerContent
		with
			org.subshare.core.pgp.PgpKeyStateLockerContent,
			org.subshare.core.repo.ServerRepoRegistryLockerContent,
			org.subshare.core.server.ServerRegistryLockerContent,
			org.subshare.core.user.UserRegistryLockerContent;

	provides org.subshare.core.locker.transport.LockerTransportFactory
		with org.subshare.core.locker.transport.local.LocalLockerTransportFactory;

	provides org.subshare.core.pgp.Pgp
		with org.subshare.core.pgp.gnupg.BcWithLocalGnuPgPgp;

	provides org.subshare.core.pgp.transport.PgpTransportFactory
		with org.subshare.core.pgp.transport.local.LocalPgpTransportFactory;

	exports org.subshare.core;
	exports org.subshare.core.appid;
	exports org.subshare.core.crypto;
	exports org.subshare.core.dto;
	exports org.subshare.core.dto.jaxb;
	exports org.subshare.core.dto.split;
	exports org.subshare.core.fbor;
	exports org.subshare.core.file;
	exports org.subshare.core.io;
	exports org.subshare.core.locker;
	exports org.subshare.core.locker.sync;
	exports org.subshare.core.locker.transport;
	exports org.subshare.core.locker.transport.local;
	exports org.subshare.core.observable;
	exports org.subshare.core.observable.standard;
	exports org.subshare.core.pgp;
	exports org.subshare.core.pgp.gnupg;
	exports org.subshare.core.pgp.man;
	exports org.subshare.core.pgp.sync;
	exports org.subshare.core.pgp.transport;
	exports org.subshare.core.pgp.transport.local;
	exports org.subshare.core.repair;
	exports org.subshare.core.repo;
	exports org.subshare.core.repo.histo;
	exports org.subshare.core.repo.listener;
	exports org.subshare.core.repo.local;
	exports org.subshare.core.repo.metaonly;
	exports org.subshare.core.repo.sync;
	exports org.subshare.core.repo.transport;
	exports org.subshare.core.server;
	exports org.subshare.core.sign;
	exports org.subshare.core.sync;
	exports org.subshare.core.user;
	exports org.subshare.core.version;

}