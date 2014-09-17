package org.subshare.rest.client.transport;

import static org.assertj.core.api.Assertions.*;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.crypto.KeyFactory;
import org.subshare.core.user.User;
import org.subshare.core.user.UserRegistry;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyPublicKeyLookup;
import org.subshare.core.user.UserRepoKeyRing;
import org.junit.Test;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionContext;
import co.codewizards.cloudstore.rest.client.ssl.CheckServerTrustedCertificateExceptionResult;
import co.codewizards.cloudstore.rest.client.ssl.DynamicX509TrustManagerCallback;

public class CryptreeRepoTransportTest {

	private static Random random = new Random();

	@Test
	public void encryptAndSignAndVerifyAndDecrypt() throws Exception {
		final CryptreeRepoTransportFactory factory = new CryptreeRepoTransportFactory();
//		factory.setDynamicX509TrustManagerCallbackClass(TestDynamicX509TrustManagerCallback.class);
		final UUID serverRepositoryId = UUID.randomUUID();
		final UUID clientRepositoryId = UUID.randomUUID();
		final UserRepoKeyRing userRepoKeyRing = createUserRepoKeyRing(serverRepositoryId);
		factory.setUserRepoKeyRing(userRepoKeyRing);

		final CryptreeRepoTransport repoTransport = new CryptreeRepoTransport() {
			@Override
			public UUID getRepositoryId() {
				return serverRepositoryId;
			}
		};
		try {
			repoTransport.setRepoTransportFactory(factory);
			repoTransport.setClientRepositoryId(clientRepositoryId);
			repoTransport.setRemoteRoot(new URL("https://localhost:12345/dummy"));

			final UserRepoKeyPublicKeyLookup userRepoKeyPublicKeyLookup = new UserRepoKeyPublicKeyLookup() {
				@Override
				public UserRepoKey.PublicKey getUserRepoKeyPublicKey(final Uid userRepoKeyId) {
					return userRepoKeyRing.getUserRepoKeyOrFail(userRepoKeyId).getPublicKey();
				}
			};

			final KeyParameter keyParameter = KeyFactory.getInstance().createSymmetricKey();

			for (int i = 0; i < 100; ++i) {
				final int length = 1 + random.nextInt(1024 * 1024);
				System.out.println("length=" + length);

				final byte[] plainText = new byte[length];
				random.nextBytes(plainText);

				final byte[] encryptedAndSigned = repoTransport.encryptAndSign(plainText, keyParameter);
				final byte[] decrypted = repoTransport.verifyAndDecrypt(encryptedAndSigned, keyParameter, userRepoKeyPublicKeyLookup);
				assertThat(decrypted).isEqualTo(plainText);

				System.out.println();
			}
		} finally {
			repoTransport.close();
		}
	}

	public static class TestDynamicX509TrustManagerCallback implements DynamicX509TrustManagerCallback {
		@Override
		public CheckServerTrustedCertificateExceptionResult handleCheckServerTrustedCertificateException(final CheckServerTrustedCertificateExceptionContext context) {
			final CheckServerTrustedCertificateExceptionResult result = new CheckServerTrustedCertificateExceptionResult();
			result.setTrusted(true);
			return result;
		}
	}

	protected static UserRepoKeyRing createUserRepoKeyRing(final UUID repositoryId) {
		final UserRegistry userRegistry = new TestUserRegistry();
		final User user = userRegistry.getUsers().iterator().next();
		final UserRepoKeyRing userRepoKeyRing = user.getUserRepoKeyRingOrCreate();
		user.createUserRepoKey(repositoryId);
		user.createUserRepoKey(repositoryId);
		return userRepoKeyRing;
	}

	private static class TestUserRegistry extends UserRegistry {
		private final User user;

		public TestUserRegistry() {
			user = new User();
			user.getPgpKeyIds().add(0L);
			user.getEmails().add("user@domain.tld");
			user.setFirstName("Hans");
			user.setLastName("Müller");
			user.getUserRepoKeyRingOrCreate();
		}

		@Override
		protected void readUserListFile() {
			// nothing
		}
		@Override
		protected void readPgpUsers() {
			// nothing
		}

		@Override
		public synchronized Collection<User> getUsers() {
			return Collections.singleton(user);
		}
	}

}
