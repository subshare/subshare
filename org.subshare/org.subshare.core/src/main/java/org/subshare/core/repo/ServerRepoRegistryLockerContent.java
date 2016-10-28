package org.subshare.core.repo;

import java.io.IOException;

import org.subshare.core.locker.FileLockerContent;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.oio.File;

public class ServerRepoRegistryLockerContent extends FileLockerContent {

	@Override
	public File getFile() {
		getServerRepoRegistry().writeIfNeeded();
		return getServerRepoRegistry().getFile();
	}

	protected ServerRepoRegistryImpl getServerRepoRegistry() {
		final ServerRepoRegistryImpl userRegistry = (ServerRepoRegistryImpl) ServerRepoRegistryImpl.getInstance();
		return userRegistry;
	}

	@Override
	public void mergeFrom(byte[] serverRepoData) throws IOException {
		getServerRepoRegistry().mergeFrom(serverRepoData);
	}

	@Override
	public Uid getLocalVersion() {
		return getServerRepoRegistry().getVersion();
	}
}
