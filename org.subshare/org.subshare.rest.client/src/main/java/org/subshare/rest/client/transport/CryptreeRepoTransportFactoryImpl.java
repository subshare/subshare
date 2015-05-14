package org.subshare.rest.client.transport;

import java.net.URL;

import org.subshare.core.user.UserRepoKeyRingLookup;

import co.codewizards.cloudstore.core.repo.transport.AbstractRepoTransportFactory;
import co.codewizards.cloudstore.core.repo.transport.RepoTransport;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;

public class CryptreeRepoTransportFactoryImpl extends AbstractRepoTransportFactory {
	protected final RestRepoTransportFactory restRepoTransportFactory = new RestRepoTransportFactory();
	private UserRepoKeyRingLookup userRepoKeyRingLookup;

	public Class<? extends DynamicX509TrustManagerCallback> getDynamicX509TrustManagerCallbackClass() {
		return restRepoTransportFactory.getDynamicX509TrustManagerCallbackClass();
	}
	public void setDynamicX509TrustManagerCallbackClass(final Class<? extends DynamicX509TrustManagerCallback> dynamicX509TrustManagerCallbackClass) {
		restRepoTransportFactory.setDynamicX509TrustManagerCallbackClass(dynamicX509TrustManagerCallbackClass);
	}

	public UserRepoKeyRingLookup getUserRepoKeyRingLookup() {
		return userRepoKeyRingLookup;
	}
	public void setUserRepoKeyRingLookup(final UserRepoKeyRingLookup userRepoKeyRingLookup) {
		this.userRepoKeyRingLookup = userRepoKeyRingLookup;
	}

	@Override
	public int getPriority() {
		// We replace the ordinary REST repo transport by returning a higher priority.
		return restRepoTransportFactory.getPriority() + 1;
	}

	@Override
	public String getName() {
		return "SubShare";
	}

	@Override
	public String getDescription() {
		return "SubShare crypto-repository on a remote server accessible via REST";
	}

	@Override
	public boolean isSupported(final URL remoteRoot) {
		// We support the same protocols. Or should we better use a different one like "cc+https",
		// "cryptree+https" or similar (in analogy to "svn+ssh")?
		return restRepoTransportFactory.isSupported(remoteRoot);
	}

	@Override
	protected RepoTransport _createRepoTransport() {
		return new CryptreeRepoTransportImpl();
	}
}
