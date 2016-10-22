package org.subshare.core.locker.sync;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.locker.LockerContent;
import org.subshare.core.locker.LockerEncryptedDataFile;
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
import org.subshare.core.sync.Sync;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;

public class LockerSync implements Sync {

	private static final Logger logger = LoggerFactory.getLogger(LockerSync.class);

	private final Uid serverId;
	private final Server server;
	private final URL serverUrl;

	private LockerTransport localLockerTransport;
	private LockerTransport serverLockerTransport;

	private File lockerSyncPropertiesFile;
	private Properties lockerSyncProperties;

	private PgpKey pgpKey;
	private LockerContent lockerContent;

	public LockerSync(final Server server) {
		this.server = assertNotNull("server", server); //$NON-NLS-1$
		this.serverId = assertNotNull("server.serverId", this.server.getServerId()); //$NON-NLS-1$
		this.serverUrl = assertNotNull("server.url", this.server.getUrl()); //$NON-NLS-1$
	}

	public Server getServer() {
		return server;
	}
	public Uid getServerId() {
		return serverId;
	}
	public URL getServerUrl() {
		return serverUrl;
	}

	@Override
	public String getName() {
		return Messages.getString("LockerSync.name"); //$NON-NLS-1$
	}

	@Override
	public void sync() {
		logger.info("sync: serverId='{}' serverName='{}'", server.getServerId(), server.getName()); //$NON-NLS-1$

		lockerContent = null;
		pgpKey = null;
		final Set<PgpKeyId> pgpKeyIds = PgpPrivateKeyPassphraseStoreImpl.getInstance().getPgpKeyIdsHavingPassphrase();
		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
		for (final LockerContent lockerContent : getLockerContents()) {
			this.lockerContent = lockerContent;
			getLocalLockerTransport().setLockerContent(lockerContent);
			getServerLockerTransport().setLockerContent(lockerContent);

			for (final PgpKeyId pgpKeyId : pgpKeyIds) {
				pgpKey = pgp.getPgpKey(pgpKeyId);
				if (pgpKey == null)
					throw new IllegalStateException("PgpKey not found: " + pgpKeyId); //$NON-NLS-1$

				getLocalLockerTransport().setPgpKey(pgpKey);
				getServerLockerTransport().setPgpKey(pgpKey);

				final List<Uid> localVersions = getLocalLockerTransport().getVersions();
				if (localVersions.size() != 1)
					throw new IllegalStateException("localVersions.size() != 1"); //$NON-NLS-1$

				final Uid localVersion = localVersions.get(0);
				final Uid lastSyncLocalVersion = getLastSyncLocalVersion();

				boolean syncDownDone = false;
				boolean syncUpNeeded = false;

				List<Uid> serverVersions = getServerLockerTransport().getVersions();
				if (serverVersions.size() > 1) { // multiple server versions must be merged!
					syncUpNeeded = true;
					serverVersions = syncDown(); // replace serverVersions by the ones actually synced (might have changed in the meantime).
					syncDownDone = true;
				}
				else if (serverVersions.isEmpty()) {
					// if there's nothing on the server, it obviously makes no sense to sync down.
					syncUpNeeded = true; // but we definitely need to sync *up*!
				}
				else {
					final Set<Uid> lastSyncServerVersions = new HashSet<>(getLastSyncServerVersions());
					final Set<Uid> serverVersionsSet = new HashSet<>(serverVersions);
					if (!serverVersionsSet.equals(lastSyncServerVersions)) {
//						syncUpNeeded = true; // NO!
						serverVersions = syncDown(); // replace serverVersions by the ones actually synced (might have changed in the meantime).
						syncDownDone = true;
					}

					if (!syncUpNeeded)
						syncUpNeeded = !localVersion.equals(lastSyncLocalVersion); // serverVersions.get(0));
				}

				if (syncUpNeeded) {
					if (! syncDownDone)
						getLocalLockerTransport().addMergedVersions(serverVersions); // if we don't sync down, now, we must register the versions we synced, previously.

					syncUp();
				}
			}
		}
		pgpKey = null;
		lockerContent = null;
	}

