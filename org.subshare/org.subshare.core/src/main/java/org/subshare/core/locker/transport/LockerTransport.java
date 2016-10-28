package org.subshare.core.locker.transport;

import java.net.URL;
import java.util.List;

import org.subshare.core.locker.LockerContent;
import org.subshare.core.locker.LockerEncryptedDataFile;
import org.subshare.core.pgp.PgpKey;

import co.codewizards.cloudstore.core.Uid;

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

	void addMergedVersions(List<Uid> serverVersions);
	List<LockerEncryptedDataFile> getEncryptedDataFiles();

	void putEncryptedDataFile(LockerEncryptedDataFile encryptedDataFile);

	PgpKey getPgpKey();
	void setPgpKey(PgpKey pgpKey);

	@Override
	void close();
}
