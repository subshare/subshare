package org.subshare.core.locker.transport;

import static java.util.Objects.*;

import java.net.URL;

public abstract class AbstractLockerTransportFactory implements LockerTransportFactory {

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public LockerTransport createLockerTransport(URL remoteRoot) {
		requireNonNull(remoteRoot, "remoteRoot");
		final LockerTransport transport = _createLockerTransport();
		if (transport == null)
			throw new IllegalStateException(String.format("Implementation error in class %s: _createLockerTransport(...) returned null!", this.getClass().getName()));

		transport.setLockerTransportFactory(this);
		transport.setUrl(remoteRoot);
		return transport;
	}

	protected abstract LockerTransport _createLockerTransport();
}
