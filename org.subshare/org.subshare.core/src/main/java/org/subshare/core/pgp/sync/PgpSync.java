package org.subshare.core.pgp.sync;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.PropertiesUtil.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.transport.PgpTransport;
import org.subshare.core.pgp.transport.PgpTransportFactory;
import org.subshare.core.pgp.transport.PgpTransportFactoryRegistry;
import org.subshare.core.pgp.transport.local.LocalPgpTransportFactory;
import org.subshare.core.server.Server;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;

public class PgpSync implements AutoCloseable {

	private final Uid serverId;
	private final Server server;
	private final URL serverUrl;

	private PgpTransport localPgpTransport;
	private PgpTransport serverPgpTransport;

	private File pgpSyncPropertiesFile;
	private Properties pgpSyncProperties;

	private final String lastSyncLocalLocalRevisionPropertyKey;
	private final String lastSyncServerLocalRevisionPropertyKey;

	public PgpSync(final Server server) {
		this.server = assertNotNull("server", server);
		this.serverId = assertNotNull("server.serverId", this.server.getServerId());
		this.serverUrl = assertNotNull("server.url", this.server.getUrl());

		lastSyncLocalLocalRevisionPropertyKey = String.format("lastSync[serverId=%s].local.localRevision", serverId);
		lastSyncServerLocalRevisionPropertyKey = String.format("lastSync[serverId=%s].server.localRevision", serverId);
	}

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
		if (lastSyncServerLocalRevision != serverLocalRevision) {
			// sync DOWN (from server to local)
			sync(getServerPgpTransport(), lastSyncServerLocalRevision, getLocalPgpTransport());
			getPgpSyncProperties().setProperty(lastSyncServerLocalRevisionPropertyKey, Long.toString(localLocalRevision));
			needWrite = true;
		}

		if (needWrite)
			writePgpSyncProperties();
	}

	public URL getServerUrl() {
		return serverUrl;
	}

	private void sync(final PgpTransport from, final long fromLastSyncLocalRevision, final PgpTransport to) {
		// we always sync all keys that are *locally* known - TODO maybe add a constraint to this later?
		final Set<PgpKeyId> allMasterKeyIds = getLocalPgpTransport().getMasterKeyIds();

		for (Set<PgpKeyId> masterKeyIds : split(allMasterKeyIds, 1000)) {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			from.exportPublicKeys(masterKeyIds, fromLastSyncLocalRevision, out);
			to.importKeys(new ByteArrayInputStream(out.toByteArray()));
		}
	}

	private static <E> List<Set<E>> split(final Set<E> inputSet, final int maxSize) {
		assertNotNull("inputSet", inputSet);
		final List<Set<E>> result = new ArrayList<Set<E>>(inputSet.size() / maxSize + 1);
		Set<E> current = null;
		for (final E element : inputSet) {
			if (current == null || current.size() >= maxSize) {
				current = new HashSet<E>(maxSize);
				result.add(current);
			}
			current.add(element);
		}
		return result;
	}

	private PgpTransport getLocalPgpTransport() {
		if (localPgpTransport == null) {
			final PgpTransportFactoryRegistry pgpTransportFactoryRegistry = PgpTransportFactoryRegistry.getInstance();
			final PgpTransportFactory pgpTransportFactory = pgpTransportFactoryRegistry.getPgpTransportFactoryOrFail(LocalPgpTransportFactory.class);
			localPgpTransport = pgpTransportFactory.createPgpTransport(LocalPgpTransportFactory.LOCAL_URL);
		}
		return localPgpTransport;
	}

	public PgpTransport getServerPgpTransport() {
		if (serverPgpTransport == null) {
			final PgpTransportFactoryRegistry pgpTransportFactoryRegistry = PgpTransportFactoryRegistry.getInstance();
			final PgpTransportFactory pgpTransportFactory = pgpTransportFactoryRegistry.getPgpTransportFactoryOrFail(serverUrl);
			serverPgpTransport = pgpTransportFactory.createPgpTransport(serverUrl);
		}
		return serverPgpTransport;
	}

	private File getPgpSyncPropertiesFile() {
		if (pgpSyncPropertiesFile == null)
			pgpSyncPropertiesFile = createFile(ConfigDir.getInstance().getFile(), "pgpSync.properties");

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
						try (final InputStream in = lockFile.createInputStream();) {
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
				try (final OutputStream out = lockFile.createOutputStream();) { // acquires LockFile.lock implicitly
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
