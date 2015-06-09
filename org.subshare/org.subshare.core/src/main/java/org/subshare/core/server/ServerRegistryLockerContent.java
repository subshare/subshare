package org.subshare.core.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.util.io.Streams;
import org.subshare.core.locker.FileLockerContent;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.oio.File;

public class ServerRegistryLockerContent extends FileLockerContent {

	@Override
	public File getFile() {
		return getServerRegistry().getServerRegistryFile();
	}

	protected ServerRegistryImpl getServerRegistry() {
		final ServerRegistryImpl userRegistry = (ServerRegistryImpl) ServerRegistryImpl.getInstance();
		return userRegistry;
	}

	@Override
	protected LockFile acquireLockFile() {
		return getServerRegistry().acquireLockFile();
	}

	@Override
	protected byte[] getData(File file, LockFile lockFile) throws IOException {
//		return super.getData(file, lockFile);
		if (! file.exists())
			return new byte[0];

		try (final InputStream in = file.createInputStream();) {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			Streams.pipeAll(in, out);
			return out.toByteArray();
		}
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
