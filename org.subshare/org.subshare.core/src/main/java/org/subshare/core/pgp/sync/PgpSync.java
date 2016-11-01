package org.subshare.core.pgp.sync;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.CollectionUtil.*;
import static co.codewizards.cloudstore.core.util.PropertiesUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.transport.PgpTransport;
import org.subshare.core.pgp.transport.PgpTransportFactory;
import org.subshare.core.pgp.transport.PgpTransportFactoryRegistry;
import org.subshare.core.pgp.transport.PgpTransportFactoryRegistryImpl;
import org.subshare.core.pgp.transport.local.LocalPgpTransportFactory;
import org.subshare.core.server.Server;
import org.subshare.core.sync.Sync;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistryImpl;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;

public class PgpSync implements Sync {

	private final Uid serverId;
	private final Server server;
	private final URL serverUrl;

	private PgpTransport localPgpTransport;
	private PgpTransport serverPgpTransport;

	private File pgpSyncPropertiesFile;
	private Properties pgpSyncProperties;

	private final String lastSyncLocalLocalRevisionPropertyKey;
	private final String lastSyncServerLocalRevisionPropertyKey;

	private static volatile Set<PgpKeyId> downSyncPgpKeyIds = Collections.emptySet(); // TODO this is a very dirty work-around! refactor and make this clean!

	public static Set<PgpKeyId> getDownSyncPgpKeyIds() {
		return downSyncPgpKeyIds;
	}
	public static void setDownSyncPgpKeyIds(final Set<PgpKeyId> downSyncPgpKeyIds) {
		PgpSync.downSyncPgpKeyIds = downSyncPgpKeyIds == null ? Collections.<PgpKeyId>emptySet() : Collections.unmodifiableSet(new HashSet<>(downSyncPgpKeyIds));
	}

	public PgpSync(final Server server) {
		this.server = assertNotNull("server", server); //$NON-NLS-1$
		this.serverId = assertNotNull("server.serverId", this.server.getServerId()); //$NON-NLS-1$
		this.serverUrl = assertNotNull("server.url", this.server.getUrl()); //$NON-NLS-1$

		lastSyncLocalLocalRevisionPropertyKey = String.format("lastSync[serverId=%s].local.localRevision", serverId); //$NON-NLS-1$
		lastSyncServerLocalRevisionPropertyKey = String.format("lastSync[serverId=%s].server.localRevision", serverId); //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return Messages.getString("PgpSync.name"); //$NON-NLS-1$
	}

	@Override
	public void sync() {
		final long localLocalRevision = getLocalPgpTransport().getLocalRevision();
		final long lastSyncLocalLocalRevision = getPropertyValueAsLong(getPgpSyncProperties(), lastSyncLocalLocalRevisionPropertyKey, -1);

		boolean needWrite = false;
		if (lastSyncLocalLocalRevision != localLocalRevision) { // detected local change => sync to server
			// sync UP (from local to server)
			sync(getLocalPgpTransport(), lastSyncLocalLocalRevision, getServerPgpTransport());
			getPgpSyncProperties().setProperty(lastSyncLocalLocalRevisionPropertyKey, Long.toString(localLocalRevision));
			needWrite = true;
		}

		final long serverLocalRevision = getServerPgpTransport().getLocalRevision();
		final long lastSyncServerLocalRevision = getPropertyValueAsLong(getPgpSyncProperties(), lastSyncServerLocalRevisionPropertyKey, -1);
		if (lastSyncServerLocalRevision != serverLocalRevision || isKeysMissing()) {
			// sync DOWN (from server to local)
			sync(getServerPgpTransport(), lastSyncServerLocalRevision, getLocalPgpTransport());
			getPgpSyncProperties().setProperty(lastSyncServerLocalRevisionPropertyKey, Long.toString(localLocalRevision));
			needWrite = true;
		}

		if (needWrite)
			writePgpSyncProperties();
	}

	private boolean isKeysMissing() {
		if (! getDownSyncPgpKeyIds().isEmpty())
			return true;

		final Set<PgpKeyId> knownMasterKeyIds = getLocalPgpTransport().getMasterKeyIds();
		final Set<PgpKeyId> missingMasterKeyIds = new HashSet<PgpKeyId>();
		for (final User user : UserRegistryImpl.getInstance().getUsers()) {
			for (final PgpKeyId pgpKeyId : user.getPgpKeyIds()) {
				if (! knownMasterKeyIds.contains(pgpKeyId))
					missingMasterKeyIds.add(pgpKeyId);
			}
		}
		return ! missingMasterKeyIds.isEmpty();
	}

	public URL getServerUrl() {
		return serverUrl;
	}

