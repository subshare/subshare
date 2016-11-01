package org.subshare.core.pgp.transport;

import java.net.URL;
import java.util.Set;

import org.subshare.core.pgp.PgpKeyId;

import co.codewizards.cloudstore.core.io.IInputStream;
import co.codewizards.cloudstore.core.io.IOutputStream;

public interface PgpTransport extends AutoCloseable {

	/**
	 * Gets the factory which created this instance.
	 * @return the factory which created this instance. Should never be <code>null</code>, if properly initialised.
	 * @see #setPgpTransportFactory(PgpTransportFactory)
	 */
	PgpTransportFactory getPgpTransportFactory();
	/**
	 * Sets the factory which created this instance.
	 * @param factory the factory which created this instance. Must not be <code>null</code>.
	 * @see #getPgpTransportFactory()
	 */
	void setPgpTransportFactory(PgpTransportFactory factory);

	URL getUrl();

	void setUrl(URL remoteUrl);

	@Override
	void close();

	long getLocalRevision();

	Set<PgpKeyId> getMasterKeyIds();

	void exportPublicKeys(Set<PgpKeyId> pgpKeyIds, long changedAfterLocalRevision, IOutputStream out);
	void exportPublicKeysMatchingQuery(String queryString, IOutputStream out);

	void importKeys(IInputStream in);
}
