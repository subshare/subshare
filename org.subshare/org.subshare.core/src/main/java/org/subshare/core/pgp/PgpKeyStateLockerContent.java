package org.subshare.core.pgp;

import java.io.IOException;

import org.subshare.core.locker.FileLockerContent;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;

public class PgpKeyStateLockerContent extends FileLockerContent {

	private Pgp pgp;

	@Override
	public File getFile() {
		return getPgpKeyStateRegistry().getFile();
	}

	protected PgpKeyStateRegistryImpl getPgpKeyStateRegistry() {
		final PgpKeyStateRegistryImpl registry = (PgpKeyStateRegistryImpl) PgpKeyStateRegistryImpl.getInstance();
		return registry;
	}

	@Override
	public Uid getLocalVersion() {
		return getPgpKeyStateRegistry().getVersion();
	}

	@Override
	public void mergeFrom(byte[] serverData) throws IOException {
		getPgpKeyStateRegistry().mergeFrom(serverData);
	}

	public Pgp getPgp() {
		if (pgp == null)
			pgp = PgpRegistry.getInstance().getPgpOrFail();

		return pgp;
	}
}
