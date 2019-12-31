open module org.subshare.local {

	requires transitive co.codewizards.cloudstore.local;

	requires transitive org.subshare.core;

	exports org.subshare.local;
	exports org.subshare.local.dto;
	exports org.subshare.local.persistence;
	exports org.subshare.local.transport;
}