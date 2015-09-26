package org.subshare.gui.backup;

import org.subshare.core.file.FileConst;

public interface BackupConst extends FileConst {
	String BACKUP_FILE_NAME_PREFIX = "backup_";
	String BACKUP_FILE_NAME_EXTENSION = SUBSHARE_FILE_EXTENSION; // like *all* subshare files - no matter what role => MANIFEST contains content-type!
	String BACKUP_FILE_CONTENT_TYPE_VALUE = "application/vnd.subshare.backup";
}
