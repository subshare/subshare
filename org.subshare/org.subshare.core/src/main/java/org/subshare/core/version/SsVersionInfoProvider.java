package org.subshare.core.version;

import co.codewizards.cloudstore.core.version.Version;
import co.codewizards.cloudstore.core.version.VersionInfoProvider;

public class SsVersionInfoProvider extends VersionInfoProvider {

	protected SsVersionInfoProvider() {
	}

	@Override
	protected Version getMinimumRemoteVersion() {
		return new Version("0.9.9");
	}
}
