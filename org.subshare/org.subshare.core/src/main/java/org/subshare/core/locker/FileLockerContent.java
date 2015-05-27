package org.subshare.core.locker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.util.io.Streams;

import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;

public abstract class FileLockerContent implements LockerContent {

	@Override
	public String getName() {
		return getFileOrFail().getName();
	}

	protected abstract File getFile();

	protected File getFileOrFail() {
		final File file = getFile();
		if (file == null)
			throw new IllegalStateException(String.format("Implementation error in %s: getFile() returned null!", getClass().getName()));

		return file;
	}

	@Override
	public byte[] getLocalData() throws IOException {
		final File file = getFileOrFail();
		try (final LockFile lockFile = acquireLockFile();) {
			return getData(file, lockFile);
		}
	}

	protected LockFile acquireLockFile() {
		final File file = getFileOrFail();
		final LockFile lockFile = LockFileFactory.getInstance().acquire(file, 30000);
		return lockFile;
	}

	protected byte[] getData(final File file, final LockFile lockFile) throws IOException {
		try (final InputStream in = lockFile.createInputStream();) {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			Streams.pipeAll(in, out);
			return out.toByteArray();
		}
	}
}
