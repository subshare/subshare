open module org.subshare.server {
	
	requires transitive co.codewizards.cloudstore.server;
	
	requires transitive org.subshare.local;
	requires transitive org.subshare.local.dbrepo;
	requires transitive org.subshare.ls.server;
	requires transitive org.subshare.rest.server;

	exports org.subshare.server;

}