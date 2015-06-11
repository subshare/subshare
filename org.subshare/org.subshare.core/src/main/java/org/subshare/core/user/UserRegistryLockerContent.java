package org.subshare.core.user;

import java.io.IOException;

import org.subshare.core.locker.FileLockerContent;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;

public class UserRegistryLockerContent extends FileLockerContent {

	@Override
	public File getFile() {
		return getUserRegistry().getFile();
	}

	protected UserRegistryImpl getUserRegistry() {
		final UserRegistryImpl userRegistry = (UserRegistryImpl) UserRegistryImpl.getInstance();
		return userRegistry;
	}

//	@Override
//	protected LockFile acquireLockFile() {
//		return getUserRegistry().acquireLockFile();
//	}
//
//	@Override
//	protected byte[] getData(File file, LockFile lockFile) throws IOException {
////		return super.getData(file, lockFile);
//		if (! file.exists())
//			return new byte[0];
//
//		try (final InputStream in = file.createInputStream();) {
//			final ByteArrayOutputStream out = new ByteArrayOutputStream();
//			Streams.pipeAll(in, out);
//			return out.toByteArray();
//		}
//	}

	@Override
	public void mergeFrom(byte[] serverData) throws IOException {
		getUserRegistry().mergeFrom(serverData);
	}

	@Override
	public Uid getLocalVersion() {
		return getUserRegistry().getVersion();
	}
}
