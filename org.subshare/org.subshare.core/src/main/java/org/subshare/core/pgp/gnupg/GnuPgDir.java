package org.subshare.core.pgp.gnupg;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;

public class GnuPgDir {

	public static final String CONFIG_KEY_GNU_PG_DIR = "gnupg.dir";

	private File dir;

	private static final class Holder {
		public static final GnuPgDir instance = new GnuPgDir();
	}

	public static GnuPgDir getInstance() {
		return Holder.instance;
	}

	protected GnuPgDir() { }

	public File getFile() {
		if (dir == null) {
			final String dirString = Config.getInstance().getProperty(CONFIG_KEY_GNU_PG_DIR, "${user.home}/.gnupg");
			final String resolvedDir = IOUtil.replaceTemplateVariables(dirString, System.getProperties());
			dir = createFile(resolvedDir).getAbsoluteFile();
		}
		return dir;
	}

}
