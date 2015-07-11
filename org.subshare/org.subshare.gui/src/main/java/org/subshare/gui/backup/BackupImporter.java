package org.subshare.gui.backup;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;

import org.subshare.core.pgp.Pgp;
import org.subshare.gui.ls.PgpLs;

import co.codewizards.cloudstore.core.oio.File;

public class BackupImporter {

	private final Pgp pgp;

	public BackupImporter() {
		pgp = PgpLs.getPgpOrFail();
	}

	public void importBackup(final File backupFile) throws IOException {
		assertNotNull("backupFile", backupFile);
		backupFile.getParentFile().mkdirs();
		try (final OutputStream fout = backupFile.createOutputStream();) {
			try (ZipOutputStream zout = new ZipOutputStream(fout);) {

//				pgp.exportPublicKeysWithPrivateKeys(masterKeysWithPrivateKey, out);
			}
		}
	}

}
