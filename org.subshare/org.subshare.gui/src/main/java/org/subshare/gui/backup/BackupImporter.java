package org.subshare.gui.backup;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.gui.backup.BackupDataFile.*;

import java.io.IOException;
import java.io.InputStream;

import org.subshare.core.locker.LockerContent;
import org.subshare.core.server.ServerRegistryLockerContent;
import org.subshare.gui.ls.UserRegistryLs;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.ls.client.util.ByteArrayInputStreamLs;

public class BackupImporter extends AbstractBackupImExporter {

	public BackupImporter() {
	}

	public void importBackup(final File backupFile) throws IOException {
		assertNotNull(backupFile, "backupFile");
		backupFile.getParentFile().mkdirs();

		final BackupDataFile backupDataFile;
		try (InputStream in = castStream(backupFile.createInputStream())) {
			backupDataFile = new BackupDataFile(in);
		}

		UserRegistryLs.getUserRegistry(); // instantiating it before loading the PGP-keys to prevent it from populating itself from PGP keys (during init).

		final byte[] pgpKeyData = backupDataFile.getData(ENTRY_NAME_PGP_KEYS);
		assertNotNull(pgpKeyData, "backupDataFile.getData(ENTRY_NAME_PGP_KEYS)");
		pgp.importKeys(ByteArrayInputStreamLs.create(pgpKeyData));

		final LockerContent serverRegistryLockerContent = localServerClient.invokeConstructor(ServerRegistryLockerContent.class);
		final byte[] serverRegistryData = backupDataFile.getData(ENTRY_NAME_SERVER_REGISTRY_FILE);
		assertNotNull(serverRegistryData, "backupDataFile.getData(ENTRY_NAME_SERVER_REGISTRY_FILE)");
		serverRegistryLockerContent.mergeFrom(serverRegistryData);

		registerPgpKeyRelatedBackupProperties(backupDataFile.getManifestTimestamp());
		registerServerRegistryRelatedBackupProperties(backupDataFile.getManifestTimestamp());
		writeBackupProperties();
	}
}
