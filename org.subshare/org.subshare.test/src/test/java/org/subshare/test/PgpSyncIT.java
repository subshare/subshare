package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.URL;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.gnupg.BcWithLocalGnuPgPgp;
import org.subshare.core.pgp.gnupg.GnuPgDir;
import org.subshare.core.pgp.sync.PgpSync;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerImpl;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;

@RunWith(JMockit.class)
public class PgpSyncIT extends AbstractIT {

	static File forcedGnuPgDirFile;
	static File clientGnuPgDirFile;
	static File serverGnuPgDirFile;

	static Pgp clientPgp;
	static Pgp serverPgp;

	@BeforeClass
	public static void beforePgpSyncIT() throws Exception {
		clientGnuPgDirFile = createFile("build/" + jvmInstanceId + "/client/.gnupg");
		serverGnuPgDirFile = createFile("build/" + jvmInstanceId + "/server/.gnupg");

		new MockUp<GnuPgDir>() {
			@Mock
			File getFile() {
				if (forcedGnuPgDirFile != null)
					return forcedGnuPgDirFile;

				if (isServerThread())
					return serverGnuPgDirFile;
				else
					return clientGnuPgDirFile;
			}
		};

		forcedGnuPgDirFile = clientGnuPgDirFile;
		clientPgp = new BcWithLocalGnuPgPgp();

		forcedGnuPgDirFile = serverGnuPgDirFile;
		serverPgp = new BcWithLocalGnuPgPgp();

		forcedGnuPgDirFile = null;

		new MockUp<PgpRegistry>() {
			@Mock
			Pgp getPgpOrFail(Invocation invocation) {
				if (isServerThread())
					return serverPgp;
				else
					return clientPgp;
			}
		};

		setupPgp(clientGnuPgDirFile, "marco");
	}

	public static final String PUBRING_FILE_NAME = "pubring.gpg";
	public static final String SECRING_FILE_NAME = "secring.gpg";

	protected static void setupPgp(final File gnuPgDir, String ownerName) throws Exception {
		final String gpgDir = "gpg/" + ownerName;

		gnuPgDir.getParentFile().mkdir();
		gnuPgDir.mkdir();
		copyResource(gpgDir + '/' + PUBRING_FILE_NAME, createFile(gnuPgDir, PUBRING_FILE_NAME));
		copyResource(gpgDir + '/' + SECRING_FILE_NAME, createFile(gnuPgDir, SECRING_FILE_NAME));
	}

	private static void copyResource(final String sourceResName, final File destinationFile) throws IOException {
		IOUtil.copyResource(AbstractIT.class, sourceResName, destinationFile);
	}

	private static boolean isServerThread() {
		final StackTraceElement[] stackTrace = new Exception().getStackTrace();
		for (final StackTraceElement stackTraceElement : stackTrace) {
			if ("org.eclipse.jetty.server.Server".equals(stackTraceElement.getClassName()))
				return true;
		}
		return false;
	}

	@Ignore // TODO remove this and implement the missing functionality!
	@Test
	public void testPgpSync() throws Exception {
		Server server = new ServerImpl();
		server.setUrl(new URL(getSecureUrl()));

		forcedGnuPgDirFile = clientGnuPgDirFile;
		assertThat(clientPgp.getMasterKeys()).isNotEmpty();

		forcedGnuPgDirFile = serverGnuPgDirFile;
		assertThat(serverPgp.getMasterKeys()).isEmpty();

		forcedGnuPgDirFile = null;

		try (PgpSync pgpSync = new PgpSync(server);) {
			pgpSync.sync();
		}
	}
}
