package org.subshare.core.server;

import java.io.IOException;

import org.subshare.core.locker.FileLockerContent;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;

public class ServerRegistryLockerContent extends FileLockerContent {

	@Override
	public File getFile() {
		getServerRegistry().writeIfNeeded();
		return getServerRegistry().getFile();
	}

	protected ServerRegistryImpl getServerRegistry() {
		final ServerRegistryImpl userRegistry = (ServerRegistryImpl) ServerRegistryImpl.getInstance();
		return userRegistry;
	}

	@Override
	public void mergeFrom(byte[] serverData) throws IOException {
		getServerRegistry().mergeFrom(serverData);
	}

	@Override
	public Uid getLocalVersion() {
		return getServerRegistry().getVersion();
	}
}
