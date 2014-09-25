package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static mockit.Deencapsulation.*;
import static org.assertj.core.api.Assertions.*;

import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.crypto.CryptoConfigUtil;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.CryptoKeyType;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoLink;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.oio.File;

public class PermissionIT extends AbstractRepoToRepoSyncIT {
	private static final Logger logger = LoggerFactory.getLogger(PermissionIT.class);

	@Override
	public void before() {
		super.before();
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + CryptoConfigUtil.CONFIG_KEY_BACKDATING_MAX_PERMISSION_VALID_TO_AGE, "15000");
	}

	@Override
	public void after() {
		super.after();
		System.clearProperty(Config.SYSTEM_PROPERTY_PREFIX + CryptoConfigUtil.CONFIG_KEY_BACKDATING_MAX_PERMISSION_VALID_TO_AGE);
	}

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

	private Date forceSignatureCreated;

	private boolean isInClient() {
		for (final StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
			if (stackTraceElement.getClassName().contains(".jetty."))
				return false;
		}
		return true;
	}

	@Test
	public void uploadBackdatedSignature() throws Exception {
		new MockUp<Date>() {
			@Mock
			public void $init(final Invocation invocation) {
				invocation.proceed();
				final Date date = invocation.getInvokedInstance();
				if (forceSignatureCreated != null && isInClient())
					setField(date, "fastTime", forceSignatureCreated.getTime());
				else
					setField(date, "fastTime", System.currentTimeMillis());
			}
		};

		createLocalSourceAndRemoteRepo();
		populateLocalSourceRepo();
		syncFromLocalSrcToRemote();
		determineRemotePathPrefix2Encrypted();

		final UserRepoKeyRing otherUserRepoKeyRing1 = createUserRepoKeyRing();
		final PublicKey publicKey1 = otherUserRepoKeyRing1.getUserRepoKeys(remoteRepositoryId).get(0).getPublicKey();

		grantPermission("/", PermissionType.write, publicKey1);
		syncFromLocalSrcToRemote();

		final UserRepoKeyRing ownerUserRepoKeyRing = cryptreeRepoTransportFactory.getUserRepoKeyRing();
		assertThat(ownerUserRepoKeyRing).isNotNull();
		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);
			createLocalDestinationRepo();
			syncFromRemoteToLocalDest();

			createFileWithRandomContent(localDestRoot, "new-file1");

			syncFromRemoteToLocalDest(false);
		} finally {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(ownerUserRepoKeyRing);
		}

		final Date timestampBeforeRevokingWritePermission = new Date();

		revokePermission("/", PermissionType.write, publicKey1);
		syncFromLocalSrcToRemote();

		Thread.sleep(10);

		try {
			cryptreeRepoTransportFactory.setUserRepoKeyRing(otherUserRepoKeyRing1);
			syncFromRemoteToLocalDest(false);

			final File file = createFileWithRandomContent(localDestRoot, "new-file2");

			try {
				syncFromRemoteToLocalDest(false);
				fail("This should have failed!");
			} catch (final WriteAccessDeniedException x) {
				doNothing();
			}

			file.setLastModified(timestampBeforeRevokingWritePermission.getTime() - 10000); // this could be anything and serves only to re-trigger the local sync.
			forceSignatureCreated = timestampBeforeRevokingWritePermission;

			// backdating should still be allowed (within time-range)
			syncFromRemoteToLocalDest(false);

			Thread.sleep(15000);

			final OutputStream out = file.createOutputStream();
			out.write(123);
			out.close();
			file.setLastModified(timestampBeforeRevokingWritePermission.getTime() - 5000); // this could be anything and serves only to re-trigger the local sync.

			try {
				syncFromRemoteToLocalDest(false);
				fail("Backdating was not detected by the server!");
			} catch (final WriteAccessDeniedException x) {
				logger.debug("Detected ", x);
				doNothing();
			}
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
