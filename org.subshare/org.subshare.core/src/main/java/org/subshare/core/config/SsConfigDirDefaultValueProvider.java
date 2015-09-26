package org.subshare.core.config;

import co.codewizards.cloudstore.core.config.ConfigDirDefaultValueProvider;

public class SsConfigDirDefaultValueProvider implements ConfigDirDefaultValueProvider {

	@Override
	public int getPriority() {
		return 10;
	}

	@Override
	public String getConfigDir() {
		return "${user.home}/.subshare";
	}
}
