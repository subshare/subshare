package org.subshare.core.user;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.subshare.core.gpg.GnuPgTest;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyTrustLevel;
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

	@Test
	public void initUserRegistryFromGpgKeys() throws Exception {
		initPgp();

		final UserRegistry userRegistry = new UserRegistry();
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

	@Test
	public void testGpgKeyTrustLevels() throws Exception {
		initPgp();

		final UserRegistry userRegistry = new UserRegistry();
		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();

		final Map<String, PgpKeyTrustLevel> email2ExpectedPgpKeyTrustLevel = new HashMap<String, PgpKeyTrustLevel>();
		email2ExpectedPgpKeyTrustLevel.put("marco@codewizards.co", PgpKeyTrustLevel.ULTIMATE);
		email2ExpectedPgpKeyTrustLevel.put("alex@nightlabs.de", PgpKeyTrustLevel.TRUSTED);
		email2ExpectedPgpKeyTrustLevel.put("daniel@nightlabs.de", PgpKeyTrustLevel.TRUSTED);
		email2ExpectedPgpKeyTrustLevel.put("janmorti@gmx.de", PgpKeyTrustLevel.NOT_TRUSTED);
		email2ExpectedPgpKeyTrustLevel.put("jonathan@codewizards.co", PgpKeyTrustLevel.NOT_TRUSTED);
		email2ExpectedPgpKeyTrustLevel.put("marc@nightlabs.de", PgpKeyTrustLevel.NOT_TRUSTED);

		for (final Map.Entry<String, PgpKeyTrustLevel> me : email2ExpectedPgpKeyTrustLevel.entrySet()) {
			final String email = me.getKey();
			final PgpKeyTrustLevel expectedPgpKeyTrustLevel = me.getValue();

			final Collection<User> users = userRegistry.getUsersByEmail(email);
			assertThat(users).hasSize(1);
			final User user = users.iterator().next();

			PgpKeyTrustLevel highestKeyTrustLevel = null;
			for (final Long pgpKeyId : user.getPgpKeyIds()) {
				final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
				if (pgpKey != null) {
					final PgpKeyTrustLevel ktl = pgp.getKeyTrustLevel(pgpKey);
					if (highestKeyTrustLevel == null || ktl.compareTo(highestKeyTrustLevel) > 0)
						highestKeyTrustLevel = ktl;
				}
			}
			assertThat(highestKeyTrustLevel).isNotNull();
			assertThat(highestKeyTrustLevel).isEqualTo(expectedPgpKeyTrustLevel);
		}
	}

	private void initPgp() throws IOException {
		GnuPgDir.getInstance().getFile().mkdir();
		try (
				OutputStream out = createFile(GnuPgDir.getInstance().getFile(), GnuPgTest.PUBRING_FILE_NAME).createOutputStream();
				InputStream in = GnuPgTest.createPubringInputStream();
				) {
			IOUtil.transferStreamData(in, out);
		}
		try (
				OutputStream out = createFile(GnuPgDir.getInstance().getFile(), GnuPgTest.SECRING_FILE_NAME).createOutputStream();
				InputStream in = GnuPgTest.createSecringInputStream();
				) {
			IOUtil.transferStreamData(in, out);
		}

		PgpRegistry.getInstance().setPgpAuthenticationCallback(new PgpAuthenticationCallback() {
			@Override
			public char[] getPassphrase(final PgpKey pgpKey) {
				return "test12345".toCharArray();
			}
		});
	}

}