package org.subshare.gui.backup;

import org.subshare.core.file.FileConst;

public interface BackupConst extends FileConst {
	public static final String BACKUP_FILE_NAME_PREFIX = "backup_";
	public static final String BACKUP_FILE_NAME_EXTENSION = SUBSHARE_FILE_EXTENSION; // like *all* CSX files - no matter what role => MANIFEST contains content-type!
}
