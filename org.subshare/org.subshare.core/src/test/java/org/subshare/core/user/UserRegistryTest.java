package org.subshare.core.user;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.subshare.core.gpg.GnuPgTest;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpKeyValidity;
import org.subshare.core.pgp.PgpOwnerTrust;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.gnupg.GnuPgDir;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;

public class UserRegistryTest {

	@BeforeClass
	public static void beforeClass() {
		final Uid jvmInstanceId = new Uid(); // for parallel test execution ;-)
		System.setProperty(ConfigDir.SYSTEM_PROPERTY_CONFIG_DIR, "build/" + jvmInstanceId + "/.cloudstore");
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + GnuPgDir.CONFIG_KEY_GNU_PG_DIR, "build/" + jvmInstanceId + "/.gnupg");
		createFile("build/" + jvmInstanceId).mkdir();
	}

	@Before
	public void before() throws Exception {
		createFile(ConfigDir.getInstance().getFile(), UserRegistry.USER_REGISTRY_FILE_NAME).delete();
		initPgp();
	}

	@After
	public void after() throws Exception {
		PgpRegistry.getInstance().clearCache();
	}

	@Test
	public void initUserRegistryFromGpgKeys() throws Exception {
		final UserRegistry userRegistry = new UserRegistryImpl();
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
		final UserRegistry userRegistry = new UserRegistryImpl();
		final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();

		final Map<String, PgpKeyValidity> email2ExpectedPgpKeyTrustLevel = new HashMap<String, PgpKeyValidity>();
		email2ExpectedPgpKeyTrustLevel.put("marco@codewizards.co", PgpKeyValidity.ULTIMATE);
		email2ExpectedPgpKeyTrustLevel.put("alex@nightlabs.de", PgpKeyValidity.FULL);
		email2ExpectedPgpKeyTrustLevel.put("daniel@nightlabs.de", PgpKeyValidity.FULL);
		email2ExpectedPgpKeyTrustLevel.put("janmorti@gmx.de", PgpKeyValidity.EXPIRED); // PgpKeyValidity.NOT_TRUSTED);
		email2ExpectedPgpKeyTrustLevel.put("jonathan@codewizards.co", PgpKeyValidity.FULL); // PgpKeyValidity.NOT_TRUSTED);
		email2ExpectedPgpKeyTrustLevel.put("marc@nightlabs.de", PgpKeyValidity.EXPIRED);

		User marco = userRegistry.getUsersByEmail("marco@codewizards.co").iterator().next();
		pgp.setOwnerTrust(marco.getPgpKeys().iterator().next(), PgpOwnerTrust.ULTIMATE);
		pgp.updateTrustDb();

		for (final Map.Entry<String, PgpKeyValidity> me : email2ExpectedPgpKeyTrustLevel.entrySet()) {
			final String email = me.getKey();
			final PgpKeyValidity expectedPgpKeyTrustLevel = me.getValue();

			final Collection<User> users = userRegistry.getUsersByEmail(email);
			assertThat(users).hasSize(1);
			final User user = users.iterator().next();

			PgpKeyValidity highestKeyTrustLevel = null;
			for (final PgpKeyId pgpKeyId : user.getPgpKeyIds()) {
				final PgpKey pgpKey = pgp.getPgpKey(pgpKeyId);
				if (pgpKey != null) {
					final PgpKeyValidity ktl = pgp.getKeyValidity(pgpKey);
					if (highestKeyTrustLevel == null || ktl.compareTo(highestKeyTrustLevel) > 0)
						highestKeyTrustLevel = ktl;
				}
			}
			assertThat(highestKeyTrustLevel).isNotNull();
			assertThat(highestKeyTrustLevel).isEqualTo(expectedPgpKeyTrustLevel);
		}
	}

	@Test
	public void addUser() throws Exception {
		final UserRegistry userRegistry1 = new UserRegistryImpl();

		User user1 = userRegistry1.createUser();
		user1.setUserId(new Uid());
		user1.setFirstName("Anton");
		user1.setLastName("Müller");
		String email1 = "anton.mueller@test.org";
		user1.getEmails().add(email1);

		userRegistry1.addUser(user1);

		Collection<User> users1 = userRegistry1.getUsersByEmail(email1);
		assertThat(users1).containsExactly(user1);

		userRegistry1.writeIfNeeded();

		final UserRegistry userRegistry2 = new UserRegistryImpl();

		Collection<User> users2 = userRegistry2.getUsersByEmail(email1);
		assertThat(users2).hasSize(1);
		final User user2 = users2.iterator().next();
		assertThat(user2).isEqualToIgnoringGivenFields(user1, "userRepoKeyRingChangeListener", "beanSupport");
	}

	private void initPgp() throws IOException {
		PgpRegistry.getInstance().clearCache();

		final File gnuPgDir = GnuPgDir.getInstance().getFile();

		gnuPgDir.mkdir();
		IOUtil.copyResource(GnuPgTest.class, GnuPgTest.PUBRING_FILE_NAME, createFile(gnuPgDir, GnuPgTest.PUBRING_FILE_NAME));
		IOUtil.copyResource(GnuPgTest.class, GnuPgTest.SECRING_FILE_NAME, createFile(gnuPgDir, GnuPgTest.SECRING_FILE_NAME));

		PgpRegistry.getInstance().setPgpAuthenticationCallback(new PgpAuthenticationCallback() {
			@Override
			public char[] getPassphrase(final PgpKey pgpKey) {
				return "test12345".toCharArray();
			}
		});
	}
}
