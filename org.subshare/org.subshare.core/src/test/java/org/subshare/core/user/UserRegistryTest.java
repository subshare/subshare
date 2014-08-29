package org.subshare.core.user;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.UUID;

import org.subshare.core.gpg.GnuPgTest;
import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.gnupg.GnuPgDir;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.util.IOUtil;

public class UserRegistryTest {

	@BeforeClass
	public static void beforeClass() {
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + GnuPgDir.CONFIG_KEY_GNU_PG_DIR, "build/.gnupg");
	}

	private static class TestUserRegistry extends UserRegistry {

	}

	@Test
	public void readGpgKeys() throws Exception {
		createGnuPgDir();
		PgpRegistry.getInstance().setPgpAuthenticationCallback(new PgpAuthenticationCallback() {
			@Override
			public char[] getPassphrase(final PgpKey pgpKey) {
				return "test12345".toCharArray();
			}
		});

		final UserRegistry userRegistry = new TestUserRegistry();
		final Collection<User> users = userRegistry.getUsers();
		assertThat(users).isNotEmpty();

		User marcoAtCodeWizards = null;
		User marcoAtNightLabs = null;

		for (final User user : users) {
			if (user.getEmails().contains("marco@codewizards.co"))
				marcoAtCodeWizards = user;
			else if (user.getEmails().contains("marco@nightlabs.de"))
				marcoAtNightLabs = user;
		}

		assertThat(marcoAtCodeWizards).isNotNull();
		assertThat(marcoAtNightLabs).isNotNull();

		marcoAtCodeWizards.createUserRepoKey(UUID.randomUUID());
	}

	private void createGnuPgDir() throws IOException {
		GnuPgDir.getInstance().getFile().mkdir();
		try (
				OutputStream out = createFile(GnuPgDir.getInstance().getFile(), GnuPgTest.PUBRING_FILE_NAME).createFileOutputStream();
				InputStream in = GnuPgTest.createPubringInputStream();
				) {
			IOUtil.transferStreamData(in, out);
		}
		try (
				OutputStream out = createFile(GnuPgDir.getInstance().getFile(), GnuPgTest.SECRING_FILE_NAME).createFileOutputStream();
				InputStream in = GnuPgTest.createSecringInputStream();
				) {
			IOUtil.transferStreamData(in, out);
		}
	}

}
