package org.subshare.test;

import org.subshare.server.SubShareServer;

import co.codewizards.cloudstore.server.CloudStoreServer;
import co.codewizards.cloudstore.test.CloudStoreServerTestSupport;

public class SubShareServerTestSupport extends CloudStoreServerTestSupport {

	@Override
	protected CloudStoreServer createCloudStoreServer() {
		return new SubShareServer();
	}

}
