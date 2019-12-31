open module org.subshare.rest.server {

	requires transitive co.codewizards.cloudstore.rest.server;

	requires transitive org.subshare.rest.shared;

	exports org.subshare.rest.server;
	exports org.subshare.rest.server.service;

}