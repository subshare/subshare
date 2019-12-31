open module org.subshare.ls.core {

	requires transitive co.codewizards.cloudstore.ls.core;

	requires transitive org.subshare.core;

	exports org.subshare.ls.core.dto;
	exports org.subshare.ls.core.dto.jaxb;
	exports org.subshare.ls.core.invoke.filter;
	exports org.subshare.ls.core.invoke.refjanitor;

}