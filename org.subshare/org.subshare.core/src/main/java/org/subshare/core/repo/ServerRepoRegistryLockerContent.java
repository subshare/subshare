package org.subshare.core.repo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.util.io.Streams;
import org.subshare.core.locker.FileLockerContent;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.oio.File;

public class ServerRepoRegistryLockerContent extends FileLockerContent {

	@Override
	public File getFile() {
		return getServerRepoRegistry().getServerRepoRegistryFile();
	}

	protected ServerRepoRegistryImpl getServerRepoRegistry() {
		final ServerRepoRegistryImpl userRegistry = (ServerRepoRegistryImpl) ServerRepoRegistryImpl.getInstance();
		return userRegistry;
	}

	@Override
	protected LockFile acquireLockFile() {
		return getServerRepoRegistry().acquireLockFile();
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
	public void mergeFrom(byte[] serverRepoData) throws IOException {
		getServerRepoRegistry().mergeFrom(serverRepoData);
	}

	@Override
	public Uid getLocalVersion() {
		return getServerRepoRegistry().getVersion();
	}
}
