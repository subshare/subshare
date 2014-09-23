package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static org.assertj.core.api.Assertions.*;

import org.subshare.core.AccessDeniedException;
import org.subshare.core.ReadAccessDeniedException;
import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;

public class RepoToRepoSyncIT extends AbstractRepoToRepoSyncIT {

	static final Logger logger = LoggerFactory.getLogger(RepoToRepoSyncIT.class);

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
					otherUserRepoKeyRing.getUserRepoKeys(remoteRepositoryId).get(0).getPublicKey());

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

			final UserRepoKeyRing ownerUserRepoKeyRing = cryptreeRepoTransportFactory.getUserRepoKeyRing();
			assertThat(ownerUserRepoKeyRing).isNotNull();
			try {
				cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing);
				createLocalDestinationRepo();
				syncFromRemoteToLocalDest();
			} finally {
				cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
			}

			createFile(child_2, "yyyyyyyy").delete();
			createFileWithRandomContent(child_2, "yyyyyyyy"); // overwrite
			createFileWithRandomContent(child_2, "ttttt"); // new file

			createFile(child_3, "cc").delete();
			createFileWithRandomContent(child_3, "cc"); // overwrite
			createFileWithRandomContent(child_3, "kkkkk"); // new file

			syncFromLocalSrcToRemote();

			try {
				cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing);
				syncFromRemoteToLocalDest();
			} finally {
				cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
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
		final PublicKey publicKey1 = otherUserRepoKeyRing1.getUserRepoKeys(remoteRepositoryId).get(0).getPublicKey();
		grantPermission(remotePathPrefix2Plain, PermissionType.read, publicKey1);
		grantPermission(remotePathPrefix2Plain, PermissionType.read,
				otherUserRepoKeyRing2.getUserRepoKeys(remoteRepositoryId).get(0).getPublicKey());

		syncFromLocalSrcToRemote();

		final UserRepoKeyRing ownerUserRepoKeyRing = cryptreeRepoTransportFactory.getUserRepoKeyRing();
		assertThat(ownerUserRepoKeyRing).isNotNull();
		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);
			createLocalDestinationRepo();
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}

		revokePermission(remotePathPrefix2Plain, PermissionType.read, publicKey1);
		syncFromLocalSrcToRemote();

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);
			createLocalDestinationRepo();

			// The following sync should still work, because the original files/dirs were not yet modified
			// and revoking the access rights therefore does not yet affect anything. This is called
			// lazy revocation.
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}

		// Now, we modify the directory locally and sync it up again. This should cause the next down-sync
		// with the revoked key to fail.
		getLocalRootWithPathPrefix().setLastModified(getLocalRootWithPathPrefix().lastModified() + 3000);
		syncFromLocalSrcToRemote();

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);
			createLocalDestinationRepo();
			try {
				syncFromRemoteToLocalDest();
				Assert.fail("Could still check-out after access rights were revoked!");
			} catch (final AccessDeniedException x) {
				logger.info("Fine! Expected this AccessDeniedException: " + x);
			}

			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing2);
			createLocalDestinationRepo();

			logger.info("");
			logger.info("");
			logger.info("");
			logger.info("*** before syncFromRemoteToLocalDest() ***");
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
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
		final PublicKey publicKey1 = otherUserRepoKeyRing1.getUserRepoKeys(remoteRepositoryId).get(0).getPublicKey();
		grantPermission(remotePathPrefix2Plain, PermissionType.read, publicKey1);

		grantPermission(remotePathPrefix2Plain, PermissionType.write, publicKey1);

		syncFromLocalSrcToRemote();

		final UserRepoKeyRing ownerUserRepoKeyRing = cryptreeRepoTransportFactory.getUserRepoKeyRing();
		assertThat(ownerUserRepoKeyRing).isNotNull();
		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);
			createLocalDestinationRepo();
			syncFromRemoteToLocalDest();

			final File child_3 = createFile(localSrcRoot, remotePathPrefix2Plain);
			createFile(child_3, "bb").delete();
			createFileWithRandomContent(child_3, "bb"); // overwrite
			createFileWithRandomContent(child_3, "zzzzzzz"); // new file

			syncFromLocalSrcToRemote();
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}

		revokePermission(remotePathPrefix2Plain, PermissionType.write, publicKey1);

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);

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
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}

		// We enact the revocation in the other repository - this should work fine ;-)
		syncFromRemoteToLocalDest();

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);

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
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
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
		final UserRepoKeyRing ownerUserRepoKeyRing = cryptreeRepoTransportFactory.getUserRepoKeyRing();
		// Do *not* grant read access to the sub-dir! It must fail.
		assertThat(ownerUserRepoKeyRing).isNotNull();
		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing);
			createLocalDestinationRepo();

			try {
				syncFromRemoteToLocalDest();
				Assert.fail("ReadAccessDeniedException was *not* thrown! It should have been!");
			} catch (final ReadAccessDeniedException x) {
				logger.info("syncFromLocalToRemoteToLocalWithPathPrefixWithoutSubdirClearanceKey: Caught ReadAccessDeniedException as expected.");
			}
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}
	}


}
