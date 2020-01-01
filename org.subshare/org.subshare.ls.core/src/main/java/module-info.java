open module org.subshare.ls.core {

	requires transitive co.codewizards.cloudstore.ls.core;

	requires transitive org.subshare.core;

	provides co.codewizards.cloudstore.core.dto.jaxb.CloudStoreJaxbContextProvider
		with org.subshare.ls.core.dto.jaxb.CloudStoreJaxbContextProviderImpl;

	provides co.codewizards.cloudstore.ls.core.invoke.filter.InvocationFilter
		with org.subshare.ls.core.invoke.filter.AllowSubShareInvocationFilter;

	provides co.codewizards.cloudstore.ls.core.invoke.refjanitor.ReferenceJanitor
		with org.subshare.ls.core.invoke.refjanitor.LocalRepoCommitEventListenerJanitor;

	exports org.subshare.ls.core.dto;
	exports org.subshare.ls.core.dto.jaxb;
	exports org.subshare.ls.core.invoke.filter;
	exports org.subshare.ls.core.invoke.refjanitor;

}