package org.subshare.local.dbrepo.transport;

import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.local.transport.FileRepoTransportFactory;

public class DbFileRepoTransportFactoryImpl extends FileRepoTransportFactory {

	public DbFileRepoTransportFactoryImpl() {
	}

	@Override
	public int getPriority() {
		return super.getPriority() + 1;
	}

	@Override
	protected RepoTransport _createRepoTransport() {
		return new DbFileRepoTransportImpl();
	}
}
