open module org.subshare.ls.rest.server {

	requires transitive co.codewizards.cloudstore.ls.rest.server;

	requires transitive org.subshare.ls.core;

	exports org.subshare.ls.rest.server;
	exports org.subshare.ls.rest.server.service;

}