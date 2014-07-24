package org.subshare.rest.client.transport;

import java.net.URL;

class RestRepoTransport extends co.codewizards.cloudstore.rest.client.transport.RestRepoTransport {

	@Override
	protected URL determineRemoteRootWithoutPathPrefix() {
		return super.determineRemoteRootWithoutPathPrefix();
	}

}
