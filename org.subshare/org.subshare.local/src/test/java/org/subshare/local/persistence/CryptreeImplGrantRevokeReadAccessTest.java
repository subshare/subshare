package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import org.subshare.core.AccessDeniedException;
import org.subshare.core.Cryptree;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.AbstractTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class CryptreeImplGrantRevokeReadAccessTest extends AbstractTest {

	private static final Logger logger = LoggerFactory.getLogger(CryptreeImplGrantRevokeReadAccessTest.class);

	private final String[] localPaths = {
			"/",
			"/1/",
			"/1/1_a",
			"/1/1_b",
			"/2/",
			"/2/2_a",
			"/2/2_b",
			"/2/2_c",
			"/2/2_1/",
			"/2/2_1/2_1_a",
			"/2/2_1/2_1_b",
			"/3/",
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
	};

	private File localRoot;
	private File remoteRoot;
	private UUID remoteRepositoryId;

	private UserRepoKeyRing ownerUserRepoKeyRing;
	private UserRepoKeyRing friend1UserRepoKeyRing;
	private UserRepoKey friend1UserRepoKey;
	private UserRepoKeyRing friend2UserRepoKeyRing;
	private UserRepoKey friend2UserRepoKey;

	@Test
	public void grantAndRevokeReadAccess() throws Exception {
		createLocalRoot();
		createRemoteRoot();
		createDirectoriesAndFiles(localRoot, localPaths);
		connectLocalWithRemoteRepository();
		createUserRepoKeyRings();
		createOrUpdateCryptoRepoFiles(localPaths);

		grantReadAccess("/3", friend1UserRepoKey.getPublicKey(), friend2UserRepoKey.getPublicKey());

		assertReadAccessCorrect(friend1UserRepoKeyRing, "/3");
		assertReadAccessCorrect(friend2UserRepoKeyRing, "/3");

		revokeReadAccess("/3", friend1UserRepoKey.getUserRepoKeyId());

		// Should still be readable, because we have lazy revocation and there was no modification, yet!
		assertReadAccessCorrect(friend1UserRepoKeyRing, "/3");
		assertReadAccessCorrect(friend2UserRepoKeyRing, "/3");

		createOrUpdateCryptoRepoFiles("/3");

		assertReadAccessDenied(friend1UserRepoKeyRing, "/3");

		// We modified only "/3" and none of the children - and neither the parent (the root "/").
		// Due to the lazy revocation, they should thus still be readable.
		assertReadAccessGranted(friend1UserRepoKeyRing,
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
		assertReadAccessCorrect(friend2UserRepoKeyRing, "/3");

		// Modify a few more.
		createOrUpdateCryptoRepoFiles("/", "/3/3_a", "/3/3_1/3_1_b");

		// All those modified should *not* be accessible anymore.
		assertReadAccessDenied(friend1UserRepoKeyRing, "/", "/3", "/3/3_a", "/3/3_1/3_1_b");

		// All those not yet modified should still be accessible.
		assertReadAccessGranted(friend1UserRepoKeyRing,
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
		assertReadAccessCorrect(friend2UserRepoKeyRing, "/3");
	}

	private void createLocalRoot() throws Exception {
		localRoot = newTestRepositoryLocalRoot("local");
		localRoot.mkdirs();
	}

	private void createRemoteRoot() throws Exception {
		remoteRoot = newTestRepositoryLocalRoot("remote");
		remoteRoot.mkdirs();
	}

	private void connectLocalWithRemoteRepository() throws Exception {
		try (LocalRepoManager localLocalRepoManager = createLocalRepoManagerForNewRepository(localRoot);) {
			try (LocalRepoManager remoteLocalRepoManager = createLocalRepoManagerForNewRepository(remoteRoot);) {
				remoteRepositoryId = remoteLocalRepoManager.getRepositoryId();
				localLocalRepoManager.putRemoteRepository(remoteRepositoryId, null, remoteLocalRepoManager.getPublicKey(), "");
				remoteLocalRepoManager.putRemoteRepository(localLocalRepoManager.getRepositoryId(), null, localLocalRepoManager.getPublicKey(), "");
			}
		}
	}

	private void createUserRepoKeyRings() throws Exception {
		ownerUserRepoKeyRing = createUserRepoKeyRing(remoteRepositoryId);

		friend1UserRepoKeyRing = createUserRepoKeyRing(remoteRepositoryId);
		friend1UserRepoKey = friend1UserRepoKeyRing.getRandomUserRepoKeyOrFail(remoteRepositoryId);

		friend2UserRepoKeyRing = createUserRepoKeyRing(remoteRepositoryId);
		friend2UserRepoKey = friend2UserRepoKeyRing.getRandomUserRepoKeyOrFail(remoteRepositoryId);
	}

	private void createOrUpdateCryptoRepoFiles(final String ... localPaths) {
		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
			try (LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				try (Cryptree cryptree = createCryptree(transaction, remoteRepositoryId, "", ownerUserRepoKeyRing.getRandomUserRepoKeyOrFail(remoteRepositoryId));) {
					for (String localPath : localPaths) {
						localPath = removeFinalSlash(localPath);
						cryptree.createOrUpdateCryptoRepoFile(localPath);
					}
				}
				transaction.commit();
			}
		}
	}

	private void grantReadAccess(final String localPath, final UserRepoKey.PublicKey ... publicKeys) {
		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
			try (LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				try (Cryptree cryptree = createCryptree(transaction, remoteRepositoryId, "", ownerUserRepoKeyRing.getRandomUserRepoKeyOrFail(remoteRepositoryId));) {
					for (final UserRepoKey.PublicKey publicKey : publicKeys)
						cryptree.grantReadPermission(localPath, publicKey);
				}
				transaction.commit();
			}
		}
	}

	private void revokeReadAccess(final String localPath, final Uid ... userRepoKeyIds) {
		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
			try (LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				try (Cryptree cryptree = createCryptree(transaction, remoteRepositoryId, "", ownerUserRepoKeyRing.getRandomUserRepoKeyOrFail(remoteRepositoryId));) {
					cryptree.revokeReadPermission(localPath, new HashSet<Uid>(Arrays.asList(userRepoKeyIds)));
				}
				transaction.commit();
			}
		}
	}

	private void assertReadAccessCorrect(final UserRepoKeyRing userRepoKeyRing, final String localPathReadAccessGranted) {
		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
			try (LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				try (Cryptree cryptree = createCryptree(transaction, remoteRepositoryId, "", userRepoKeyRing.getRandomUserRepoKeyOrFail(remoteRepositoryId));) {
					for (String localPath : localPaths) {
						localPath = removeFinalSlash(localPath);
						try {
							cryptree.getDecryptedRepoFileDto(localPath);
							if (! isChildOrEqual(localPathReadAccessGranted, localPath) && ! isParentOrEqual(localPathReadAccessGranted, localPath))
								fail("We should not have access to this: " + localPath);
						} catch (final AccessDeniedException x) {
							if (isChildOrEqual(localPathReadAccessGranted, localPath) || isParentOrEqual(localPathReadAccessGranted, localPath))
								fail("We should have access to this: " + localPath, x);
						}

						try {
							cryptree.getDataKeyOrFail(localPath);
							if (! isChildOrEqual(localPathReadAccessGranted, localPath) && ! isParentOrEqual(localPathReadAccessGranted, localPath))
								fail("We should not have access to this: " + localPath);
						} catch (final AccessDeniedException x) {
							if (isChildOrEqual(localPathReadAccessGranted, localPath) || isParentOrEqual(localPathReadAccessGranted, localPath))
								fail("We should have access to this: " + localPath, x);
						}
					}
				}
				transaction.commit();
			}
		}
	}

	private void assertReadAccessDenied(final UserRepoKeyRing userRepoKeyRing, final String ... localPaths) {
		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
			try (LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				try (Cryptree cryptree = createCryptree(transaction, remoteRepositoryId, "", userRepoKeyRing.getRandomUserRepoKeyOrFail(remoteRepositoryId));) {
					for (String localPath : localPaths) {
						localPath = removeFinalSlash(localPath);
						try {
							cryptree.getDecryptedRepoFileDto(localPath);
							fail("We should not have access to this: " + localPath);
						} catch (final AccessDeniedException x) {
							doNothing(); // we expect exactly this!
						}

						try {
							cryptree.getDataKeyOrFail(localPath);
							fail("We should not have access to this: " + localPath);
						} catch (final AccessDeniedException x) {
							doNothing(); // we expect exactly this!
						}
					}
				}
				transaction.commit();
			}
		}
	}

	private void assertReadAccessGranted(final UserRepoKeyRing userRepoKeyRing, final String ... localPaths) {
		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
			try (LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				try (Cryptree cryptree = createCryptree(transaction, remoteRepositoryId, "", userRepoKeyRing.getRandomUserRepoKeyOrFail(remoteRepositoryId));) {
					for (String localPath : localPaths) {
						localPath = removeFinalSlash(localPath);
						try {
							cryptree.getDecryptedRepoFileDto(localPath);
						} catch (final AccessDeniedException x) {
							fail("We should have access to this: " + localPath, x);
						}

						try {
							cryptree.getDataKeyOrFail(localPath);
						} catch (final AccessDeniedException x) {
							fail("We should have access to this: " + localPath, x);
						}
					}
				}
				transaction.commit();
			}
		}
	}

	private boolean isChildOrEqual(final String path, final String maybeChild) {
		return maybeChild.startsWith(path);
	}

	private boolean isParentOrEqual(final String path, final String maybeParent) {
		String dir = path;
		while (true) {
			if (maybeParent.equals(dir))
				return true;

			final int lastSlashIndex = dir.lastIndexOf("/");
			if (lastSlashIndex < 0)
				return false;

			dir = lastSlashIndex == 0 ? "" : dir.substring(0, lastSlashIndex - 1);
		}
	}
}
