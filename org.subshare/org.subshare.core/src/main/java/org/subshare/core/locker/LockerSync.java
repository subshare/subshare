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
import org.subshare.core.pgp.PgpKeyId;
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

	private final String lastSyncServerVersionsPropertyKey;

	public LockerSync(final Server server) {
		this.server = assertNotNull("server", server);
		this.serverId = assertNotNull("server.serverId", this.server.getServerId());
		this.serverUrl = assertNotNull("server.url", this.server.getUrl());

		lastSyncServerVersionsPropertyKey = String.format("lastSync[serverId=%s].server.version", serverId);
	}

	public void sync() {
		final Set<PgpKeyId> pgpKeyIds = PgpPrivateKeyPassphraseStoreImpl.getInstance().getPgpKeyIdsHavingPassphrase();

		for (final LockerContent lockerContent : getLockerContents()) {
			getLocalLockerTransport().setPgpKeyIds(pgpKeyIds);
			getServerLockerTransport().setPgpKeyIds(pgpKeyIds);
			getLocalLockerTransport().setLockerContent(lockerContent);
			getServerLockerTransport().setLockerContent(lockerContent);

			final List<Uid> localVersions = getLocalLockerTransport().getVersions();
			if (localVersions.size() != 1)
				throw new IllegalStateException("localVersions.size() != 1");

			final Uid localVersion = localVersions.get(0);

			boolean syncUpNeeded = false;

			List<Uid> serverVersions = getServerLockerTransport().getVersions();
			if (serverVersions.size() > 1) {
				syncUpNeeded = true;
				serverVersions = syncDown(serverVersions); // replace serverVersions by the ones actually synced (might have changed in the meantime).
			}
			else if (serverVersions.isEmpty())
				doNothing(); // if there's nothing on the server, it obviously makes no sense to sync down.
			else {
				final Set<Uid> lastSyncServerVersions = new HashSet<>(getLastSyncServerVersions());
				final Set<Uid> serverVersionsSet = new HashSet<>(serverVersions);
				if (!serverVersionsSet.equals(lastSyncServerVersions)) {
					syncUpNeeded = true;
					serverVersions = syncDown(serverVersions); // replace serverVersions by the ones actually synced (might have changed in the meantime).
				}
			}

			if (!syncUpNeeded) {
				if (serverVersions.size() != 1)
					syncUpNeeded = true;
				else
					syncUpNeeded = !localVersion.equals(serverVersions.get(0));
			}

			if (syncUpNeeded)
				syncUp(serverVersions, localVersion);
		}
	}

	/**
	 * Gets the last {@linkplain LockerTransport#getVersions() version} that was synced from/to the server.
	 * @return the last {@linkplain LockerTransport#getVersions() version} that was synced from/to the server. Never <code>null</code>.
	 */
	private List<Uid> getLastSyncServerVersions() {
		final String value = getLockerSyncProperties().getProperty(lastSyncServerVersionsPropertyKey);
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
		getLockerSyncProperties().setProperty(lastSyncServerVersionsPropertyKey, sb.toString());
		writeLockerSyncProperties();
	}

	private List<Uid> syncDown(final List<Uid> serverVersions) {
//		getServerLockerTransport().getEncryptedDataFiles()


		setLastSyncServerVersions(serverVersions);
		throw new UnsupportedOperationException("NYI"); // TODO implement!
	}

	private void syncUp(final List<Uid> serverVersions, final Uid localVersion) {



		setLastSyncServerVersions(Collections.singletonList(localVersion));
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
