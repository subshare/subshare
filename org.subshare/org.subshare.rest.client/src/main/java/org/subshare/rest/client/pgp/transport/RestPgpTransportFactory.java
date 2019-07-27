package org.subshare.rest.client.pgp.transport;

import static co.codewizards.cloudstore.rest.client.transport.RestRepoTransportFactory.*;
import static java.util.Objects.*;

import java.net.URL;

import org.subshare.core.pgp.transport.AbstractPgpTransportFactory;
import org.subshare.core.pgp.transport.PgpTransport;

import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;

public class RestPgpTransportFactory extends AbstractPgpTransportFactory {

	private volatile Class<? extends DynamicX509TrustManagerCallback> dynamicX509TrustManagerCallbackClass;

	public Class<? extends DynamicX509TrustManagerCallback> getDynamicX509TrustManagerCallbackClass() {
		return dynamicX509TrustManagerCallbackClass;
	}
	public void setDynamicX509TrustManagerCallbackClass(Class<? extends DynamicX509TrustManagerCallback> dynamicX509TrustManagerCallbackClass) {
		this.dynamicX509TrustManagerCallbackClass = dynamicX509TrustManagerCallbackClass;
	}

	@Override
	public String getName() {
		return "REST";
	}

	@Override
	public String getDescription() {
		return "PGP on a remote server";
	}

	@Override
	public boolean isSupported(final URL url) {
		return PROTOCOL_HTTP.equals(requireNonNull(url, "url").getProtocol())
				|| PROTOCOL_HTTPS.equals(url.getProtocol());
	}

	@Override
	protected PgpTransport _createPgpTransport() {
		return new RestPgpTransport();
	}

}
