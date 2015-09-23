package org.subshare.local;

import org.subshare.core.AbstractLocalRepoStorageFactory;
import org.subshare.core.LocalRepoStorage;

public class LocalRepoStorageFactoryImpl extends AbstractLocalRepoStorageFactory {

	@Override
	protected LocalRepoStorage _createLocalRepoStorage() {
		return new LocalRepoStorageImpl();
	}

}
