open module org.subshare.local.dbrepo {

	requires transitive org.subshare.local;

	provides co.codewizards.cloudstore.core.repo.transport.RepoTransportFactory
		with org.subshare.local.dbrepo.transport.DbFileRepoTransportFactoryImpl;
}