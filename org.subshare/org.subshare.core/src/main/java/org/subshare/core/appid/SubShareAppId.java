package org.subshare.core.appid;

import co.codewizards.cloudstore.core.appid.AppId;

public class SubShareAppId implements AppId {

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public String getSimpleId() {
		return "subshare";
	}

	@Override
	public String getQualifiedId() {
		return "org.subshare";
	}

	@Override
	public String getWebSiteBaseUrl() {
		return "http://subshare.org/";
	}

}
