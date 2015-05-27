package org.subshare.core.locker.transport.local;

import java.util.Collections;
import java.util.List;

import org.subshare.core.file.EncryptedDataFile;
import org.subshare.core.locker.transport.AbstractLockerTransport;

import co.codewizards.cloudstore.core.dto.Uid;

public class LocalLockerTransport extends AbstractLockerTransport {

	public LocalLockerTransport() {
	}

	@Override
	public List<Uid> getVersions() {
		return Collections.singletonList(getLockerContentOrFail().getLocalVersion());
	}

	@Override
	public List<EncryptedDataFile> getEncryptedDataFiles() {
		// TODO Auto-generated method stub
		return null;
	}
}
