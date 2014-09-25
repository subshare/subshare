package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.CryptoKeyType;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoLink;
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

		final long cryptoKeyCountTotalBeforeGrant = getCryptoKeyCount(localSrcRoot, null, null);
		final long cryptoKeyCountClearanceKeyBeforeGrant = getCryptoKeyCount(localSrcRoot, CryptoKeyRole.clearanceKey, null);
		final long cryptoKeyCountSubdirKeyBeforeGrant = getCryptoKeyCount(localSrcRoot, CryptoKeyRole.subdirKey, null);

		assertThat(getCryptoKeyCount(localSrcRoot, CryptoKeyRole.subdirKey, CryptoKeyType.asymmetric)).isZero();

		final long cryptoLinkCountFromUserRepoKeyToClearanceKeyBeforeGrant = getCryptoLinkCount(localSrcRoot, false, CryptoKeyRole.clearanceKey);

		grantPermission(remotePathPrefix2Plain, PermissionType.grant, publicKey1);

		// There should be exactly one new clearanceKey and one new subdirKey
		assertThat(getCryptoKeyCount(localSrcRoot, CryptoKeyRole.clearanceKey, null)).isEqualTo(cryptoKeyCountClearanceKeyBeforeGrant + 1);
		assertThat(getCryptoKeyCount(localSrcRoot, CryptoKeyRole.subdirKey, null)).isEqualTo(cryptoKeyCountSubdirKeyBeforeGrant + 1);
		final long cryptoKeyCountTotalAfterGrant1 = getCryptoKeyCount(localSrcRoot, null, null);
		assertThat(cryptoKeyCountTotalAfterGrant1).isEqualTo(cryptoKeyCountTotalBeforeGrant + 2);

		// Since the clearanceKey is new, there should be 2 new links: one for the owner and one for the new user (publicKey1).
		assertThat(getCryptoLinkCount(localSrcRoot, false, CryptoKeyRole.clearanceKey)).isEqualTo(cryptoLinkCountFromUserRepoKeyToClearanceKeyBeforeGrant + 2);

		assertThat(getCryptoKeyCount(localSrcRoot, CryptoKeyRole.subdirKey, CryptoKeyType.asymmetric)).isEqualTo(1);

		final long cryptoLinkCountTotalAfterGrant1 = getCryptoLinkCount(localSrcRoot, null, null);

		syncFromLocalSrcToRemote();

		assertThat(getCryptoKeyCount(localSrcRoot, null, null)).isEqualTo(cryptoKeyCountTotalAfterGrant1);
		assertThat(getCryptoLinkCount(localSrcRoot, null, null)).isEqualTo(cryptoLinkCountTotalAfterGrant1);

		// BEGIN These new invocations should have *no* effect whatsoever.
		grantPermission(remotePathPrefix2Plain, PermissionType.grant, publicKey1);
		assertThat(getCryptoKeyCount(localSrcRoot, null, null)).isEqualTo(cryptoKeyCountTotalAfterGrant1);
		assertThat(getCryptoLinkCount(localSrcRoot, null, null)).isEqualTo(cryptoLinkCountTotalAfterGrant1);

		grantPermission(remotePathPrefix2Plain, PermissionType.read, publicKey1);
		assertThat(getCryptoKeyCount(localSrcRoot, null, null)).isEqualTo(cryptoKeyCountTotalAfterGrant1);
		assertThat(getCryptoLinkCount(localSrcRoot, null, null)).isEqualTo(cryptoLinkCountTotalAfterGrant1);

		grantPermission(remotePathPrefix2Plain, PermissionType.write, publicKey1);
		assertThat(getCryptoKeyCount(localSrcRoot, null, null)).isEqualTo(cryptoKeyCountTotalAfterGrant1);
		assertThat(getCryptoLinkCount(localSrcRoot, null, null)).isEqualTo(cryptoLinkCountTotalAfterGrant1);
		// END These new invocations should have *no* effect whatsoever.

		final UserRepoKeyRing otherUserRepoKeyRing2;
		final PublicKey publicKey2;

		final long cryptoKeyCountTotalAfterGrant2 = cryptoKeyCountTotalAfterGrant1; // this should not change
		final long cryptoLinkCountTotalAfterGrant2;
		final UserRepoKeyRing ownerUserRepoKeyRing = cryptreeRepoTransportFactory.getUserRepoKeyRing();
		assertThat(ownerUserRepoKeyRing).isNotNull();
		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);
			createLocalDestinationRepo();
			syncFromRemoteToLocalDest();

			otherUserRepoKeyRing2 = createUserRepoKeyRing();
			publicKey2 = otherUserRepoKeyRing2.getUserRepoKeys(remoteRepositoryId).get(0).getPublicKey();

			grantPermission(localDestRoot, "/", PermissionType.read, publicKey2);

			// The clearanceKey already exists, thus this number should not change!
			assertThat(getCryptoKeyCount(localDestRoot, null, null)).isEqualTo(cryptoKeyCountTotalAfterGrant1);

			// However, there should be 1 new CryptoLinks to this existing clearanceKey.
			cryptoLinkCountTotalAfterGrant2 = getCryptoLinkCount(localDestRoot, null, null);
			assertThat(cryptoLinkCountTotalAfterGrant2).isEqualTo(cryptoLinkCountTotalAfterGrant1 + 1);

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

			// We already granted read access before, hence granting write access should not have any effect.
			assertThat(getCryptoKeyCount(localDestRoot, null, null)).isEqualTo(cryptoKeyCountTotalAfterGrant2);
			assertThat(getCryptoLinkCount(localDestRoot, null, null)).isEqualTo(cryptoLinkCountTotalAfterGrant2);

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

	private long getCryptoKeyCount(final File localRoot, final CryptoKeyRole cryptoKeyRole, final CryptoKeyType cryptoKeyType) {
		final PersistenceManager pm = getTransactionalPersistenceManager(localRoot);
		try {
			final Query q = pm.newQuery(CryptoKey.class);
			q.setResult("count(this)");

			final Map<String, Object> params = new HashMap<>();
			final StringBuilder filter = new StringBuilder();
			if (cryptoKeyRole != null) {
				appendWithAnd(filter, "this.cryptoKeyRole == :cryptoKeyRole");
				params.put("cryptoKeyRole", cryptoKeyRole);
			}

			if (cryptoKeyType != null) {
				appendWithAnd(filter, "this.cryptoKeyType == :cryptoKeyType");
				params.put("cryptoKeyType", cryptoKeyType);
			}

			q.setFilter(filter.toString());
			final Long result = (Long) q.executeWithMap(params);
			return result;
		} finally {
			pm.currentTransaction().rollback();
			pm.close();
		}
	}

	private void appendWithAnd(final StringBuilder filter, final String criterion) {
		if (filter.length() > 0)
			filter.append(" && ");

		filter.append(criterion);
	}

	private long getCryptoLinkCount(final File localRoot, final Boolean fromCryptoKey, final CryptoKeyRole toCryptoKeyRole) {
		final PersistenceManager pm = getTransactionalPersistenceManager(localRoot);
		try {
			final Query q = pm.newQuery(CryptoLink.class);
			q.setResult("count(this)");

			final Map<String, Object> params = new HashMap<>();
			final StringBuilder filter = new StringBuilder();
			if (toCryptoKeyRole != null) {
				appendWithAnd(filter, "this.toCryptoKey.cryptoKeyRole == :toCryptoKeyRole");
				params.put("toCryptoKeyRole", toCryptoKeyRole);
			}

			if (fromCryptoKey != null) {
				if (fromCryptoKey.booleanValue())
					appendWithAnd(filter, "this.fromCryptoKey != null");
				else
					appendWithAnd(filter, "this.fromUserRepoKeyPublicKey != null");
			}

			q.setFilter(filter.toString());
			final Long result = (Long) q.executeWithMap(params);
			return result;

		} finally {
			pm.currentTransaction().rollback();
			pm.close();
		}
	}

}
