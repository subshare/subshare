package org.subshare.gui.backup;

import static org.subshare.gui.backup.BackupConst.*;

import java.io.IOException;
import java.io.InputStream;

import org.subshare.core.file.DataFile;
import org.subshare.core.server.ServerRegistryImpl;

public class BackupDataFile extends DataFile {
	public static final String MANIFEST_PROPERTY_NAME_TIMESTAMP = "timestamp";
	public static final String ENTRY_NAME_PGP_KEYS = "public+secret_keys.gpg";
	public static final String ENTRY_NAME_SERVER_REGISTRY_FILE = ServerRegistryImpl.SERVER_REGISTRY_FILE_NAME;

	public BackupDataFile(byte[] in) throws IOException {
		super(in);
	}

	public BackupDataFile(InputStream in) throws IOException {
		super(in);
	}

	public BackupDataFile() {
	}

	@Override
	protected String getContentTypeValue() {
		return BACKUP_FILE_CONTENT_TYPE_VALUE;
	}
}
