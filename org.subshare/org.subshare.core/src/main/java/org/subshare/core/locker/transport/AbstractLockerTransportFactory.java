package org.subshare.core.locker.transport;

import java.net.URL;

import co.codewizards.cloudstore.core.util.AssertUtil;

public abstract class AbstractLockerTransportFactory implements LockerTransportFactory {

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public LockerTransport createLockerTransport(URL remoteRoot) {
		AssertUtil.assertNotNull(remoteRoot, "remoteRoot");
		final LockerTransport transport = _createLockerTransport();
		if (transport == null)
			throw new IllegalStateException(String.format("Implementation error in class %s: _createLockerTransport(...) returned null!", this.getClass().getName()));

		transport.setLockerTransportFactory(this);
		transport.setUrl(remoteRoot);
		return transport;
	}

	protected abstract LockerTransport _createLockerTransport();
}
