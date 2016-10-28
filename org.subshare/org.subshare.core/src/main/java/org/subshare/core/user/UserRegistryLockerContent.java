package org.subshare.core.user;

import java.io.IOException;

import org.subshare.core.locker.FileLockerContent;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.oio.File;

public class UserRegistryLockerContent extends FileLockerContent {

	@Override
	public File getFile() {
		getUserRegistry().writeIfNeeded();
		return getUserRegistry().getFile();
	}

	protected UserRegistryImpl getUserRegistry() {
		final UserRegistryImpl userRegistry = (UserRegistryImpl) UserRegistryImpl.getInstance();
		return userRegistry;
	}

	@Override
	public void mergeFrom(byte[] serverData) throws IOException {
		getUserRegistry().mergeFrom(serverData);
	}

	@Override
	public Uid getLocalVersion() {
		return getUserRegistry().getVersion();
	}
}
