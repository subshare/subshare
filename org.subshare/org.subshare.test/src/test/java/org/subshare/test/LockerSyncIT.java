package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;
import static org.assertj.core.api.Assertions.*;
import static org.subshare.test.PgpTestUtil.*;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import mockit.Mock;
import mockit.MockUp;

import org.subshare.core.locker.LockerSync;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.gnupg.BcWithLocalGnuPgPgp;
import org.subshare.core.pgp.gnupg.GnuPgDir;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStoreImpl;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerImpl;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRegistryImpl;
import org.subshare.rest.server.LockerDir;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;

public class LockerSyncIT extends AbstractIT {

	private static final Logger logger = LoggerFactory.getLogger(LockerSyncIT.class);

	private enum Location {
		client1,
		client2,
		server
	}

	private Location location;

	private MockUp<ConfigDir> configDirMockUp;
	private MockUp<Config> configMockUp;
	private MockUp<GnuPgDir> gnuPgDirMockUp;
	private MockUp<PgpRegistry> pgpRegistryMockUp;
	private MockUp<UserRegistryImpl> userRegistryImplMockUp;

	private Map<Location, ConfigDir> location2ConfigDir = new HashMap<>(Location.values().length);
	private Map<Location, Config> location2Config = new HashMap<>(Location.values().length);
	private Map<Location, GnuPgDir> location2GnuPgDir = new HashMap<>(Location.values().length);
	private Map<Location, Pgp> location2Pgp = new HashMap<>(Location.values().length);
	private Map<Location, UserRegistry> location2UserRegistry = new HashMap<>(Location.values().length);

	protected void setLocation(Location location) {
		this.location = assertNotNull("location", location);
		System.setProperty(ConfigDir.SYSTEM_PROPERTY_CONFIG_DIR, "build/" + jvmInstanceId + '/' + location + "/.cloudstore");
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + GnuPgDir.CONFIG_KEY_GNU_PG_DIR, "build/" + jvmInstanceId + '/' + location + "/.gnupg");
	}

	@Before
	public void before() throws Exception {
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + LockerDir.CONFIG_KEY_LOCKER_DIR, "build/" + jvmInstanceId + "/locker");

		setLocation(Location.server);

		configDirMockUp = new MockUp<ConfigDir>() {
			@Mock
			ConfigDir getInstance() {
				ConfigDir configDir = location2ConfigDir.get(location);
				if (configDir == null) {
					configDir = invokeConstructor(ConfigDir.class);
					location2ConfigDir.put(location, configDir);
				}
				return configDir;
			}
		};

		configMockUp = new MockUp<Config>() {
			@Mock
			Config getInstance() {
				Config config = location2Config.get(location);
				if (config == null) {
					config = invokeConstructor(Config.class,
							null, null,
							new File[] { createFile(ConfigDir.getInstance().getFile(), "cloudstore.properties") });
					location2Config.put(location, config);
				}
				return config;
			}
		};

		gnuPgDirMockUp = new MockUp<GnuPgDir>() {
			@Mock
			GnuPgDir getInstance() {
				GnuPgDir gnuPgDir = location2GnuPgDir.get(location);
				if (gnuPgDir == null) {
					gnuPgDir = invokeConstructor(GnuPgDir.class);
					location2GnuPgDir.put(location, gnuPgDir);
				}
				return gnuPgDir;
			}
		};

		pgpRegistryMockUp = new MockUp<PgpRegistry>() {
			@Mock
			Pgp getPgpOrFail() {
				Pgp pgp = location2Pgp.get(location);
				if (pgp == null) {
					pgp = new BcWithLocalGnuPgPgp();
					location2Pgp.put(location, pgp);
				}
				return pgp;
			}
		};

		userRegistryImplMockUp = new MockUp<UserRegistryImpl>() {
			@Mock
			UserRegistry getInstance() {
				UserRegistry userRegistry = location2UserRegistry.get(location);
				if (userRegistry == null) {
					userRegistry = invokeConstructor(UserRegistryImpl.class);
					location2UserRegistry.put(location, userRegistry);
				}
				return userRegistry;
			}
		};
	}

	@After
	public void after() throws Exception {
		if (userRegistryImplMockUp != null)
			userRegistryImplMockUp.tearDown();

		if (configMockUp != null)
			configMockUp.tearDown();

		if (configDirMockUp != null)
			configDirMockUp.tearDown();

		if (pgpRegistryMockUp != null)
			pgpRegistryMockUp.tearDown();

		if (gnuPgDirMockUp != null)
			gnuPgDirMockUp.tearDown();
	}

	@Test
	public void testLockerSync() throws Exception {
		// preparation
		Server server = new ServerImpl();
		server.setUrl(new URL(getSecureUrl()));

		// *** machine 1 ***
		setLocation(Location.client1);
		createUserRegistry("marco");

		PgpPrivateKeyPassphraseStoreImpl.getInstance().putPassphrase(
				new PgpKeyId("d7a92a24aa97ddbd"), "test12345".toCharArray());

		// *** machine 2 ***
		setLocation(Location.client2);
		createUserRegistry("marco");

		// *** machine 1 ***
		setLocation(Location.client1);
		UserRegistry userRegistry = getUserRegistry();

		// create new user
		User user1 = userRegistry.createUser();
		user1.setUserId(new Uid());
		user1.setFirstName("First 1");
		user1.setLastName("Last 1");
		user1.getEmails().add("first1.last1@domain1.tld");
		userRegistry.addUser(user1);
		userRegistry.writeIfNeeded();

		Collection<User> users = userRegistry.getUsersByEmail("first1.last1@domain1.tld");
		assertThat(users).hasSize(1);

		// sync client1 to server
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}

		// *** machine 2 ***
		setLocation(Location.client2);
		userRegistry = getUserRegistry();

		users = userRegistry.getUsersByEmail("first1.last1@domain1.tld");
		assertThat(users).isEmpty();

		// sync from server to client2
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}

		users = userRegistry.getUsersByEmail("first1.last1@domain1.tld");
		assertThat(users).hasSize(1);
	}

	protected UserRegistry getUserRegistry() {
		UserRegistry userRegistry = location2UserRegistry.get(location);
		assertNotNull("userRegistry[" + location + "]", userRegistry);
		return userRegistry;
	}

	protected UserRegistry createUserRegistry(String ownerName) throws Exception {
		setupPgp(ownerName, "not-used");
		PgpRegistry.getInstance().setPgpAuthenticationCallback(PgpPrivateKeyPassphraseStoreImpl.getInstance().getPgpAuthenticationCallback());

		UserRegistry userRegistry = new UserRegistryImpl() { // protected constructor => subclass ;-)
		};
		location2UserRegistry.put(location, userRegistry);
		return userRegistry;
	}
}