	private void sync(final PgpTransport from, final long fromLastSyncLocalRevision, final PgpTransport to) {
		final Set<PgpKeyId> downSyncPgpKeyIds = getDownSyncPgpKeyIds();

		// We always sync all keys that are *locally* known - TODO maybe add a constraint to this later?
		// From these keys, only the ones having changed after the last sync are up/downloaded.
		final Set<PgpKeyId> knownMasterKeyIds = getLocalPgpTransport().getMasterKeyIds();

		for (Set<PgpKeyId> masterKeyIds : splitSet(knownMasterKeyIds, 1000)) {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			from.exportPublicKeys(masterKeyIds, fromLastSyncLocalRevision, out);
			to.importKeys(new ByteArrayInputStream(out.toByteArray()));
		}

		// Additionally, we sync *all keys *down* that are referenced by our Users and not yet in our local key ring.
		// These are synced without a limit on the lastSyncLocalRevision.
		if (to == getLocalPgpTransport()) { // DOWN!
			// Especially after a restore, when our key-ring is empty (except for our own keys), we must down-sync
			// these keys!
			final Set<PgpKeyId> missingMasterKeyIds = new HashSet<PgpKeyId>();
			for (final User user : UserRegistryImpl.getInstance().getUsers()) {
				for (final PgpKeyId pgpKeyId : user.getPgpKeyIds()) {
					if (! knownMasterKeyIds.contains(pgpKeyId))
						missingMasterKeyIds.add(pgpKeyId);
				}
			}
			missingMasterKeyIds.addAll(downSyncPgpKeyIds);

			if (! missingMasterKeyIds.isEmpty()) {
				for (Set<PgpKeyId> masterKeyIds : splitSet(missingMasterKeyIds, 1000)) {
					final ByteArrayOutputStream out = new ByteArrayOutputStream();
					from.exportPublicKeys(masterKeyIds, -1, out);
					to.importKeys(new ByteArrayInputStream(out.toByteArray()));
				}
				// TODO those keys that are neither locally known nor by any server should be removed ;-)
			}
		}
	}

	private PgpTransport getLocalPgpTransport() {
		if (localPgpTransport == null) {
			final PgpTransportFactoryRegistry pgpTransportFactoryRegistry = PgpTransportFactoryRegistryImpl.getInstance();
			final PgpTransportFactory pgpTransportFactory = pgpTransportFactoryRegistry.getPgpTransportFactoryOrFail(LocalPgpTransportFactory.LOCAL_URL);
			localPgpTransport = pgpTransportFactory.createPgpTransport(LocalPgpTransportFactory.LOCAL_URL);
		}
		return localPgpTransport;
	}

	public PgpTransport getServerPgpTransport() {
		if (serverPgpTransport == null) {
			final PgpTransportFactoryRegistry pgpTransportFactoryRegistry = PgpTransportFactoryRegistryImpl.getInstance();
			final PgpTransportFactory pgpTransportFactory = pgpTransportFactoryRegistry.getPgpTransportFactoryOrFail(serverUrl);
			serverPgpTransport = pgpTransportFactory.createPgpTransport(serverUrl);
		}
		return serverPgpTransport;
	}

	private File getPgpSyncPropertiesFile() {
		if (pgpSyncPropertiesFile == null)
			pgpSyncPropertiesFile = createFile(ConfigDir.getInstance().getFile(), "pgpSync.properties"); //$NON-NLS-1$

		return pgpSyncPropertiesFile;
	}

	private Properties getPgpSyncProperties() {
		if (pgpSyncProperties == null) {
			try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getPgpSyncPropertiesFile(), 30000);) {
				final Lock lock = lockFile.getLock();
				lock.lock();
				try {
					if (pgpSyncProperties == null) {
						final Properties p = new Properties();
						try (final InputStream in = castStream(lockFile.createInputStream())) {
							p.load(in);
						}
						pgpSyncProperties = p;
					}
				} finally {
					lock.unlock();
				}
			} catch (final IOException x) {
				throw new RuntimeException(x);
			}
		}
		return pgpSyncProperties;
	}

	private void writePgpSyncProperties() {
		final Properties pgpSyncProperties = getPgpSyncProperties();
		synchronized (pgpSyncProperties) {
			try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getPgpSyncPropertiesFile(), 30000);) {
				try (final OutputStream out = castStream(lockFile.createOutputStream())) { // acquires LockFile.lock implicitly
					pgpSyncProperties.store(out, null);
				}
			} catch (final IOException x) {
				throw new RuntimeException(x);
			}
		}
	}

	@Override
	public void close() {
		final PgpTransport lpt = localPgpTransport;
		localPgpTransport = null;
		if (lpt != null)
			lpt.close();

		final PgpTransport spt = serverPgpTransport;
		serverPgpTransport = null;
		if (spt != null)
			spt.close();
	}
}
