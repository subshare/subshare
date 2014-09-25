package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.junit.Test;

import co.codewizards.cloudstore.core.oio.File;

public class PermissionIT extends AbstractRepoToRepoSyncIT {

	@Test
	public void nonOwnerAdminGrantsWritePermission() throws Exception {
		remotePathPrefix2Plain = "/3 + &#ä";

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();

		final UserRepoKeyRing otherUserRepoKeyRing1 = createUserRepoKeyRing();
		final PublicKey publicKey1 = otherUserRepoKeyRing1.getUserRepoKeys(remoteRepositoryId).get(0).getPublicKey();
		grantPermission(remotePathPrefix2Plain, PermissionType.grant, publicKey1);

		syncFromLocalSrcToRemote();

		final UserRepoKeyRing otherUserRepoKeyRing2;
		final PublicKey publicKey2;

		final UserRepoKeyRing ownerUserRepoKeyRing = cryptreeRepoTransportFactory.getUserRepoKeyRing();
		assertThat(ownerUserRepoKeyRing).isNotNull();
		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);
			createLocalDestinationRepo();
			syncFromRemoteToLocalDest();

			otherUserRepoKeyRing2 = createUserRepoKeyRing();
			publicKey2 = otherUserRepoKeyRing2.getUserRepoKeys(remoteRepositoryId).get(0).getPublicKey();

			grantPermission(localDestRoot, "/", PermissionType.read, publicKey2);

			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}

		final File localDestRoot1 = localDestRoot;

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing2);
			createLocalDestinationRepo();
			syncFromRemoteToLocalDest();

			createFile(localDestRoot, "bb").delete();
			createFileWithRandomContent(localDestRoot, "bb"); // overwrite

			try {
				syncFromRemoteToLocalDest();
				fail("This should not have worked!");
			} catch (final WriteAccessDeniedException x) {
				doNothing();
			}
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}

		final File localDestRoot2 = localDestRoot;
		localDestRoot = localDestRoot1;

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);
			grantPermission(localDestRoot, "/", PermissionType.write, publicKey2);
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}

		localDestRoot = localDestRoot2;

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing2);
			createLocalDestinationRepo();
			syncFromRemoteToLocalDest();

			createFileWithRandomContent(localDestRoot, "yyaagjohdsfg");

			syncFromRemoteToLocalDest(false);
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}

		syncFromLocalSrcToRemote();

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing2);
			syncFromRemoteToLocalDest();
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}
	}

}
