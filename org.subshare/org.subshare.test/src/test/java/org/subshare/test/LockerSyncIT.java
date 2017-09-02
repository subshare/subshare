package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.locker.sync.LockerSync;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.gnupg.BcWithLocalGnuPgPgp;
import org.subshare.core.pgp.gnupg.GnuPgDir;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStoreImpl;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerImpl;
import org.subshare.core.server.ServerRegistry;
import org.subshare.core.server.ServerRegistryImpl;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRegistryImpl;
import org.subshare.rest.server.LockerDir;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.config.ConfigImpl;
import co.codewizards.cloudstore.core.oio.File;
import mockit.Mock;
import mockit.MockUp;

public class LockerSyncIT extends AbstractIT {

	private static final Logger logger = LoggerFactory.getLogger(LockerSyncIT.class);

	private enum Location {
		client1,
		client2,
		server
	}

	private Location location;

	private MockUp<ConfigDir> configDirMockUp;
	private MockUp<ConfigImpl> configMockUp;
	private MockUp<GnuPgDir> gnuPgDirMockUp;
	private MockUp<PgpRegistry> pgpRegistryMockUp;
	private MockUp<UserRegistryImpl> userRegistryImplMockUp;
	private MockUp<ServerRegistryImpl> serverRegistryImplMockUp;

	private Map<Location, ConfigDir> location2ConfigDir = new HashMap<>(Location.values().length);
	private Map<Location, ConfigImpl> location2Config = new HashMap<>(Location.values().length);
	private Map<Location, GnuPgDir> location2GnuPgDir = new HashMap<>(Location.values().length);
	private Map<Location, Pgp> location2Pgp = new HashMap<>(Location.values().length);
	private Map<Location, UserRegistry> location2UserRegistry = new HashMap<>(Location.values().length);
	private Map<Location, ServerRegistry> location2ServerRegistry = new HashMap<>(Location.values().length);

