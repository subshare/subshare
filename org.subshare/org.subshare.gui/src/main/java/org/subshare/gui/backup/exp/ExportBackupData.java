package org.subshare.gui.backup.exp;

import co.codewizards.cloudstore.core.oio.File;

public class ExportBackupData {

	private File exportBackupDirectory;

	public File getExportBackupDirectory() {
		return exportBackupDirectory;
	}
	public void setExportBackupDirectory(File exportBackupDirectory) {
		this.exportBackupDirectory = exportBackupDirectory;
	}
}
