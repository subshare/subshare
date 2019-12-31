open module org.subshare.rest.client {

	requires transitive co.codewizards.cloudstore.rest.client;
	requires transitive org.subshare.rest.shared;

	exports org.subshare.rest.client.locker.transport;
	exports org.subshare.rest.client.locker.transport.request;
	exports org.subshare.rest.client.pgp.transport;
	exports org.subshare.rest.client.pgp.transport.request;
	exports org.subshare.rest.client.transport;
	exports org.subshare.rest.client.transport.request;
	
}