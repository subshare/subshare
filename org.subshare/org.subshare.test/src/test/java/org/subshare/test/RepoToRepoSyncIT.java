package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;

import mockit.Mock;
import mockit.MockUp;

import org.subshare.core.AccessDeniedException;
import org.subshare.core.ReadAccessDeniedException;
import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.UserRepoKeyPublicKeyHelper;
import org.subshare.local.persistence.InvitationUserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKeyDao;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;

public class RepoToRepoSyncIT extends AbstractRepoToRepoSyncIT {

	private static final Logger logger = LoggerFactory.getLogger(RepoToRepoSyncIT.class);

	@Override
	public void before() throws Exception {
		super.before();

		new MockUp<UserRepoKeyPublicKeyHelper>() {
			@Mock
			private void createUserIdentities(final UserRepoKeyPublicKey userRepoKeyPublicKey) {
				// Our mock should do nothing, because we don't have a real UserRegistry here.
			}
		};
	}

	@Test
	public void syncFromLocalToRemoteToLocal() throws Exception {
		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();
		createLocalDestinationRepo();
		syncFromRemoteToLocalDest();
	}

	@Test
	public void syncFromLocalToRemoteToLocalThenDeleteFileAndSyncAgain() throws Exception {
		syncFromLocalToRemoteToLocal();

		// SsDelete file /2/a in local source repository and sync this deletion.
		final File child_2 = createFile(localSrcRoot, "2");
		final File child_2_a = createFile(child_2, "a");
		assertThat(child_2_a.exists()).isTrue();

		child_2_a.delete();

		assertThat(child_2_a.exists()).isFalse();

		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest();
	}

//	@Override
//	protected void populateLocalSourceRepo() throws Exception {
//		final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);
//
////		final File child_1 = createDirectory(localSrcRoot, "1 {11 11ä11} 1");
////
////		createFileWithRandomContent(child_1, "a");
////		createFileWithRandomContent(child_1, "b");
////		createFileWithRandomContent(child_1, "c");
//
//		final File child_2 = createDirectory(localSrcRoot, "2");
//
//		createFileWithRandomContent(child_2, "a");
//
////		final File child_2_1 = createDirectory(child_2, "1 {11 11ä11} 1");
////		createFileWithRandomContent(child_2_1, "a");
////		createFileWithRandomContent(child_2_1, "b");
////
////		final File child_3 = createDirectory(localSrcRoot, "3 + &#ä");
////
////		createFileWithRandomContent(child_3, "aa");
////		createFileWithRandomContent(child_3, "bb");
////		createFileWithRandomContent(child_3, "cc");
////		createFileWithRandomContent(child_3, "dd");
////
////		final File child_3_5 = createDirectory(child_3, "5");
////		createFileWithRandomContent(child_3_5, "h");
////		createFileWithRandomContent(child_3_5, "i");
//
//		localRepoManagerLocal.localSync(new LoggerProgressMonitor(logger));
//		localRepoManagerLocal.close();
//	}

	@Ignore("Still working on this - collisions are still not supported!")
	@Test
	public void syncFromLocalToRemoteToLocalThenCauseDeleteCollisionOnServerDuringUpSync() throws Exception {
		syncFromLocalToRemoteToLocal();

		syncFromRemoteToLocalDest(false);
		syncFromRemoteToLocalDest(false);
		syncFromRemoteToLocalDest(false);

		// 2 modifications:
		// 1) change file /2/a in local destination repo and sync
		File child_2 = createFile(localDestRoot, "2");
		File child_2_a = createFile(child_2, "a");
		assertThat(child_2_a.exists()).isTrue();

		child_2_a.delete(); createFileWithRandomContent(child_2_a);
		syncFromRemoteToLocalDest(false);

		// 2) delete file /2/a in local source repo and sync
		child_2 = createFile(localSrcRoot, "2");
		child_2_a = createFile(child_2, "a");
		assertThat(child_2_a.exists()).isTrue();

		child_2_a.delete();

		assertThat(child_2_a.exists()).isFalse();

		syncFromLocalSrcToRemote();
		syncFromRemoteToLocalDest();
	}

	@Test
	public void syncFromLocalToRemoteToLocalWithPathPrefix() throws Exception {
		remotePathPrefix2Plain = "/3 + &#ä";
		syncFromLocalToRemoteToLocal();
	}

