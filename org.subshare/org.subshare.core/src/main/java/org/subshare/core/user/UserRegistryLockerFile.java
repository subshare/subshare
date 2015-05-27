package org.subshare.core.user;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.util.io.Streams;
import org.subshare.core.locker.FileLockerContent;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.oio.File;

public class UserRegistryLockerFile extends FileLockerContent {

	@Override
	public File getFile() {
		return getUserRegistry().getUserRegistryFile();
	}

	protected UserRegistryImpl getUserRegistry() {
		final UserRegistryImpl userRegistry = (UserRegistryImpl) UserRegistryImpl.getInstance();
		return userRegistry;
	}

	@Override
	protected LockFile acquireLockFile() {
		return getUserRegistry().acquireLockFile();
	}

	@Override
	protected byte[] getData(File file, LockFile lockFile) throws IOException {
//		return super.getData(file, lockFile);
		try (final InputStream in = file.createInputStream();) {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			Streams.pipeAll(in, out);
			return out.toByteArray();
		}
	}

	@Override
	public void merge(byte[] serverData) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public Uid getLocalVersion() {
		return getUserRegistry().getVersion();
	}
}
