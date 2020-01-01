open module org.subshare.rest.client {

	requires transitive co.codewizards.cloudstore.rest.client;
	requires transitive org.subshare.rest.shared;

	provides co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory
		with org.subshare.rest.client.transport.CryptreeRestRepoTransportFactoryImpl;

	provides org.subshare.core.locker.transport.LockerTransportFactory
		with org.subshare.rest.client.locker.transport.RestLockerTransportFactory;

	provides org.subshare.core.pgp.transport.PgpTransportFactory
		with org.subshare.rest.client.pgp.transport.RestPgpTransportFactory;

	exports org.subshare.rest.client.locker.transport;
	exports org.subshare.rest.client.locker.transport.request;
	exports org.subshare.rest.client.pgp.transport;
	exports org.subshare.rest.client.pgp.transport.request;
	exports org.subshare.rest.client.transport;
	exports org.subshare.rest.client.transport.request;
	
}