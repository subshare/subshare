package org.subshare.core.appid;

import co.codewizards.cloudstore.core.appid.AppId;

public class SubShareAppId implements AppId {

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public String getSimpleId() {
		return "subshare"; //$NON-NLS-1$
	}

	@Override
	public String getQualifiedId() {
		return "org.subshare"; //$NON-NLS-1$
	}
	
	@Override
	public String getName() {
		return "Subshare"; //$NON-NLS-1$
	}

	@Override
	public String getWebSiteBaseUrl() {
		return "http://subshare.org/"; //$NON-NLS-1$
	}
}
