package org.subshare.gui.backup;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.zip.ZipOutputStream;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.gui.ls.PgpLs;

import co.codewizards.cloudstore.core.oio.File;

public class BackupExporter {
	private final Pgp pgp;
	private final Collection<PgpKey> masterKeysWithPrivateKey;

	public BackupExporter() {
		pgp = PgpLs.getPgpOrFail();
		masterKeysWithPrivateKey = pgp.getMasterKeysWithPrivateKey();
	}

	public void exportBackup(final File backupFile) throws IOException {
		assertNotNull("backupFile", backupFile);
		backupFile.getParentFile().mkdirs();
		try (final OutputStream fout = backupFile.createOutputStream();) {
			try (ZipOutputStream zout = new ZipOutputStream(fout);) {

//				pgp.exportPublicKeysWithPrivateKeys(masterKeysWithPrivateKey, out);
			}
		}
	}

	public boolean isBackupNeeded() {
		return wasPrivateKeyAddedAfterLastBackup() || wasServerAddedAfterLastBackup();
	}

	/**
	 * Is there any private key which was not yet written to a backup?
	 */
	private boolean wasPrivateKeyAddedAfterLastBackup() {
		if (masterKeysWithPrivateKey.isEmpty())
			return false;

		// TODO track in a properties file, which private keys were already backed up!
		return true;
	}

	private boolean wasServerAddedAfterLastBackup() {
		return false;
	}


}