	@Test
	public void multiSyncFromLocalToRemoteToLocalWithPathPrefixWithSubdirClearanceKey() throws Exception {
		remotePathPrefix2Plain = "/3 + &#ä";

		createLocalSourceAndRemoteRepo();

		// Opening localRepoManagerLocal to make sure it's not discarded while test is running.
		// This caused occasional build errors - especially on slower machines - when creating files
		// 'yyyyyyyy' etc. below.
		try (final LocalRepoManager localRepoManagerLocal = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(localSrcRoot);)
		{
			populateLocalSourceRepo();
			syncFromLocalSrcToRemote();
			determineRemotePathPrefix2Encrypted();

			final UserRepoKeyRing otherUserRepoKeyRing = createUserRepoKeyRing();
			grantPermission("/", PermissionType.read,
					otherUserRepoKeyRing.getPermanentUserRepoKeys(remoteRepositoryId).get(0).getPublicKey());

			createFileWithRandomContent(localSrcRoot, "xxxxxxx");

			final File child_2 = createFile(localSrcRoot, "2");
			createFile(child_2, "a").delete();
			createFileWithRandomContent(child_2, "a"); // overwrite
			createFileWithRandomContent(child_2, "yyyyyyyy"); // new file

			final File child_3 = createFile(localSrcRoot, remotePathPrefix2Plain);
			createFile(child_3, "b").delete();
			createFileWithRandomContent(child_3, "b"); // overwrite
			createFileWithRandomContent(child_3, "zzzzzzz"); // new file

			syncFromLocalSrcToRemote();

			final UserRepoKeyRing ownerUserRepoKeyRing = getUserRepoKeyRing(cryptreeRepoTransportFactory);
			assertThat(ownerUserRepoKeyRing).isNotNull();
			try {
				cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(otherUserRepoKeyRing));
				createLocalDestinationRepo();
				syncFromRemoteToLocalDest();
			} finally {
				cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(ownerUserRepoKeyRing));
			}

			createFile(child_2, "yyyyyyyy").delete();
			createFileWithRandomContent(child_2, "yyyyyyyy"); // overwrite
			createFileWithRandomContent(child_2, "ttttt"); // new file

			createFile(child_3, "cc").delete();
			createFileWithRandomContent(child_3, "cc"); // overwrite
			createFileWithRandomContent(child_3, "kkkkk"); // new file

			syncFromLocalSrcToRemote();