	protected void setLocation(Location location) {
		this.location = assertNotNull(location, "location");
		System.setProperty(ConfigDir.SYSTEM_PROPERTY_CONFIG_DIR, jvmInstanceDir + '/' + location + "/.cloudstore");
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + GnuPgDir.CONFIG_KEY_GNU_PG_DIR, jvmInstanceDir + '/' + location + "/.gnupg");
	}

	@Override
	@Before
	public void before() throws Exception {
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + LockerDir.CONFIG_KEY_LOCKER_DIR, jvmInstanceDir + "/locker");

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

		configMockUp = new MockUp<ConfigImpl>() {
			@Mock
			Config getInstance() {
				ConfigImpl config = location2Config.get(location);
				if (config == null) {
					config = invokeConstructor(ConfigImpl.class,
							(Object) null, null,
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
					pgp = invokeConstructor(BcWithLocalGnuPgPgp.class);
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

		serverRegistryImplMockUp = new MockUp<ServerRegistryImpl>() {
			@Mock
			ServerRegistry getInstance() {
				ServerRegistry serverRegistry = location2ServerRegistry.get(location);
				if (serverRegistry == null) {
					serverRegistry = invokeConstructor(ServerRegistryImpl.class);
					location2ServerRegistry.put(location, serverRegistry);
				}
				return serverRegistry;
			}
		};
	}

	@Override
	@After
	public void after() throws Exception {
		if (serverRegistryImplMockUp != null)
			serverRegistryImplMockUp.tearDown();

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
	public void syncUserRegistry() throws Exception {
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

		// again create new user (this time on machine 2)
		User user2 = userRegistry.createUser();
		user2.setUserId(new Uid());
		user2.setFirstName("First 2");
		user2.setLastName("Last 2");
		user2.getEmails().add("first2.last2@domain2.tld");
		userRegistry.addUser(user2);
		userRegistry.writeIfNeeded();

		// sync from client2 to server
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}


		// *** machine 1 ***
		setLocation(Location.client1);
		userRegistry = getUserRegistry();

		users = userRegistry.getUsersByEmail("first1.last1@domain1.tld");
		assertThat(users).hasSize(1);

		users = userRegistry.getUsersByEmail("first2.last2@domain2.tld");
		assertThat(users).isEmpty();

		// sync from server to client1
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}

		users = userRegistry.getUsersByEmail("first1.last1@domain1.tld");
		assertThat(users).hasSize(1);

		users = userRegistry.getUsersByEmail("first2.last2@domain2.tld");
		assertThat(users).hasSize(1);

		user2 = users.iterator().next();
		assertThat(user2.getFirstName()).isEqualTo("First 2");
		assertThat(user2.getLastName()).isEqualTo("Last 2");

		user2.setFirstName("First 2 XXX");
		user2.setLastName("Last 2 YYY");
		userRegistry.writeIfNeeded();

		// sync from client1 to server
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}


		// *** machine 2 ***
		setLocation(Location.client2);
		userRegistry = getUserRegistry();

		users = userRegistry.getUsersByEmail("first1.last1@domain1.tld");
		assertThat(users).hasSize(1);

		user1 = users.iterator().next();
		user1.setFirstName("Anton");
		user1.setLastName("Müller");
		userRegistry.writeIfNeeded();

		users = userRegistry.getUsersByEmail("first2.last2@domain2.tld");
		assertThat(users).hasSize(1);

		user2 = users.iterator().next();
		assertThat(user2.getFirstName()).isEqualTo("First 2");
		assertThat(user2.getLastName()).isEqualTo("Last 2");

		// sync user2 from server to client2 and user1 vice-versa at the same time
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}

		users = userRegistry.getUsersByEmail("first2.last2@domain2.tld");
		assertThat(users).hasSize(1);

		users = userRegistry.getUsersByEmail("first1.last1@domain1.tld");
		assertThat(users).hasSize(1);

		user1 = users.iterator().next();
		assertThat(user1.getFirstName()).isEqualTo("Anton");
		assertThat(user1.getLastName()).isEqualTo("Müller");

		users = userRegistry.getUsersByEmail("first2.last2@domain2.tld");
		assertThat(users).hasSize(1);

		user2 = users.iterator().next();
		assertThat(user2.getFirstName()).isEqualTo("First 2 XXX");
		assertThat(user2.getLastName()).isEqualTo("Last 2 YYY");


		// *** machine 1 ***
		setLocation(Location.client1);
		userRegistry = getUserRegistry();

		users = userRegistry.getUsersByEmail("first1.last1@domain1.tld");
		assertThat(users).hasSize(1);

		user1 = users.iterator().next();
		assertThat(user1.getFirstName()).isEqualTo("First 1");
		assertThat(user1.getLastName()).isEqualTo("Last 1");

		users = userRegistry.getUsersByEmail("first2.last2@domain2.tld");
		assertThat(users).hasSize(1);

		user2 = users.iterator().next();
		assertThat(user2.getFirstName()).isEqualTo("First 2 XXX");
		assertThat(user2.getLastName()).isEqualTo("Last 2 YYY");

		// sync from server to client1
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}

		users = userRegistry.getUsersByEmail("first1.last1@domain1.tld");
		assertThat(users).hasSize(1);

		user1 = users.iterator().next();
		assertThat(user1.getFirstName()).isEqualTo("Anton");
		assertThat(user1.getLastName()).isEqualTo("Müller");

		users = userRegistry.getUsersByEmail("first2.last2@domain2.tld");
		assertThat(users).hasSize(1);

		user2 = users.iterator().next();
		assertThat(user2.getFirstName()).isEqualTo("First 2 XXX");
		assertThat(user2.getLastName()).isEqualTo("Last 2 YYY");

		userRegistry.removeUser(user1);
		userRegistry.writeIfNeeded();

		users = userRegistry.getUsersByEmail("first1.last1@domain1.tld");
		assertThat(users).isEmpty();

		// sync from client1 to server
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}


		// *** machine 2 ***
		setLocation(Location.client2);
		userRegistry = getUserRegistry();

		users = userRegistry.getUsersByEmail("first1.last1@domain1.tld");
		assertThat(users).hasSize(1);

		// sync from server to client2
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}

		users = userRegistry.getUsersByEmail("first1.last1@domain1.tld");
		assertThat(users).isEmpty();

		users = userRegistry.getUsersByEmail("first2.last2@domain2.tld");
		assertThat(users).hasSize(1);
	}

	@Test
	public void syncServerRegistry() throws Exception {
		// preparation
		Server server = new ServerImpl();
		server.setUrl(new URL(getSecureUrl()));

		// *** machine 1 ***
		setLocation(Location.client1);
		createServerRegistry("marco");

		PgpPrivateKeyPassphraseStoreImpl.getInstance().putPassphrase(
				new PgpKeyId("d7a92a24aa97ddbd"), "test12345".toCharArray());

		// *** machine 2 ***
		setLocation(Location.client2);
		createServerRegistry("marco");

		// *** machine 1 ***
		setLocation(Location.client1);
		ServerRegistry serverRegistry = getServerRegistry();

		// create a new server
		Server server1 = serverRegistry.createServer();
		server1.setName("Name 1");
		server1.setUrl(new URL("https://server1.domain.tld:12345"));
		serverRegistry.getServers().add(server1);
		serverRegistry.writeIfNeeded();

		assertThat(serverRegistry.getServers()).hasSize(1);

		// sync client1 to server
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}


		// *** machine 2 ***
		setLocation(Location.client2);
		serverRegistry = getServerRegistry();

		assertThat(serverRegistry.getServers()).isEmpty();

		// sync from server to client2
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}

		assertThat(serverRegistry.getServers()).hasSize(1);
		server1 = serverRegistry.getServers().get(0);

		assertThat(server1.getName()).isEqualTo("Name 1");
		assertThat(server1.getUrl()).isEqualTo(new URL("https://server1.domain.tld:12345"));

		Server server2 = serverRegistry.createServer();
		server2.setName("Name 2");
		server2.setUrl(new URL("https://server2.bla.com:666"));
		serverRegistry.getServers().add(server2);

		Server server3 = serverRegistry.createServer();
		server3.setName("Name 3");
		server3.setUrl(new URL("https://server3.oink.org:1111"));
		serverRegistry.getServers().add(server3);
		serverRegistry.writeIfNeeded();

		// sync from client2 to server
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}


		// *** machine 1 ***
		setLocation(Location.client1);
		serverRegistry = getServerRegistry();

		assertThat(serverRegistry.getServers()).hasSize(1);

		// sync from server to client1
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}

		assertThat(serverRegistry.getServers()).hasSize(3);

		server1 = serverRegistry.getServers().get(0);
		server2 = serverRegistry.getServers().get(1);
		server3 = serverRegistry.getServers().get(2);

		assertThat(server1.getName()).isEqualTo("Name 1");
		assertThat(server2.getName()).isEqualTo("Name 2");
		assertThat(server3.getName()).isEqualTo("Name 3");

		server1.setName("Name 1 bla bla");
		server2.setName("Name 2 blubb blubb");
		serverRegistry.writeIfNeeded();

		// sync from client1 to server
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}


		// *** machine 2 ***
		setLocation(Location.client2);
		serverRegistry = getServerRegistry();

		assertThat(serverRegistry.getServers()).hasSize(3);

		server1 = serverRegistry.getServers().get(0);
		server2 = serverRegistry.getServers().get(1);

		assertThat(server1.getName()).isEqualTo("Name 1");
		assertThat(server2.getName()).isEqualTo("Name 2");

		// sync from server to client2
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}

		assertThat(server1.getName()).isEqualTo("Name 1 bla bla");
		assertThat(server2.getName()).isEqualTo("Name 2 blubb blubb");

		serverRegistry.getServers().remove(server2);
		serverRegistry.writeIfNeeded();

		// sync from client2 to server
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}


		// *** machine 1 ***
		setLocation(Location.client1);
		serverRegistry = getServerRegistry();

		assertThat(serverRegistry.getServers()).hasSize(3);

		// sync from server to client1
		try (LockerSync lockerSync = new LockerSync(server);) {
			lockerSync.sync();
		}

		assertThat(serverRegistry.getServers()).hasSize(2);
		server1 = serverRegistry.getServers().get(0);
		server2 = null;
		server3 = serverRegistry.getServers().get(1);

		assertThat(server1.getName()).isEqualTo("Name 1 bla bla");
		assertThat(server3.getName()).isEqualTo("Name 3");
	}

	protected UserRegistry getUserRegistry() {
		UserRegistry userRegistry = location2UserRegistry.get(location);
		assertNotNull(userRegistry, "userRegistry[" + location + "]");
		return userRegistry;
	}

	protected ServerRegistry getServerRegistry() {
		ServerRegistry serverRegistry = location2ServerRegistry.get(location);
		assertNotNull(serverRegistry, "serverRegistry[" + location + "]");
		return serverRegistry;
	}

	protected UserRegistry createUserRegistry(String ownerName) throws Exception {
		setupPgp(ownerName);

		UserRegistry userRegistry = invokeConstructor(UserRegistryImpl.class);
		location2UserRegistry.put(location, userRegistry);
		return userRegistry;
	}

	protected ServerRegistry createServerRegistry(String ownerName) throws Exception {
		setupPgp(ownerName);

		ServerRegistry serverRegistry = invokeConstructor(ServerRegistryImpl.class);
		location2ServerRegistry.put(location, serverRegistry);
		return serverRegistry;
	}

	protected void setupPgp(String ownerName) throws Exception {
		PgpTestUtil.setupPgp(ownerName, "not-used");
		PgpRegistry.getInstance().setPgpAuthenticationCallback(PgpPrivateKeyPassphraseStoreImpl.getInstance().getPgpAuthenticationCallback());
	}
}
