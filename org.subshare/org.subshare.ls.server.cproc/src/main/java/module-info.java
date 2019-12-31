open module org.subshare.ls.server.cproc {
	
	requires transitive co.codewizards.cloudstore.ls.server.cproc;
	
	requires transitive org.subshare.ls.server;
	requires transitive org.subshare.rest.client;

	exports org.subshare.ls.server.cproc;
	exports org.subshare.ls.server.cproc.ssl;
}