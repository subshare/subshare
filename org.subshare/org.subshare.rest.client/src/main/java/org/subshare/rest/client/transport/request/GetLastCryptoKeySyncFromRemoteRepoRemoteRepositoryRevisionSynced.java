package org.subshare.rest.client.transport.request;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import co.codewizards.cloudstore.rest.client.request.AbstractRequest;

public class GetLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced extends AbstractRequest<Long> {

	private final String repositoryName;

	public GetLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced(final String repositoryName) {
		this.repositoryName = assertNotNull(repositoryName, "repositoryName");
	}

	@Override
	public Long execute() {
		WebTarget webTarget = createWebTarget("_getLastCryptoKeySyncFromRemoteRepoRemoteRepositoryRevisionSynced", urlEncode(repositoryName));
		final Long result = assignCredentials(webTarget.request(MediaType.TEXT_PLAIN)).get(Long.class);
//		if (resultString == null || resultString.isEmpty()) {
//			return null;
//		}
//		return Long.parseLong(resultString);
		return result;
	}

	@Override
	public boolean isResultNullable() {
		return true;
	}
}
