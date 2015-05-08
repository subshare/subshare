package org.subshare.rest.client.pgp.transport;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.Set;

import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.transport.AbstractPgpTransport;
import org.subshare.rest.client.pgp.transport.request.GetLocalRevisionRequest;
import org.subshare.rest.client.pgp.transport.request.GetPgpPublicKeys;
import org.subshare.rest.client.pgp.transport.request.PutPgpPublicKeys;

import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;
import co.codewizards.cloudstore.rest.client.CredentialsProvider;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
import co.codewizards.cloudstore.rest.client.ssl.HostnameVerifierAllowingAll;
import co.codewizards.cloudstore.rest.client.ssl.SSLContextBuilder;

public class RestPgpTransport extends AbstractPgpTransport {

	//	private final URL serverUrl;
	//
	//	public RestPgpTransport(final URL serverUrl) {
	//		this.serverUrl = assertNotNull("serverUrl", serverUrl);
	//	}

	private CloudStoreRestClient client;

	protected DynamicX509TrustManagerCallback getDynamicX509TrustManagerCallback() {
		final RestPgpTransportFactory pgpTransportFactory = (RestPgpTransportFactory) getPgpTransportFactory();
		final Class<? extends DynamicX509TrustManagerCallback> klass = pgpTransportFactory.getDynamicX509TrustManagerCallbackClass();
		if (klass == null)
			throw new IllegalStateException("dynamicX509TrustManagerCallbackClass is not set!");

		try {
			final DynamicX509TrustManagerCallback instance = klass.newInstance();
			return instance;
		} catch (final Exception e) {
			throw new RuntimeException(String.format("Could not instantiate class %s: %s", klass.getName(), e.toString()), e);
		}
	}

	protected CloudStoreRestClient getClient() {
		if (client == null) {
			final CloudStoreRestClient c = new CloudStoreRestClient(getUrl());
			c.setHostnameVerifier(new HostnameVerifierAllowingAll());
			try {
				c.setSslContext(SSLContextBuilder.create()
						.remoteURL(getUrl())
						.callback(getDynamicX509TrustManagerCallback()).build());
			} catch (final GeneralSecurityException e) {
				throw new RuntimeException(e);
			}
			c.setCredentialsProvider(nullCredentialsProvider);
			client = c;
		}
		return client;
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

	@Override
	public long getLocalRevision() {
		final Long localRevision = getClient().execute(new GetLocalRevisionRequest());
		return assertNotNull("localRevision", localRevision);
	}

	@Override
	public Set<PgpKeyId> getMasterKeyIds() {
		throw new UnsupportedOperationException(); // not supported for security and technical reasons
	}

	@Override
	public void exportPublicKeys(Set<PgpKeyId> pgpKeyIds, OutputStream out) {
		final InputStream in = getClient().execute(new GetPgpPublicKeys(pgpKeyIds));
//		final InputStream in = getClient().execute(new GetPgpPublicKeys());
		if (in == null)
			return;

		final byte[] buf = new byte[64 * 1024];
		int bytesRead;
		try {

			while ((bytesRead = in.read(buf)) >= 0)
				out.write(buf, 0, bytesRead);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void importKeys(InputStream in) {
		getClient().execute(new PutPgpPublicKeys(in));
	}
}
