package org.subshare.core.locker.transport.local;

import java.net.MalformedURLException;
import java.net.URL;

import org.subshare.core.locker.transport.AbstractLockerTransportFactory;
import org.subshare.core.locker.transport.LockerTransport;

public class LocalLockerTransportFactory extends AbstractLockerTransportFactory {

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
		return "Local locker-content on this computer.";
	}

	@Override
	public boolean isSupported(URL url) {
		return LOCAL_URL_PROTOCOL.equals(url.getProtocol());
	}

	@Override
	protected LockerTransport _createLockerTransport() {
		return new LocalLockerTransport();
	}
}
