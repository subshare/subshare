package org.subshare.core.locker;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.subshare.core.locker.transport.LockerTransport;
import org.subshare.core.locker.transport.LockerTransportFactory;
import org.subshare.core.locker.transport.LockerTransportFactoryRegistry;
import org.subshare.core.locker.transport.local.LocalLockerTransportFactory;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStoreImpl;
import org.subshare.core.server.Server;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;

public class LockerSync implements AutoCloseable {

	private final Uid serverId;
	private final Server server;
	private final URL serverUrl;

	private LockerTransport localLockerTransport;
	private LockerTransport serverLockerTransport;

	private File lockerSyncPropertiesFile;
	private Properties lockerSyncProperties;

	private PgpKey pgpKey;

	public LockerSync(final Server server) {
		this.server = assertNotNull("server", server);
		this.serverId = assertNotNull("server.serverId", this.server.getServerId());
		this.serverUrl = assertNotNull("server.url", this.server.getUrl());
	}

	public void sync() {
		pgpKey = null;
		final Set<PgpKeyId> pgpKeyIds = PgpPrivateKeyPassphraseStoreImpl.getInstance().getPgpKeyIdsHavingPassphrase();
		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
		for (final LockerContent lockerContent : getLockerContents()) {
			getLocalLockerTransport().setLockerContent(lockerContent);
			getServerLockerTransport().setLockerContent(lockerContent);

			for (final PgpKeyId pgpKeyId : pgpKeyIds) {
				pgpKey = pgp.getPgpKey(pgpKeyId);
				if (pgpKey == null)
					throw new IllegalStateException("PgpKey not found: " + pgpKeyId);

				getLocalLockerTransport().setPgpKey(pgpKey);
				getServerLockerTransport().setPgpKey(pgpKey);

				final List<Uid> localVersions = getLocalLockerTransport().getVersions();
				if (localVersions.size() != 1)
					throw new IllegalStateException("localVersions.size() != 1");

				final Uid localVersion = localVersions.get(0);

				boolean syncUpNeeded = false;

				List<Uid> serverVersions = getServerLockerTransport().getVersions();
				if (serverVersions.size() > 1) {
					syncUpNeeded = true;
					serverVersions = syncDown(); // replace serverVersions by the ones actually synced (might have changed in the meantime).
				}
				else if (serverVersions.isEmpty())
					doNothing(); // if there's nothing on the server, it obviously makes no sense to sync down.
				else {
					final Set<Uid> lastSyncServerVersions = new HashSet<>(getLastSyncServerVersions());
					final Set<Uid> serverVersionsSet = new HashSet<>(serverVersions);
					if (!serverVersionsSet.equals(lastSyncServerVersions)) {
						syncUpNeeded = true;
						serverVersions = syncDown(); // replace serverVersions by the ones actually synced (might have changed in the meantime).
					}
				}

				if (!syncUpNeeded) {
					if (serverVersions.size() != 1)
						syncUpNeeded = true;
					else
						syncUpNeeded = !localVersion.equals(serverVersions.get(0));
				}

				if (syncUpNeeded)
					syncUp();
			}
		}
		pgpKey = null;
	}

	public String getLastSyncServerVersionsPropertyKey() {
		final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
		return String.format("lastSync[serverId=%s,pgpKeyId=%s].server.versions", serverId, pgpKeyId);
	}

	/**
	 * Gets the last {@linkplain LockerTransport#getVersions() versions} that were synced from/to the server.
	 * @return the last {@linkplain LockerTransport#getVersions() versions} that were synced from/to the server.
	 * Never <code>null</code>, but maybe empty.
	 */
	private List<Uid> getLastSyncServerVersions() {
		final String value = getLockerSyncProperties().getProperty(getLastSyncServerVersionsPropertyKey());
		if (isEmpty(value))
			return Collections.emptyList();

		final List<Uid> result = new ArrayList<Uid>();
		final String[] strings = value.split(",");
		for (String string : strings) {
			if (! isEmpty(string))
				result.add(new Uid(string));
		}
		return Collections.unmodifiableList(result);
	}

	private void setLastSyncServerVersions(final List<Uid> serverVersions) {
		assertNotNull("serverVersions", serverVersions);
		StringBuilder sb = new StringBuilder();
		for (Uid serverVersion : serverVersions) {
			if (sb.length() > 0)
				sb.append(',');

			sb.append(serverVersion);
		}
		getLockerSyncProperties().setProperty(getLastSyncServerVersionsPropertyKey(), sb.toString());
		writeLockerSyncProperties();
	}

