package org.subshare.gui.backup.export;

import co.codewizards.cloudstore.core.oio.File;

public class ExportBackupData {

	private File exportBackupDirectory;

	public ExportBackupData() {
	}

	public File getExportBackupDirectory() {
		return exportBackupDirectory;
	}
	public void setExportBackupDirectory(File exportBackupDirectory) {
		this.exportBackupDirectory = exportBackupDirectory;
	}
}
