package org.subshare.rest.server;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;

public class LockerDir {

	private static final Logger logger = LoggerFactory.getLogger(LockerDir.class);

	public static final String CONFIG_KEY_LOCKER_DIR = "locker.dir";
	public static final String DEFAULT_LOCKER_DIR = "${subshare.configDir}/locker";

	protected LockerDir() { }

	private static final class Holder {
		public static final LockerDir instance = new LockerDir();
	}

	public static LockerDir getInstance() {
		return Holder.instance;
	}

	public File getFile() {
		final String dirString = ConfigImpl.getInstance().getPropertyAsNonEmptyTrimmedString(CONFIG_KEY_LOCKER_DIR, DEFAULT_LOCKER_DIR);
		logger.debug("getFile: dirString={}", dirString);
		final String resolvedDir = IOUtil.replaceTemplateVariables(dirString, System.getProperties());
		final File result = createFile(resolvedDir).getAbsoluteFile();
		logger.debug("getFile: result={}", result);
		return result;
	}
}
