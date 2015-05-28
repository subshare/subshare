package org.subshare.core.locker.transport;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.net.URL;

import org.subshare.core.locker.LockerContent;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.util.AssertUtil;
import co.codewizards.cloudstore.core.util.UrlUtil;

public abstract class AbstractLockerTransport implements LockerTransport {
	private static final Logger logger = LoggerFactory.getLogger(AbstractLockerTransport.class);

	private static final String SLASH = "/";

	private LockerTransportFactory lockerTransportFactory;
	private URL url;
	private LockerContent lockerContent;
	private PgpKey pgpKey;

	// Don't know, if fillInStackTrace() is necessary, but better do it.
	// I did a small test: 1 million invocations of new Exception() vs. new Exception() with fillInStackTrace(): 3 s vs 2.2 s
	private volatile Throwable lockerTransportCreatedStackTraceException = new Exception("lockerTransportCreatedStackTraceException").fillInStackTrace();

	@Override
	public LockerTransportFactory getLockerTransportFactory() {
		return lockerTransportFactory;
	}

	@Override
	public void setLockerTransportFactory(final LockerTransportFactory lockerTransportFactory) {
		this.lockerTransportFactory = AssertUtil.assertNotNull("lockerTransportFactory", lockerTransportFactory);
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
	public LockerContent getLockerContent() {
		return lockerContent;
	}
	protected LockerContent getLockerContentOrFail() {
		final LockerContent lockerContent = getLockerContent();
		assertNotNull("lockerContent", lockerContent);
		return lockerContent;
	}
	@Override
	public void setLockerContent(LockerContent lockerContent) {
		this.lockerContent = lockerContent;
	}

	protected Pgp getPgp() {
		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
		return pgp;
	}

	@Override
	public PgpKey getPgpKey() {
		return pgpKey;
	}
	protected PgpKey getPgpKeyOrFail() {
		final PgpKey pgpKey = getPgpKey();
		assertNotNull("pgpKey", pgpKey);
		return pgpKey;
	}
	@Override
	public void setPgpKey(PgpKey pgpKey) {
		this.pgpKey = pgpKey;
	}

	@Override
	protected void finalize() throws Throwable {
		if (lockerTransportCreatedStackTraceException != null) {
			logger.warn("finalize: Detected forgotten close() invocation!", lockerTransportCreatedStackTraceException);
		}
		super.finalize();
	}

	@Override
	public void close() {
		lockerTransportCreatedStackTraceException = null;
	}

}
