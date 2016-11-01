package org.subshare.rest.client.pgp.transport;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Set;

import javax.ws.rs.client.ClientBuilder;

import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.transport.AbstractPgpTransport;
import org.subshare.rest.client.pgp.transport.request.GetLocalRevisionRequest;
import org.subshare.rest.client.pgp.transport.request.GetPgpPublicKeys;
import org.subshare.rest.client.pgp.transport.request.GetPgpPublicKeysMatchingQuery;
import org.subshare.rest.client.pgp.transport.request.PutPgpPublicKeys;

import co.codewizards.cloudstore.core.io.IInputStream;
import co.codewizards.cloudstore.core.io.IOutputStream;
import co.codewizards.cloudstore.core.util.IOUtil;
import co.codewizards.cloudstore.rest.client.ClientBuilderDefaultValuesDecorator;
import co.codewizards.cloudstore.rest.client.CloudStoreRestClient;
import co.codewizards.cloudstore.rest.client.CredentialsProvider;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;
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
	public void exportPublicKeys(final Set<PgpKeyId> pgpKeyIds, final long changedAfterLocalRevision, final IOutputStream out) {
		final InputStream in = getClient().execute(new GetPgpPublicKeys(pgpKeyIds, changedAfterLocalRevision));
		if (in == null)
			return;

		try {
			try {
				IOUtil.transferStreamData(in, castStream(out));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void exportPublicKeysMatchingQuery(final String queryString, final IOutputStream out) {
		final InputStream in = getClient().execute(new GetPgpPublicKeysMatchingQuery(queryString));
		if (in == null)
			return;

		try {
			try {
				IOUtil.transferStreamData(in, castStream(out));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void importKeys(IInputStream in) {
		getClient().execute(new PutPgpPublicKeys(castStream(in)));
	}
}