	private List<Uid> syncDown() {
//		final List<LockerEncryptedDataFile> encryptedDataFiles = getServerLockerTransport().getEncryptedDataFiles();
//		final List<Uid> serverVersions = new ArrayList<Uid>(encryptedDataFiles.size());
//		for (final LockerEncryptedDataFile encryptedDataFile : encryptedDataFiles) {
//			encryptedDataFile.assertManifestSignatureValid();
//			final Uid serverVersion = encryptedDataFile.getContentVersion();
//			assertNotNull("encryptedDataFile.contentVersion", serverVersion);
//			serverVersions.add(serverVersion);
//
//			getLocalLockerTransport().putEncryptedDataFile(encryptedDataFile);
//		}
//
//		setLastSyncServerVersions(serverVersions);
//		return serverVersions;
		return sync(getServerLockerTransport(), getLocalLockerTransport());
	}

	private List<Uid> sync(final LockerTransport fromLockerTransport, final LockerTransport toLockerTransport) {
		final List<LockerEncryptedDataFile> encryptedDataFiles = fromLockerTransport.getEncryptedDataFiles();
		final List<Uid> serverVersions = new ArrayList<Uid>(encryptedDataFiles.size());
		for (final LockerEncryptedDataFile encryptedDataFile : encryptedDataFiles) {
			encryptedDataFile.assertManifestSignatureValid();
			final Uid serverVersion = encryptedDataFile.getContentVersion();
			assertNotNull("encryptedDataFile.contentVersion", serverVersion);
			serverVersions.add(serverVersion);

			toLockerTransport.putEncryptedDataFile(encryptedDataFile);
		}
		setLastSyncServerVersions(serverVersions);
		return serverVersions;
	}

	private void syncUp() {
		sync(getLocalLockerTransport(), getServerLockerTransport());
	}

	private List<LockerContent> getLockerContents() {
		final List<LockerContent> result = new ArrayList<>();
		final Iterator<LockerContent> iterator = ServiceLoader.load(LockerContent.class).iterator();
		while (iterator.hasNext())
			result.add(iterator.next());

		return result;
	}

	private LockerTransport getLocalLockerTransport() {
		if (localLockerTransport == null) {
			final LockerTransportFactoryRegistry lockerTransportFactoryRegistry = LockerTransportFactoryRegistry.getInstance();
			final LockerTransportFactory lockerTransportFactory = lockerTransportFactoryRegistry.getLockerTransportFactoryOrFail(LocalLockerTransportFactory.LOCAL_URL);
			localLockerTransport = lockerTransportFactory.createLockerTransport(LocalLockerTransportFactory.LOCAL_URL);
		}
		return localLockerTransport;
	}

	public LockerTransport getServerLockerTransport() {
		if (serverLockerTransport == null) {
			final LockerTransportFactoryRegistry lockerTransportFactoryRegistry = LockerTransportFactoryRegistry.getInstance();
			final LockerTransportFactory lockerTransportFactory = lockerTransportFactoryRegistry.getLockerTransportFactoryOrFail(serverUrl);
			serverLockerTransport = lockerTransportFactory.createLockerTransport(serverUrl);
		}
		return serverLockerTransport;
	}

	private File getLockerSyncPropertiesFile() {
		if (lockerSyncPropertiesFile == null)
			lockerSyncPropertiesFile = createFile(ConfigDir.getInstance().getFile(), "lockerSync.properties");

		return lockerSyncPropertiesFile;
	}

	private Properties getLockerSyncProperties() {
		if (lockerSyncProperties == null) {
			try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getLockerSyncPropertiesFile(), 30000);) {
				final Lock lock = lockFile.getLock();
				lock.lock();
				try {
					if (lockerSyncProperties == null) {
						final Properties p = new Properties();
						try (final InputStream in = lockFile.createInputStream();) {
							p.load(in);
						}
						lockerSyncProperties = p;
					}
				} finally {
					lock.unlock();
				}
			} catch (final IOException x) {
				throw new RuntimeException(x);
			}
		}
		return lockerSyncProperties;
	}

	private void writeLockerSyncProperties() {
		final Properties lockerSyncProperties = getLockerSyncProperties();
		synchronized (lockerSyncProperties) {
			try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getLockerSyncPropertiesFile(), 30000);) {
				try (final OutputStream out = lockFile.createOutputStream();) { // acquires LockFile.lock implicitly
					lockerSyncProperties.store(out, null);
				}
			} catch (final IOException x) {
				throw new RuntimeException(x);
			}
		}
	}

	@Override
	public void close() {
		final LockerTransport llt = localLockerTransport;
		localLockerTransport = null;
		if (llt != null)
			llt.close();

		final LockerTransport slt = serverLockerTransport;
		serverLockerTransport = null;
		if (slt != null)
			slt.close();
	}
}
