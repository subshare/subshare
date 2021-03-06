package org.subshare.rest.client.transport;

import java.net.URL;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;

class RestRepoTransport extends co.codewizards.cloudstore.rest.client.transport.RestRepoTransport {

	@Override
	protected File getLocalRepoTmpDir() {
		return super.getLocalRepoTmpDir();
	}

	@Override
	protected URL determineRemoteRootWithoutPathPrefix() {
		return super.determineRemoteRootWithoutPathPrefix();
	}

	@Override
	protected CloudStoreRestClient getClient() {
		return super.getClient();
	}

}
