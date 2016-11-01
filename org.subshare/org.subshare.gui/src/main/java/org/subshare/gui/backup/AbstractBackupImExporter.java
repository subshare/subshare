package org.subshare.gui.backup;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerRegistry;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.ls.ServerRegistryLs;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.ISO8601;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class AbstractBackupImExporter {

	protected final LocalServerClient localServerClient;
	protected final Pgp pgp;
	protected final ServerRegistry serverRegistry;
	protected final File backupPropertiesFile;
	protected final Properties backupProperties = new Properties();

	public AbstractBackupImExporter() {
		localServerClient = LocalServerClient.getInstance();
		pgp = PgpLs.getPgpOrFail();
		serverRegistry = ServerRegistryLs.getServerRegistry();
		backupPropertiesFile = createFile(ConfigDir.getInstance().getFile(), "backup.properties");
		readBackupProperties();
	}

	private void readBackupProperties() {
		try (LockFile lockFile = LockFileFactory.getInstance().acquire(backupPropertiesFile, 30000);) {
			try (InputStream in = castStream(lockFile.createInputStream())) {
				backupProperties.load(in);
			}
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	protected void writeBackupProperties() {
		try (LockFile lockFile = LockFileFactory.getInstance().acquire(backupPropertiesFile, 30000);) {
			try (OutputStream out = castStream(lockFile.createOutputStream())) {
				backupProperties.store(out, null);
			}
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	protected void registerPgpKeyRelatedBackupProperties(final Date backupTimestamp) {
		assertNotNull("backupTimestamp", backupTimestamp);
		for (final PgpKey masterKey : new HashSet<>(pgp.getMasterKeysWithSecretKey())) {
			final SortedSet<String> userIds = new TreeSet<String>();
			for (final PgpKey pgpKey : masterKey.getMasterKeyAndSubKeys()) {
				final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
				setPgpKeyLastBackupTimestamp(pgpKeyId, backupTimestamp);
				userIds.addAll(pgpKey.getUserIds());
			}
			final String userIdsString = userIds.toString();
			final String userIdsSha1 = sha1(userIdsString);
			setPgpKeyLastBackupUserIdsSha1(masterKey.getPgpKeyId(), userIdsSha1);
		}
	}

	protected void registerServerRegistryRelatedBackupProperties(final Date backupTimestamp) {
		assertNotNull("backupTimestamp", backupTimestamp);
		final SortedSet<String> serverIdAndUrlPairs = new TreeSet<String>();
		for (final Server server : serverRegistry.getServers())
			serverIdAndUrlPairs.add(getServerIdAndUrlPair(server));

		final String serverIdAndUrlPairsString = serverIdAndUrlPairs.toString();
		final String serverIdAndUrlPairsSha1 = sha1(serverIdAndUrlPairsString);
		setServerRegistryLastBackupSha1(serverIdAndUrlPairsSha1);
		setServerRegistryLastBackupTimestamp(assertNotNull("now", backupTimestamp));
	}

	protected static String getServerIdAndUrlPair(final Server server) {
		return server.getServerId().toString() + server.getUrl(); // no separator necessary, because Uid has a fixed width.
	}

	protected String getPgpKeyLastBackupTimestampPropertyKey(final PgpKeyId pgpKeyId) {
		assertNotNull("pgpKeyId", pgpKeyId);
		return String.format("pgpKey[%s].lastBackup.timestamp", pgpKeyId);
	}

	protected String getPgpKeyLastBackupUserIdsSha1PropertyKey(final PgpKeyId pgpKeyId) {
		assertNotNull("pgpKeyId", pgpKeyId);
		return String.format("pgpKey[%s].lastBackup.userIdsSha1", pgpKeyId);
	}

	protected Date getPgpKeyLastBackupTimestamp(final PgpKeyId pgpKeyId) {
		final String propertyKey = getPgpKeyLastBackupTimestampPropertyKey(pgpKeyId);
		final String value = backupProperties.getProperty(propertyKey);

		if (isEmpty(value))
			return null;

		return ISO8601.parseDate(value);
	}

	protected void setPgpKeyLastBackupTimestamp(final PgpKeyId pgpKeyId, final Date date) {
		assertNotNull("date", date);
		final String propertyKey = getPgpKeyLastBackupTimestampPropertyKey(pgpKeyId);
		final String value = date == null ? null : ISO8601.formatDate(date);
		backupProperties.setProperty(propertyKey, value);
	}

	protected String getPgpKeyLastBackupUserIdsSha1(final PgpKeyId pgpKeyId) {
		final String propertyKey = getPgpKeyLastBackupUserIdsSha1PropertyKey(pgpKeyId);
		final String value = backupProperties.getProperty(propertyKey);
		return emptyToNull(value);
	}

	protected void setPgpKeyLastBackupUserIdsSha1(final PgpKeyId pgpKeyId, final String userIdsSha1) {
		assertNotNull("userIdsSha1", userIdsSha1);
		final String propertyKey = getPgpKeyLastBackupUserIdsSha1PropertyKey(pgpKeyId);
		backupProperties.setProperty(propertyKey, userIdsSha1);
	}

	protected Date getServerRegistryLastBackupTimestamp() {
		final String value = backupProperties.getProperty("serverRegistry.lastBackup.timestamp");

		if (isEmpty(value))
			return null;

		return ISO8601.parseDate(value);
	}

	protected void setServerRegistryLastBackupTimestamp(final Date date) {
		assertNotNull("date", date);
		final String value = date == null ? null : ISO8601.formatDate(date);
		backupProperties.setProperty("serverRegistry.lastBackup.timestamp", value);
	}

	protected String getServerRegistryLastBackupSha1() {
		final String value = backupProperties.getProperty("serverRegistry.lastBackup.serverIdsAndUrlsSha1");
		return emptyToNull(value);
	}

	protected void setServerRegistryLastBackupSha1(final String sha1) {
		assertNotNull("sha1", sha1);
		backupProperties.setProperty("serverRegistry.lastBackup.serverIdsAndUrlsSha1", sha1);
	}
}
