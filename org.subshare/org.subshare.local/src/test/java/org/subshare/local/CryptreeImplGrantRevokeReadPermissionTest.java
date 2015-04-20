package org.subshare.local;

import static co.codewizards.cloudstore.core.util.Util.doNothing;
import static org.assertj.core.api.Assertions.fail;
import mockit.Mock;
import mockit.MockUp;

import org.subshare.core.Cryptree;
import org.subshare.core.GrantAccessDeniedException;
import org.subshare.core.ReadAccessDeniedException;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class CryptreeImplGrantRevokeReadPermissionTest extends AbstractPermissionTest {

	private static final Logger logger = LoggerFactory.getLogger(CryptreeImplGrantRevokeReadPermissionTest.class);

	@Test
	public void grantAndRevokeReadPermission() throws Exception {
		new MockUp<UserRepoKeyPublicKeyHelper>() {
			@Mock
			private void createUserIdentities(final UserRepoKeyPublicKey userRepoKeyPublicKey) {
				// Our mock should do nothing, because we don't have a real UserRegistry here.
			}
		};

		final boolean ownerIsAdmin = false; // TODO set to false (or maybe use a random value?) once read-cryptree was extended to support 'grant' functionality)!

		final UserRepoKeyRing adminUserRepoKeyRing = ownerIsAdmin ? ownerUserRepoKeyRing : friend0UserRepoKeyRing;

		if (! ownerIsAdmin) {
			try {
				grantReadPermission(adminUserRepoKeyRing, "/3", friend1UserRepoKey.getPublicKey(), friend2UserRepoKey.getPublicKey());
				fail("friend0 was able to grant read permissions without having grant permissions!");
			} catch (final GrantAccessDeniedException x) {
				doNothing(); // this is expected!
			}

			// The following automatically grants read+write permissions, too, because this is technically required.
			grantGrantPermission("/3", friend0UserRepoKey.getPublicKey());
		}

		grantReadPermission(adminUserRepoKeyRing, "/3", friend1UserRepoKey.getPublicKey(), friend2UserRepoKey.getPublicKey());

		assertReadPermissionCorrect(friend1UserRepoKeyRing, "/3");
		assertReadPermissionCorrect(friend2UserRepoKeyRing, "/3");

		revokeReadPermission(adminUserRepoKeyRing, "/3", friend1UserRepoKey.getUserRepoKeyId());

		// Should still be readable, because we have lazy revocation and there was no modification, yet!
		assertReadPermissionCorrect(friend1UserRepoKeyRing, "/3");
		assertReadPermissionCorrect(friend2UserRepoKeyRing, "/3");

		createOrUpdateCryptoRepoFiles("/3");

		assertReadPermissionDenied(friend1UserRepoKeyRing, "/3");

		// We modified only "/3" and none of the children - and neither the parent (the root "/").
		// Due to the lazy revocation, they should thus still be readable.
		assertReadPermissionGranted(friend1UserRepoKeyRing,
				"/",
				"/3/3_a",
				"/3/3_b",
				"/3/3_1/",
				"/3/3_1/3_1_a",
				"/3/3_1/3_1_b",
				"/3/3_1/3_1_c",
				"/3/3_2/",
				"/3/3_2/3_2_a",
				"/3/3_2/3_2_b",
				"/3/3_3/",
				"/3/3_3/3_3_a",
				"/3/3_2/3_2_1/",
				"/3/3_2/3_2_1/3_2_1_a"
				);

		// The 2nd user should be unaffected by the revocation.
		assertReadPermissionCorrect(friend2UserRepoKeyRing, "/3");

		// Modify a few more.
		createOrUpdateCryptoRepoFiles("/", "/3/3_a", "/3/3_1/3_1_b");

		// All those modified should *not* be accessible anymore.
		assertReadPermissionDenied(friend1UserRepoKeyRing, "/", "/3", "/3/3_a", "/3/3_1/3_1_b");

		// All those not yet modified should still be accessible.
		assertReadPermissionGranted(friend1UserRepoKeyRing,
				"/3/3_b",
				"/3/3_1/",
				"/3/3_1/3_1_a",
				"/3/3_1/3_1_c",
				"/3/3_2/",
				"/3/3_2/3_2_a",
				"/3/3_2/3_2_b",
				"/3/3_3/",
				"/3/3_3/3_3_a",
				"/3/3_2/3_2_1/",
				"/3/3_2/3_2_1/3_2_1_a"
				);

		// Still, the 2nd user should be unaffected by the revocation.
		assertReadPermissionCorrect(friend2UserRepoKeyRing, "/3");
	}

	protected void assertReadPermissionCorrect(final UserRepoKeyRing userRepoKeyRing, final String localPathReadAccessGranted) {
		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
			try (LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				final Cryptree cryptree = getCryptree(transaction, remoteRepositoryId, "", userRepoKeyRing);
				for (String localPath : localPaths) {
					localPath = removeFinalSlash(localPath);
					try {
						cryptree.getDecryptedRepoFileDto(localPath);
						if (! isChildOrEqual(localPathReadAccessGranted, localPath) && ! isParentOrEqual(localPathReadAccessGranted, localPath))
							fail("We should not have access to this: " + localPath);
					} catch (final ReadAccessDeniedException x) {
						if (isChildOrEqual(localPathReadAccessGranted, localPath) || isParentOrEqual(localPathReadAccessGranted, localPath))
							fail("We should have access to this: " + localPath, x);
					}

					try {
						cryptree.getDataKeyOrFail(localPath);
						if (! isChildOrEqual(localPathReadAccessGranted, localPath) && ! isParentOrEqual(localPathReadAccessGranted, localPath))
							fail("We should not have access to this: " + localPath);
					} catch (final ReadAccessDeniedException x) {
						if (isChildOrEqual(localPathReadAccessGranted, localPath) || isParentOrEqual(localPathReadAccessGranted, localPath))
							fail("We should have access to this: " + localPath, x);
					}
				}
				transaction.commit();
			}
		}
	}

	protected void assertReadPermissionDenied(final UserRepoKeyRing userRepoKeyRing, final String ... localPaths) {
		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
			try (LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				final Cryptree cryptree = getCryptree(transaction, remoteRepositoryId, "", userRepoKeyRing);
				for (String localPath : localPaths) {
					localPath = removeFinalSlash(localPath);
					try {
						cryptree.getDecryptedRepoFileDto(localPath);
						fail("We should not have access to this: " + localPath);
					} catch (final ReadAccessDeniedException x) {
						doNothing(); // we expect exactly this!
					}

					try {
						cryptree.getDataKeyOrFail(localPath);
						fail("We should not have access to this: " + localPath);
					} catch (final ReadAccessDeniedException x) {
						doNothing(); // we expect exactly this!
					}
				}
				transaction.commit();
			}
		}
	}

	protected void assertReadPermissionGranted(final UserRepoKeyRing userRepoKeyRing, final String ... localPaths) {
		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
			try (LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				final Cryptree cryptree = getCryptree(transaction, remoteRepositoryId, "", userRepoKeyRing);
				for (String localPath : localPaths) {
					localPath = removeFinalSlash(localPath);
					try {
						cryptree.getDecryptedRepoFileDto(localPath);
					} catch (final ReadAccessDeniedException x) {
						fail("We should have access to this: " + localPath, x);
					}

					try {
						cryptree.getDataKeyOrFail(localPath);
					} catch (final ReadAccessDeniedException x) {
						fail("We should have access to this: " + localPath, x);
					}
				}
				transaction.commit();
			}
		}
	}
}
