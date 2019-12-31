open module org.subshare.ls.server {

	requires transitive co.codewizards.cloudstore.ls.server;

	requires transitive org.subshare.ls.rest.server;

	exports org.subshare.ls.server;
}