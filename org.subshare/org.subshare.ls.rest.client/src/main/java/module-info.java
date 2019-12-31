open module org.subshare.ls.rest.client {

	requires transitive co.codewizards.cloudstore.ls.rest.client;

	requires transitive org.subshare.ls.core;

	exports org.subshare.ls.rest.client.request;

}