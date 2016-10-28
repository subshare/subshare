package org.subshare.rest.client.locker.transport;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.ClientBuilder;

import org.subshare.core.locker.LockerEncryptedDataFile;
import org.subshare.core.locker.transport.AbstractLockerTransport;
import org.subshare.rest.client.locker.transport.request.GetLockerContentVersions;
import org.subshare.rest.client.locker.transport.request.GetLockerEncryptedDataFile;
import org.subshare.rest.client.locker.transport.request.PutLockerEncryptedDataFile;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.rest.client.ClientBuilderDefaultValuesDecorator;
import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;
import co.codewizards.cloudstore.rest.client.CredentialsProvider;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.ssl.SSLContextBuilder;

public class RestLockerTransport extends AbstractLockerTransport {

	private CloudStoreRestClient client;

	@Override
	public List<Uid> getVersions() {
		final List<Uid> result = getClient().execute(
				new GetLockerContentVersions(getPgpKeyOrFail().getPgpKeyId(), getLockerContentOrFail().getName()));

		return result;
	}

	@Override
	public void addMergedVersions(List<Uid> serverVersions) {
		assertNotNull("serverVersions", serverVersions);
		throw new UnsupportedOperationException("Merging is only supported on the client side.");
	}

	@Override
	public List<LockerEncryptedDataFile> getEncryptedDataFiles() {
		final List<Uid> versions = getVersions();
		final List<LockerEncryptedDataFile> result = new ArrayList<>(versions.size());

		for (final Uid version : versions) {
			final LockerEncryptedDataFile lockerEncryptedDataFile = getClient().execute(
					new GetLockerEncryptedDataFile(getPgpKeyOrFail().getPgpKeyId(), getLockerContentOrFail().getName(), version));

			if (lockerEncryptedDataFile != null)
				result.add(lockerEncryptedDataFile);
		}
		return result;
	}

	@Override
	public void putEncryptedDataFile(LockerEncryptedDataFile encryptedDataFile) {
		getClient().execute(new PutLockerEncryptedDataFile(encryptedDataFile));
	}

	protected CloudStoreRestClient getClient() {
		if (client == null) {
			final ClientBuilder clientBuilder = createClientBuilder();
			final CloudStoreRestClient c = new CloudStoreRestClient(getUrl(), clientBuilder);
			c.setCredentialsProvider(nullCredentialsProvider);
			client = c;
		}
		return client;
	}

	private ClientBuilder createClientBuilder(){
		final ClientBuilder builder = new ClientBuilderDefaultValuesDecorator();
		try {
			builder.sslContext(SSLContextBuilder.create()
					.remoteURL(getUrl())
					.callback(getDynamicX509TrustManagerCallback()).build());
		} catch (final GeneralSecurityException e) {
			throw new RuntimeException(e);
		}
		return builder;
	}

	protected DynamicX509TrustManagerCallback getDynamicX509TrustManagerCallback() {
		final RestLockerTransportFactory lockerTransportFactory = (RestLockerTransportFactory) getLockerTransportFactory();
		final Class<? extends DynamicX509TrustManagerCallback> klass = lockerTransportFactory.getDynamicX509TrustManagerCallbackClass();
		if (klass == null)
			throw new IllegalStateException("dynamicX509TrustManagerCallbackClass is not set!");

		try {
			final DynamicX509TrustManagerCallback instance = klass.newInstance();
			return instance;
		} catch (final Exception e) {
			throw new RuntimeException(String.format("Could not instantiate class %s: %s", klass.getName(), e.toString()), e);
		}
	}

	private final CredentialsProvider nullCredentialsProvider = new CredentialsProvider() {
		@Override
		public String getUserName() {
			return null;
		}
		@Override
		public String getPassword() {
			return null;
		}
	};
}
