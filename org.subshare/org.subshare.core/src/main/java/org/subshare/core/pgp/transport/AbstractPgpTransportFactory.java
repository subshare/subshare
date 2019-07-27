package org.subshare.core.pgp.transport;

import static java.util.Objects.*;

import java.net.URL;

public abstract class AbstractPgpTransportFactory implements PgpTransportFactory {

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public PgpTransport createPgpTransport(URL remoteRoot) {
		requireNonNull(remoteRoot, "remoteRoot");
		final PgpTransport pgpTransport = _createPgpTransport();
		if (pgpTransport == null)
			throw new IllegalStateException(String.format("Implementation error in class %s: _createPgpTransport(...) returned null!", this.getClass().getName()));

		pgpTransport.setPgpTransportFactory(this);
		pgpTransport.setUrl(remoteRoot);
		return pgpTransport;
	}

	protected abstract PgpTransport _createPgpTransport();
}