	public String getLastSyncServerVersionsPropertyKey() {
		final PgpKeyId pgpKeyId = assertNotNull("pgpKey", pgpKey).getPgpKeyId(); //$NON-NLS-1$
		final String lockerContentName = assertNotNull("lockerContent", lockerContent).getName(); //$NON-NLS-1$
		return String.format("server[%s].pgpKey[%s].lockerContent[%s].lastSyncServerVersions", serverId, pgpKeyId, lockerContentName); //$NON-NLS-1$
	}

	public String getLastSyncLocalVersionPropertyKey() {
		final PgpKeyId pgpKeyId = assertNotNull("pgpKey", pgpKey).getPgpKeyId(); //$NON-NLS-1$
		final String lockerContentName = assertNotNull("lockerContent", lockerContent).getName(); //$NON-NLS-1$
		return String.format("server[%s].pgpKey[%s].lockerContent[%s].lastSyncLocalVersion", serverId, pgpKeyId, lockerContentName); //$NON-NLS-1$
	}

	/**
	 * Gets the last local {@linkplain LockerTransport#getVersions() version} (locally, there is never more than 1) that was last
	 * synced up to the server.
	 */
	private Uid getLastSyncLocalVersion() {
		final String value = getLockerSyncProperties().getProperty(getLastSyncLocalVersionPropertyKey());
		if (isEmpty(value))
			return null;

		final Uid result = new Uid(value);
		return result;
	}

	private void setLastSyncLocalVersion(Uid version) {
		getLockerSyncProperties().setProperty(getLastSyncLocalVersionPropertyKey(), version.toString());
		writeLockerSyncProperties();
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
		final String[] strings = value.split(","); //$NON-NLS-1$
		for (String string : strings) {
			if (! isEmpty(string))
				result.add(new Uid(string));
		}
		return Collections.unmodifiableList(result);
	}

	private void setLastSyncServerVersions(final List<Uid> serverVersions) {
		assertNotNull("serverVersions", serverVersions); //$NON-NLS-1$
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
		logger.info("syncDown: serverId='{}' serverName='{}' pgpKeyId={} lockerContentName='{}'", //$NON-NLS-1$
				server.getServerId(), server.getName(), pgpKey.getPgpKeyId(), lockerContent.getName());

		return sync(getServerLockerTransport(), getLocalLockerTransport());
	}

	private void syncUp() {
		logger.info("syncUp: serverId='{}' serverName='{}' pgpKeyId={} lockerContentName='{}'", //$NON-NLS-1$
				server.getServerId(), server.getName(), pgpKey.getPgpKeyId(), lockerContent.getName());

		// first obtaining the version, so that if it changes in the mean-time, while we sync up, we'd have to sync up again.
		final List<Uid> localVersions = getLocalLockerTransport().getVersions();

		sync(getLocalLockerTransport(), getServerLockerTransport());

		if (localVersions.size() != 1)
			throw new IllegalStateException("localVersions.size() != 1"); //$NON-NLS-1$

		setLastSyncLocalVersion(localVersions.get(0));
	}

	private List<Uid> sync(final LockerTransport fromLockerTransport, final LockerTransport toLockerTransport) {
		final List<LockerEncryptedDataFile> encryptedDataFiles = fromLockerTransport.getEncryptedDataFiles();
		final List<Uid> serverVersions = new ArrayList<Uid>(encryptedDataFiles.size());
		for (final LockerEncryptedDataFile encryptedDataFile : encryptedDataFiles) {
			final Uid serverVersion = encryptedDataFile.getContentVersion();
			assertNotNull("encryptedDataFile.contentVersion", serverVersion); //$NON-NLS-1$
			serverVersions.add(serverVersion);

			toLockerTransport.putEncryptedDataFile(encryptedDataFile);
		}
		setLastSyncServerVersions(serverVersions);
		return serverVersions;
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
			lockerSyncPropertiesFile = createFile(ConfigDir.getInstance().getFile(), "lockerSync.properties"); //$NON-NLS-1$

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