			try {
				cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(otherUserRepoKeyRing));
				syncFromRemoteToLocalDest();
			} finally {
				cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(ownerUserRepoKeyRing));
			}
		}
	}

	@Test
	public void syncFromLocalToRemoteToLocalWithPathPrefixWithSubdirClearanceKey() throws Exception {
		remotePathPrefix2Plain = "/3 + &#ä";

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();

		final UserRepoKeyRing otherUserRepoKeyRing1 = createUserRepoKeyRing();
		final UserRepoKeyRing otherUserRepoKeyRing2 = createUserRepoKeyRing();
		final PublicKey publicKey1 = otherUserRepoKeyRing1.getPermanentUserRepoKeys(remoteRepositoryId).get(0).getPublicKey();
		grantPermission(remotePathPrefix2Plain, PermissionType.read, publicKey1);
		grantPermission(remotePathPrefix2Plain, PermissionType.read,
				otherUserRepoKeyRing2.getPermanentUserRepoKeys(remoteRepositoryId).get(0).getPublicKey());

		syncFromLocalSrcToRemote();

		final UserRepoKeyRing ownerUserRepoKeyRing = getUserRepoKeyRing(cryptreeRepoTransportFactory);
		assertThat(ownerUserRepoKeyRing).isNotNull();
		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(otherUserRepoKeyRing1));
			createLocalDestinationRepo();
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(ownerUserRepoKeyRing));
		}

		revokePermission(remotePathPrefix2Plain, PermissionType.read, publicKey1);
		syncFromLocalSrcToRemote();

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(otherUserRepoKeyRing1));
			createLocalDestinationRepo();

			// The following sync should still work, because the original files/dirs were not yet modified
			// and revoking the access rights therefore does not yet affect anything. This is called
			// lazy revocation.
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(ownerUserRepoKeyRing));
		}

		// Now, we modify the directory locally and sync it up again. This should cause the next down-sync
		// with the revoked key to fail.
		getLocalRootWithPathPrefix().setLastModified(getLocalRootWithPathPrefix().lastModified() + 3000);
		syncFromLocalSrcToRemote();

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(otherUserRepoKeyRing1));
			createLocalDestinationRepo();
			try {
				syncFromRemoteToLocalDest();
				Assert.fail("Could still check-out after access rights were revoked!");
			} catch (final AccessDeniedException x) {
				logger.info("Fine! Expected this AccessDeniedException: " + x);
			}

			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(otherUserRepoKeyRing2));
			createLocalDestinationRepo();

			logger.info("");
			logger.info("");
			logger.info("");
			logger.info("*** before syncFromRemoteToLocalDest() ***");
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(ownerUserRepoKeyRing));
		}
	}

	@Test
	public void syncFromLocalToRemoteToLocalWithPathPrefixWithWritePermissionGrantedAndRevoked() throws Exception {
		remotePathPrefix2Plain = "/3 + &#ä";

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();

		final UserRepoKeyRing otherUserRepoKeyRing1 = createUserRepoKeyRing();
		final PublicKey publicKey1 = otherUserRepoKeyRing1.getPermanentUserRepoKeys(remoteRepositoryId).get(0).getPublicKey();
//		grantPermission(remotePathPrefix2Plain, PermissionType.read, publicKey1); // write includes read => this line is not needed

		grantPermission(remotePathPrefix2Plain, PermissionType.write, publicKey1);

		syncFromLocalSrcToRemote();

		final UserRepoKeyRing ownerUserRepoKeyRing = getUserRepoKeyRing(cryptreeRepoTransportFactory);
		assertThat(ownerUserRepoKeyRing).isNotNull();
		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(otherUserRepoKeyRing1));
			createLocalDestinationRepo();
			syncFromRemoteToLocalDest();

			final File child_3 = createFile(localSrcRoot, remotePathPrefix2Plain);
			createFile(child_3, "bb").delete();
			createFileWithRandomContent(child_3, "bb"); // overwrite
			createFileWithRandomContent(child_3, "zzzzzzz"); // new file

			syncFromLocalSrcToRemote();
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(ownerUserRepoKeyRing));
		}

		revokePermission(remotePathPrefix2Plain, PermissionType.write, publicKey1);

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(otherUserRepoKeyRing1));

			// The following write should still work, because the revocation becomes active only in the next up-sync.
			final File child_3 = createFile(localSrcRoot, remotePathPrefix2Plain);
			createFile(child_3, "bb").delete();
			createFileWithRandomContent(child_3, "bb"); // overwrite
			createFileWithRandomContent(child_3, "abczzzz"); // new file

			// Because the user is not allowed to enact the revocation, this does not yet have any
			// effect on setting Permission.validTo.
			syncFromLocalSrcToRemote();
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(ownerUserRepoKeyRing));
		}

		// We enact the revocation in the other repository - this should work fine ;-)
		syncFromRemoteToLocalDest();

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(otherUserRepoKeyRing1));

			// And now, the next write(s) should fail, because the revocation should now be active.
			final File child_3 = createFile(localSrcRoot, remotePathPrefix2Plain);
			createFile(child_3, "bb").delete();
			createFileWithRandomContent(child_3, "bb"); // overwrite

			try {
				syncFromLocalSrcToRemote();
				Assert.fail("Could still write after access rights were revoked!");
			} catch (final WriteAccessDeniedException x) {
				logger.info("Fine! Expected this WriteAccessDeniedException: " + x);
			}

			createFileWithRandomContent(child_3, "abcyyyy"); // new file
			try {
				syncFromLocalSrcToRemote();
				Assert.fail("Could still write after access rights were revoked!");
			} catch (final WriteAccessDeniedException x) {
				logger.info("Fine! Expected this WriteAccessDeniedException: " + x);
			}

		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(ownerUserRepoKeyRing));
		}
	}

	@Test
	public void syncFromLocalToRemoteToLocalWithPathPrefixWithoutSubdirClearanceKey() throws Exception {
		remotePathPrefix2Plain = "/3 + &#ä";

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();

		final UserRepoKeyRing otherUserRepoKeyRing = createUserRepoKeyRing();

		final UserRepoKeyRing ownerUserRepoKeyRing = getUserRepoKeyRing(cryptreeRepoTransportFactory);
		assertThat(ownerUserRepoKeyRing).isNotNull();

		// Do *not* grant read access to the sub-dir! It must fail.
		// However, we must persist the public keys to the server repository for the server to allow
		// the repository-connection (it is now implicitly done immediately during the request).
		persistPublicKeysToRemoteRepository(otherUserRepoKeyRing);

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(otherUserRepoKeyRing));
			createLocalDestinationRepo();

			try {
				syncFromRemoteToLocalDest();
				Assert.fail("ReadAccessDeniedException was *not* thrown! It should have been!");
			} catch (final ReadAccessDeniedException x) {
				logger.info("syncFromLocalToRemoteToLocalWithPathPrefixWithoutSubdirClearanceKey: Caught ReadAccessDeniedException as expected.");
			}
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRingLookup(new StaticUserRepoKeyRingLookup(ownerUserRepoKeyRing));
		}
	}

	private void persistPublicKeysToRemoteRepository(UserRepoKeyRing userRepoKeyRing) {
		try (final LocalRepoManager localRepoManager = localRepoManagerFactory.createLocalRepoManagerForExistingRepository(remoteRoot);) {
			final List<UserRepoKey> allUserRepoKeys = userRepoKeyRing.getUserRepoKeys(localRepoManager.getRepositoryId());

			try (final LocalRepoTransaction transaction = localRepoManager.beginWriteTransaction();) {
				final UserRepoKeyPublicKeyDao userRepoKeyPublicKeyDao = transaction.getDao(UserRepoKeyPublicKeyDao.class);
				for (final UserRepoKey userRepoKey : allUserRepoKeys) {
//					userRepoKeyPublicKeyDao.getUserRepoKeyPublicKeyOrCreate(userRepoKey.getPublicKey());

					if (userRepoKey.isInvitation()) {
						final UserRepoKey.PublicKeyWithSignature publicKeyWithSignature = userRepoKey.getPublicKey();
						userRepoKeyPublicKeyDao.makePersistent(new InvitationUserRepoKeyPublicKey(publicKeyWithSignature));
					}
					else
						userRepoKeyPublicKeyDao.makePersistent(new UserRepoKeyPublicKey(userRepoKey.getPublicKey()));
				}
				transaction.commit();
			}
		}
	}
}
