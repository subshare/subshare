package org.subshare.gui.backup;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.backup.BackupDataFile.*;

import java.io.IOException;
import java.io.InputStream;

import org.subshare.core.locker.LockerContent;
import org.subshare.core.server.ServerRegistryLockerContent;

import co.codewizards.cloudstore.core.oio.File;

public class BackupImporter extends AbstractBackupImExporter {

	public BackupImporter() {
	}

	public void importBackup(final File backupFile) throws IOException {
		assertNotNull("backupFile", backupFile);
		backupFile.getParentFile().mkdirs();

		final BackupDataFile backupDataFile;
		try (InputStream in = backupFile.createInputStream();) {
			backupDataFile = new BackupDataFile(in);
		}

		final byte[] pgpKeyData = backupDataFile.getData(ENTRY_NAME_PGP_KEYS);
		assertNotNull("backupDataFile.getData(ENTRY_NAME_PGP_KEYS)", pgpKeyData);
		pgp.importKeys(pgpKeyData);

		final LockerContent serverRegistryLockerContent = localServerClient.invokeConstructor(ServerRegistryLockerContent.class);
		final byte[] serverRegistryData = backupDataFile.getData(ENTRY_NAME_SERVER_REGISTRY_FILE);
		assertNotNull("backupDataFile.getData(ENTRY_NAME_SERVER_REGISTRY_FILE)", serverRegistryData);
		serverRegistryLockerContent.mergeFrom(serverRegistryData);

		registerPgpKeyRelatedBackupProperties(backupDataFile.getManifestTimestamp());
		registerServerRegistryRelatedBackupProperties(backupDataFile.getManifestTimestamp());
		writeBackupProperties();
	}
}
