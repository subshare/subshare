package org.subshare.test;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.util.ReflectionUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.pgp.ImportKeysResult;
import org.subshare.core.pgp.ImportKeysResult.ImportedMasterKey;
import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStore;
import org.subshare.core.pgp.man.PgpPrivateKeyPassphraseStoreImpl;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRegistryImpl;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;

import mockit.Mock;
import mockit.MockUp;

public class MultiplePgpKeysSingleUserRegistryIT extends AbstractIT {

	private static final Logger logger = LoggerFactory.getLogger(MultiplePgpKeysSingleUserRegistryIT.class);

	private UserRegistry userRegistry;
	private UUID serverRepositoryId;

	@Override
	public void before() throws Exception {
		serverRepositoryId = UUID.randomUUID();

		userRegistryImplMockUp = new MockUp<UserRegistryImpl>() {
			@Mock
			UserRegistry getInstance() {
				return userRegistry;
			}
		};
	}

	@Test
	public void multiplePgpKeysSingleUserRegistry() throws Exception {
		PgpTestUtil.setupPgp("marco", "not-used");

		PgpPrivateKeyPassphraseStore pgpPrivateKeyPassphraseStore = PgpPrivateKeyPassphraseStoreImpl.getInstance();
		pgpPrivateKeyPassphraseStore.putPassphrase( // marco
				new PgpKeyId("d7a92a24aa97ddbd"), TestUser.marco.getPgpPrivateKeyPassword().toCharArray());

		PgpRegistry pgpRegistry = PgpRegistry.getInstance();
		pgpRegistry.setPgpAuthenticationCallback(pgpPrivateKeyPassphraseStore.getPgpAuthenticationCallback());
		Pgp pgp = pgpRegistry.getPgpOrFail();

		try (InputStream in = getClass().getClassLoader().getResourceAsStream("org/subshare/test/gpg/bieber/pubring.gpg")) {
			logImportKeysResult(pgp.importKeys(castStream(in)));
		}
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("org/subshare/test/gpg/bieber/secring.gpg")) {
			logImportKeysResult(pgp.importKeys(castStream(in)));
		}

		createUserRegistry();
		User marco = userRegistry.getUsersByEmail(TestUser.marco.getEmail()).iterator().next();
		marco.createUserRepoKey(serverRepositoryId).getKeyPair();
		userRegistry.writeIfNeeded();

		pgpPrivateKeyPassphraseStore.clear();
		pgpPrivateKeyPassphraseStore.putPassphrase( // bieber
				new PgpKeyId("bbef20af171556db"), TestUser.bieber.getPgpPrivateKeyPassword().toCharArray());

		createUserRegistry();
		User bieber = userRegistry.getUsersByEmail(TestUser.bieber.getEmail()).iterator().next();
		bieber.createUserRepoKey(serverRepositoryId);
		userRegistry.writeIfNeeded();

		createUserRegistry();
		marco = userRegistry.getUsersByEmail(TestUser.marco.getEmail()).iterator().next();
		UserRepoKeyRing userRepoKeyRing = marco.getUserRepoKeyRing();
		assertThat(userRepoKeyRing).isNotNull();
		Collection<UserRepoKey> userRepoKeys = userRepoKeyRing.getUserRepoKeys();
		assertThat(userRepoKeys).hasSize(1);
	}

	private void createUserRegistry() {
		if (userRegistry != null) // make sure, all old data is written!
			userRegistry.writeIfNeeded();

		userRegistry = invokeConstructor(UserRegistryImpl.class);
	}

	private void logImportKeysResult(ImportKeysResult importKeysResult) {
		logger.info("importKeysResult:");
		for (Map.Entry<PgpKeyId, ImportedMasterKey> me : importKeysResult.getPgpKeyId2ImportedMasterKey().entrySet()) {
			logger.info("* " + me.getKey().toHumanString());
		}
	}

}
