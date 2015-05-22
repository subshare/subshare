package org.subshare.local.transport;

import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.local.transport.FileRepoTransportFactory;

public class CryptreeFileRepoTransportFactoryImpl extends FileRepoTransportFactory {

	public CryptreeFileRepoTransportFactoryImpl() {
	}

	@Override
	public int getPriority() {
		return super.getPriority() + 1;
	}

	@Override
	protected RepoTransport _createRepoTransport() {
		return new CryptreeFileRepoTransportImpl();
	}
}
