package org.subshare.gui.backup;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.util.DateUtil.*;
import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static java.util.Objects.*;
import static org.subshare.gui.backup.BackupDataFile.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.subshare.core.locker.LockerContent;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerRegistryLockerContent;

import co.codewizards.cloudstore.core.io.IByteArrayOutputStream;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.ISO8601;
import co.codewizards.cloudstore.ls.client.util.ByteArrayOutputStreamLs;

public class BackupExporter extends AbstractBackupImExporter {

	public BackupExporter() {
	}

	public void exportBackup(final File backupFile) throws IOException {
		requireNonNull(backupFile, "backupFile");
		final Date now = now(); // we want the same timestamp everywhere in the export => freeze it now.
		backupFile.getParentFile().mkdirs();
		final BackupDataFile backupDataFile = new BackupDataFile();
		backupDataFile.getManifestProperties().put(MANIFEST_PROPERTY_NAME_TIMESTAMP, ISO8601.formatDate(now()));

		registerPgpKeyRelatedBackupProperties(now);
		IByteArrayOutputStream bout = ByteArrayOutputStreamLs.create();
		pgp.exportPublicKeysWithSecretKeys(new HashSet<>(pgp.getMasterKeysWithSecretKey()), bout);
		backupDataFile.putData(ENTRY_NAME_PGP_KEYS, bout.toByteArray());

		registerServerRegistryRelatedBackupProperties(now);
		LockerContent serverRegistryLockerContent = localServerClient.invokeConstructor(ServerRegistryLockerContent.class);
		backupDataFile.putData(ENTRY_NAME_SERVER_REGISTRY_FILE, serverRegistryLockerContent.getLocalData());

		try (final OutputStream out = castStream(backupFile.createOutputStream())) {
			backupDataFile.write(out);
		}
		writeBackupProperties();
	}

	public boolean isBackupNeeded() {
		return wasPrivateKeyAddedOrChangedAfterLastBackup() || wasServerAddedAfterLastBackup();
	}

	/**
	 * Is there any private key which was not yet written to a backup?
	 */
	private boolean wasPrivateKeyAddedOrChangedAfterLastBackup() {
		final HashSet<PgpKey> masterKeysWithPrivateKey = new HashSet<>(pgp.getMasterKeysWithSecretKey());
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

}
