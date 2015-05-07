package org.subshare.core.pgp.transport.local;

import java.net.MalformedURLException;
import java.net.URL;

import org.subshare.core.pgp.transport.AbstractPgpTransportFactory;
import org.subshare.core.pgp.transport.PgpTransport;

public class LocalPgpTransportFactory extends AbstractPgpTransportFactory {

	public static final String LOCAL_URL_PROTOCOL = "file"; // we cannot use sth. like "local" without creating a StreamHandler (otherwise the URL constructor throws an exception).

	public static final URL LOCAL_URL;
	static {
		try {
			LOCAL_URL = new URL(LOCAL_URL_PROTOCOL, null, "");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getName() {
		return "Local";
	}

	@Override
	public String getDescription() {
		return "Local PGP on this computer.";
	}

	@Override
	public boolean isSupported(URL url) {
		return LOCAL_URL_PROTOCOL.equals(url.getProtocol());
	}

	@Override
	protected PgpTransport _createPgpTransport() {
		return new LocalPgpTransport();
	}
}
