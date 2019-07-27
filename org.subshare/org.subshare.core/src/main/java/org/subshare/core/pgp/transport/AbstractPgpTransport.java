package org.subshare.core.pgp.transport;

import static java.util.Objects.*;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.util.UrlUtil;

public abstract class AbstractPgpTransport implements PgpTransport {
	private static final Logger logger = LoggerFactory.getLogger(AbstractPgpTransport.class);

	private static final String SLASH = "/";

	private PgpTransportFactory pgpTransportFactory;
	private URL url;

	// Don't know, if fillInStackTrace() is necessary, but better do it.
	// I did a small test: 1 million invocations of new Exception() vs. new Exception() with fillInStackTrace(): 3 s vs 2.2 s
	private volatile Throwable pgpTransportCreatedStackTraceException = new Exception("pgpTransportCreatedStackTraceException").fillInStackTrace();

	@Override
	public PgpTransportFactory getPgpTransportFactory() {
		return pgpTransportFactory;
	}

	@Override
	public void setPgpTransportFactory(final PgpTransportFactory pgpTransportFactory) {
		this.pgpTransportFactory = requireNonNull(pgpTransportFactory, "pgpTransportFactory");
	}

	@Override
	public URL getUrl() {
		return url;
	}
	@Override
	public void setUrl(URL url) {
		url = UrlUtil.canonicalizeURL(url);
		final URL rr = this.url;
		if (rr != null && !rr.equals(url))
			throw new IllegalStateException("Cannot re-assign url!");

		this.url = url;
	}

	@Override
	protected void finalize() throws Throwable {
		if (pgpTransportCreatedStackTraceException != null) {
			logger.warn("finalize: Detected forgotten close() invocation!", pgpTransportCreatedStackTraceException);
		}
		super.finalize();
	}

	@Override
	public void close() {
		pgpTransportCreatedStackTraceException = null;
	}

}
