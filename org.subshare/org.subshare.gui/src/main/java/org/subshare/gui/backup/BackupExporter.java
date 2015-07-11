package org.subshare.gui.backup;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.gui.backup.BackupDataFile.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.subshare.core.locker.LockerContent;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerRegistry;
import org.subshare.core.server.ServerRegistryLockerContent;
import org.subshare.gui.ls.PgpLs;
import org.subshare.gui.ls.ServerRegistryLs;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.ISO8601;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class BackupExporter {
	private final LocalServerClient localServerClient;
	private final Pgp pgp;
	private final ServerRegistry serverRegistry;
	private final Set<PgpKey> masterKeysWithPrivateKey;

	private final File backupPropertiesFile;
	private final Properties backupProperties = new Properties();
	private Date now;

	public BackupExporter() {
		localServerClient = LocalServerClient.getInstance();
		pgp = PgpLs.getPgpOrFail();
		serverRegistry = ServerRegistryLs.getServerRegistry();
		masterKeysWithPrivateKey = new HashSet<>(pgp.getMasterKeysWithPrivateKey());
		backupPropertiesFile = createFile(ConfigDir.getInstance().getFile(), "backup.properties");
		readBackupProperties();
	}

	private void readBackupProperties() {
		try (LockFile lockFile = LockFileFactory.getInstance().acquire(backupPropertiesFile, 30000);) {
			try (InputStream in = lockFile.createInputStream();) {
				backupProperties.load(in);
			}
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	private void writeBackupProperties() {
		try (LockFile lockFile = LockFileFactory.getInstance().acquire(backupPropertiesFile, 30000);) {
			try (OutputStream out = lockFile.createOutputStream();) {
				backupProperties.store(out, null);
			}
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	public void exportBackup(final File backupFile) throws IOException {
		assertNotNull("backupFile", backupFile);
		now = new Date(); // we want the same timestamp everywhere in the export => freeze it now.
		backupFile.getParentFile().mkdirs();
		final BackupDataFile backupDataFile = new BackupDataFile();
		backupDataFile.getManifestProperties().put(MANIFEST_PROPERTY_NAME_TIMESTAMP, ISO8601.formatDate(new Date()));

		registerPgpKeyRelatedBackupProperties();
		backupDataFile.putData(ENTRY_NAME_PGP_KEYS, pgp.exportPublicKeysWithSecretKeys(masterKeysWithPrivateKey));

		registerServerRegistryRelatedBackupProperties();
		LockerContent serverRegistryLockerContent = localServerClient.invokeConstructor(ServerRegistryLockerContent.class);
		backupDataFile.putData(ENTRY_NAME_SERVER_REGISTRY_FILE, serverRegistryLockerContent.getLocalData());

		try (final OutputStream out = backupFile.createOutputStream();) {
			backupDataFile.write(out);
		}
		writeBackupProperties();
	}

	public boolean isBackupNeeded() {
		return wasPrivateKeyAddedOrChangedAfterLastBackup() || wasServerAddedAfterLastBackup();
	}

	private void registerPgpKeyRelatedBackupProperties() {
		for (final PgpKey masterKey : masterKeysWithPrivateKey) {
			final SortedSet<String> userIds = new TreeSet<String>();
			for (final PgpKey pgpKey : masterKey.getMasterKeyAndSubKeys()) {
				final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
				setPgpKeyLastBackupTimestamp(pgpKeyId, assertNotNull("now", now));
				userIds.addAll(pgpKey.getUserIds());
			}
			final String userIdsString = userIds.toString();
			final String userIdsSha1 = sha1(userIdsString);
			setPgpKeyLastBackupUserIdsSha1(masterKey.getPgpKeyId(), userIdsSha1);
		}
	}

	/**
	 * Is there any private key which was not yet written to a backup?
	 */
	private boolean wasPrivateKeyAddedOrChangedAfterLastBackup() {
		if (masterKeysWithPrivateKey.isEmpty())
			return false;

		for (final PgpKey masterKey : masterKeysWithPrivateKey) {
			final SortedSet<String> userIds = new TreeSet<String>();
			for (final PgpKey pgpKey : masterKey.getMasterKeyAndSubKeys()) {
				final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
				final Date lastBackupTimestamp = getPgpKeyLastBackupTimestamp(pgpKeyId);
				if (lastBackupTimestamp == null)
					return true;

				userIds.addAll(pgpKey.getUserIds());
			}
			final String userIdsString = userIds.toString();
			final String userIdsSha1 = sha1(userIdsString);
			final String lastBackupUserIdsSha1 = getPgpKeyLastBackupUserIdsSha1(masterKey.getPgpKeyId());
			if (! equal(userIdsSha1, lastBackupUserIdsSha1))
				return true;
		}
		return false;
	}

	private void registerServerRegistryRelatedBackupProperties() {
		final SortedSet<String> serverIdAndUrlPairs = new TreeSet<String>();
		for (final Server server : serverRegistry.getServers())
			serverIdAndUrlPairs.add(getServerIdAndUrlPair(server));

		final String serverIdAndUrlPairsString = serverIdAndUrlPairs.toString();
		final String serverIdAndUrlPairsSha1 = sha1(serverIdAndUrlPairsString);
		setServerRegistryLastBackupSha1(serverIdAndUrlPairsSha1);
		setServerRegistryLastBackupTimestamp(assertNotNull("now", now));
	}

	private boolean wasServerAddedAfterLastBackup() {
		if (serverRegistry.getServers().isEmpty())
			return false;

		if (getServerRegistryLastBackupTimestamp() == null)
			return true;

		final SortedSet<String> serverIdAndUrlPairs = new TreeSet<String>();
		for (final Server server : serverRegistry.getServers())
			serverIdAndUrlPairs.add(getServerIdAndUrlPair(server));

		final String serverIdAndUrlPairsString = serverIdAndUrlPairs.toString();
		final String serverIdAndUrlPairsSha1 = sha1(serverIdAndUrlPairsString);
		final String lastBackupSha1 = getServerRegistryLastBackupSha1();
		return ! equal(serverIdAndUrlPairsSha1, lastBackupSha1);
	}

	private static String getServerIdAndUrlPair(final Server server) {
		return server.getServerId().toString() + server.getUrl(); // no separator necessary, because Uid has a fixed width.
	}

	private String getPgpKeyLastBackupTimestampPropertyKey(final PgpKeyId pgpKeyId) {
		assertNotNull("pgpKeyId", pgpKeyId);
		return String.format("pgpKey[%s].lastBackup.timestamp", pgpKeyId);
	}

	private String getPgpKeyLastBackupUserIdsSha1PropertyKey(final PgpKeyId pgpKeyId) {
		assertNotNull("pgpKeyId", pgpKeyId);
		return String.format("pgpKey[%s].lastBackup.userIdsSha1", pgpKeyId);
	}

	private Date getPgpKeyLastBackupTimestamp(final PgpKeyId pgpKeyId) {
		final String propertyKey = getPgpKeyLastBackupTimestampPropertyKey(pgpKeyId);
		final String value = backupProperties.getProperty(propertyKey);

		if (isEmpty(value))
			return null;

		return ISO8601.parseDate(value);
	}

	private void setPgpKeyLastBackupTimestamp(final PgpKeyId pgpKeyId, final Date date) {
		assertNotNull("date", date);
		final String propertyKey = getPgpKeyLastBackupTimestampPropertyKey(pgpKeyId);
		final String value = date == null ? null : ISO8601.formatDate(date);
		backupProperties.setProperty(propertyKey, value);
	}

	private String getPgpKeyLastBackupUserIdsSha1(final PgpKeyId pgpKeyId) {
		final String propertyKey = getPgpKeyLastBackupUserIdsSha1PropertyKey(pgpKeyId);
		final String value = backupProperties.getProperty(propertyKey);
		return emptyToNull(value);
	}

	private void setPgpKeyLastBackupUserIdsSha1(final PgpKeyId pgpKeyId, final String userIdsSha1) {
		assertNotNull("userIdsSha1", userIdsSha1);
		final String propertyKey = getPgpKeyLastBackupUserIdsSha1PropertyKey(pgpKeyId);
		backupProperties.setProperty(propertyKey, userIdsSha1);
	}

	private Date getServerRegistryLastBackupTimestamp() {
		final String value = backupProperties.getProperty("serverRegistry.lastBackup.timestamp");

		if (isEmpty(value))
			return null;

		return ISO8601.parseDate(value);
	}

	private void setServerRegistryLastBackupTimestamp(final Date date) {
		assertNotNull("date", date);
		final String value = date == null ? null : ISO8601.formatDate(date);
		backupProperties.setProperty("serverRegistry.lastBackup.timestamp", value);
	}

	private String getServerRegistryLastBackupSha1() {
		final String value = backupProperties.getProperty("serverRegistry.lastBackup.serverIdsAndUrlsSha1");
		return emptyToNull(value);
	}

	private void setServerRegistryLastBackupSha1(final String sha1) {
		assertNotNull("sha1", sha1);
		backupProperties.setProperty("serverRegistry.lastBackup.serverIdsAndUrlsSha1", sha1);
	}
}
