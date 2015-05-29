package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;

import java.io.IOException;

import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.gnupg.GnuPgDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;

public class PgpTestUtil {

	private static final Logger logger = LoggerFactory.getLogger(PgpTestUtil.class);

	public static final String PUBRING_FILE_NAME = "pubring.gpg";
	public static final String SECRING_FILE_NAME = "secring.gpg";

	private PgpTestUtil() {
	}

	public static void setupPgp(String ownerName, final String passphrase) throws Exception {
		logger.info("setupPgp: ownerName={}", ownerName);
		final String gpgDir = "gpg/" + ownerName;

		final File gnuPgDir = GnuPgDir.getInstance().getFile();
		gnuPgDir.mkdir();
		copyResource(gpgDir + '/' + PUBRING_FILE_NAME, createFile(gnuPgDir, PUBRING_FILE_NAME));
		copyResource(gpgDir + '/' + SECRING_FILE_NAME, createFile(gnuPgDir, SECRING_FILE_NAME));

		final PgpRegistry pgpRegistry = PgpRegistry.getInstance();

		pgpRegistry.setPgpAuthenticationCallback(new PgpAuthenticationCallback() {
			@Override
			public char[] getPassphrase(final PgpKey pgpKey) {
				return passphrase.toCharArray();
			}
		});

		pgpRegistry.clearCache();
	}

	private static void copyResource(final String sourceResName, final File destinationFile) throws IOException {
		logger.info("copyResource: sourceResName='{}' destinationFile='{}'", sourceResName, destinationFile);
		IOUtil.copyResource(PgpTestUtil.class, sourceResName, destinationFile);
	}
}
