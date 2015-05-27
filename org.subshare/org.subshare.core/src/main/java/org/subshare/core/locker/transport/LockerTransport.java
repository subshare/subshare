package org.subshare.core.locker.transport;

import java.net.URL;
import java.util.List;
import java.util.Set;

import org.subshare.core.file.EncryptedDataFile;
import org.subshare.core.locker.LockerContent;
import org.subshare.core.pgp.PgpKeyId;

import co.codewizards.cloudstore.core.dto.Uid;

public interface LockerTransport extends AutoCloseable {

	/**
	 * Gets the factory which created this instance.
	 * @return the factory which created this instance. Should never be <code>null</code>, if properly initialised.
	 * @see #setLockerTransportFactory(LockerTransportFactory)
	 */
	LockerTransportFactory getLockerTransportFactory();
	/**
	 * Sets the factory which created this instance.
	 * @param factory the factory which created this instance. Must not be <code>null</code>.
	 * @see #getLockerTransportFactory()
	 */
	void setLockerTransportFactory(LockerTransportFactory factory);

	LockerContent getLockerContent();

	void setLockerContent(LockerContent lockerContent);

	URL getUrl();

	void setUrl(URL remoteUrl);

	List<Uid> getVersions();

	List<EncryptedDataFile> getEncryptedDataFiles();

	void setPgpKeyIds(Set<PgpKeyId> pgpKeyIds);
	Set<PgpKeyId> getPgpKeyIds();

	@Override
	void close();
}
