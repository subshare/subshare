package org.subshare.local;

import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;

import org.subshare.core.Cryptree;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.LoggerProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public abstract class AbstractPermissionTest extends AbstractTest {

	private static final Logger logger = LoggerFactory.getLogger(AbstractPermissionTest.class);

	protected final String[] localPaths = {
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
				"/3/3_2/3_2_1/",
				"/3/3_2/3_2_1/3_2_1_a",
				"/3/3_3/",
				"/3/3_3/3_3_a"
		};

	protected File localRoot;
	protected File remoteRoot;
	protected UUID remoteRepositoryId;
	protected UserRepoKeyRing ownerUserRepoKeyRing;
	protected UserRepoKey ownerUserRepoKey;
	protected UserRepoKeyRing friend1UserRepoKeyRing;
	protected UserRepoKey friend1UserRepoKey;
	protected UserRepoKeyRing friend2UserRepoKeyRing;
	protected UserRepoKey friend2UserRepoKey;

	@Before
	public void before() throws Exception {
		createLocalRoot();
		createRemoteRoot();
		connectLocalWithRemoteRepository();
		createUserRepoKeyRings();

		createDirectoriesAndFiles(localRoot, localPaths);
		createOrUpdateCryptoRepoFiles(localPaths);
	}

	protected void createLocalRoot() throws Exception {
		localRoot = newTestRepositoryLocalRoot("local");
		localRoot.mkdirs();
	}

	protected void createRemoteRoot() throws Exception {
		remoteRoot = newTestRepositoryLocalRoot("remote");
		remoteRoot.mkdirs();
	}

	protected void connectLocalWithRemoteRepository() throws Exception {
		try (LocalRepoManager localLocalRepoManager = createLocalRepoManagerForNewRepository(localRoot);) {
			try (LocalRepoManager remoteLocalRepoManager = createLocalRepoManagerForNewRepository(remoteRoot);) {
				remoteRepositoryId = remoteLocalRepoManager.getRepositoryId();
				localLocalRepoManager.putRemoteRepository(remoteRepositoryId, null, remoteLocalRepoManager.getPublicKey(), "");
				remoteLocalRepoManager.putRemoteRepository(localLocalRepoManager.getRepositoryId(), null, localLocalRepoManager.getPublicKey(), "");
			}
		}
	}

	protected void createUserRepoKeyRings() throws Exception {
		ownerUserRepoKeyRing = createUserRepoKeyRing(remoteRepositoryId);
		ownerUserRepoKey = ownerUserRepoKeyRing.getRandomUserRepoKey(remoteRepositoryId);

		friend1UserRepoKeyRing = createUserRepoKeyRing(remoteRepositoryId);
		friend1UserRepoKey = friend1UserRepoKeyRing.getRandomUserRepoKeyOrFail(remoteRepositoryId);

		friend2UserRepoKeyRing = createUserRepoKeyRing(remoteRepositoryId);
		friend2UserRepoKey = friend2UserRepoKeyRing.getRandomUserRepoKeyOrFail(remoteRepositoryId);
	}

	protected void createOrUpdateCryptoRepoFiles(final String ... localPaths) {
		createOrUpdateCryptoRepoFiles(ownerUserRepoKey, localPaths);
	}

	protected void createOrUpdateCryptoRepoFiles(final UserRepoKey signingUserRepoKey, final String ... localPaths) {
		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
			try (LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				try (Cryptree cryptree = createCryptree(transaction, remoteRepositoryId, "", signingUserRepoKey);) {
					for (String localPath : localPaths) {
						localPath = removeFinalSlash(localPath);
						cryptree.createOrUpdateCryptoRepoFile(localPath);
					}
				}
				transaction.commit();
			}
		}
	}

	protected void grantReadPermission(final String localPath, final UserRepoKey.PublicKey ... publicKeys) {
		grantReadPermission(ownerUserRepoKey, localPath, publicKeys);
	}

	protected void grantReadPermission(final UserRepoKey signingUserRepoKey, final String localPath, final UserRepoKey.PublicKey ... publicKeys) {
		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
			try (LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				try (Cryptree cryptree = createCryptree(transaction, remoteRepositoryId, "", signingUserRepoKey);) {
					for (final UserRepoKey.PublicKey publicKey : publicKeys)
						cryptree.grantReadPermission(localPath, publicKey);
				}
				transaction.commit();
			}
		}
	}

	protected void grantGrantPermission(final String localPath, final UserRepoKey.PublicKey ... publicKeys) {
		grantGrantPermission(ownerUserRepoKey, localPath, publicKeys);
	}

	protected void grantGrantPermission(final UserRepoKey signingUserRepoKey, final String localPath, final UserRepoKey.PublicKey ... publicKeys) {
		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
			try (LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				try (Cryptree cryptree = createCryptree(transaction, remoteRepositoryId, "", signingUserRepoKey);) {
					for (final UserRepoKey.PublicKey publicKey : publicKeys)
						cryptree.grantGrantPermission(localPath, publicKey);
				}
				transaction.commit();
			}
		}
	}

	protected void grantWritePermission(final String localPath, final UserRepoKey.PublicKey ... publicKeys) {
		grantWritePermission(ownerUserRepoKey, localPath, publicKeys);
	}

	protected void grantWritePermission(final UserRepoKey signingUserRepoKey, final String localPath, final UserRepoKey.PublicKey ... publicKeys) {
		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
			try (LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				try (Cryptree cryptree = createCryptree(transaction, remoteRepositoryId, "", signingUserRepoKey);) {
					for (final UserRepoKey.PublicKey publicKey : publicKeys)
						cryptree.grantWritePermission(localPath, publicKey);
				}
				transaction.commit();
			}
		}
	}

	protected void revokeReadPermission(final String localPath, final Uid ... userRepoKeyIds) {
		revokeReadPermission(ownerUserRepoKey, localPath, userRepoKeyIds);
	}

	protected void revokeReadPermission(final UserRepoKey signingUserRepoKey, final String localPath, final Uid ... userRepoKeyIds) {
		try (LocalRepoManager localRepoManager = createLocalRepoManagerForExistingRepository(localRoot);) {
			localRepoManager.localSync(new LoggerProgressMonitor(logger));
			try (LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				try (Cryptree cryptree = createCryptree(transaction, remoteRepositoryId, "", signingUserRepoKey);) {
					cryptree.revokeReadPermission(localPath, new HashSet<Uid>(Arrays.asList(userRepoKeyIds)));
				}
				transaction.commit();
			}
		}
	}

	protected boolean isChildOrEqual(final String path, final String maybeChild) {
		return maybeChild.startsWith(path);
	}

	protected boolean isParentOrEqual(final String path, final String maybeParent) {
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
