package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.subshare.core.AccessDeniedException;
import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.core.user.UserRepoKeyRingLookup;
import org.subshare.core.user.UserRepoKeyRingLookupContext;
import org.subshare.local.dto.CryptoRepoFileDtoConverter;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.LocalRepoMetaDataImpl;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class SsLocalRepoMetaDataImpl extends LocalRepoMetaDataImpl implements SsLocalRepoMetaData {

	private UUID remoteRepositoryId;

//	@Override
//	public CryptoRepoFileDto getCryptoRepoFileDto(final String localPath) {
//		assertNotNull("path", localPath);
//
//		final CryptoRepoFileDto result;
//		try (final LocalRepoTransaction tx = beginReadTransaction();) {
//			final CryptoRepoFileDtoConverter converter = CryptoRepoFileDtoConverter.create();
//			final CryptoRepoFile cryptoRepoFile = tx.getDao(CryptoRepoFileDao.class).getCryptoRepoFile(localPath);
//
//			result = cryptoRepoFile == null ? null : converter.toCryptoRepoFileDto(cryptoRepoFile);
//
//			tx.commit();
//		}
//		return result;
//	}

	@Override
	public CryptoRepoFileDto getCryptoRepoFileDto(final long repoFileId) {
		final CryptoRepoFileDto result;
		try (final LocalRepoTransaction tx = beginReadTransaction();) {
			final CryptoRepoFileDtoConverter converter = CryptoRepoFileDtoConverter.create();
			final RepoFile repoFile = tx.getDao(RepoFileDao.class).getObjectByIdOrNull(repoFileId);
			final CryptoRepoFile cryptoRepoFile =
					repoFile == null ? null : tx.getDao(CryptoRepoFileDao.class).getCryptoRepoFile(repoFile);

			result = cryptoRepoFile == null ? null : converter.toCryptoRepoFileDto(cryptoRepoFile);

			tx.commit();
		}
		return result;
	}

	@Override
	public Map<Long, CryptoRepoFileDto> getCryptoRepoFileDtos(final Collection<Long> repoFileIds) {
		assertNotNull("repoFileIds", repoFileIds);
		final Map<Long, CryptoRepoFileDto> result = new LinkedHashMap<>();
		try (final LocalRepoTransaction tx = beginReadTransaction();) {
			final RepoFileDao repoFileDao = tx.getDao(RepoFileDao.class);
			final CryptoRepoFileDao cryptoRepoFileDao = tx.getDao(CryptoRepoFileDao.class);
			final CryptoRepoFileDtoConverter converter = CryptoRepoFileDtoConverter.create();
			for (final Long repoFileId : repoFileIds) {
				assertNotNull("repoFileId", repoFileId);
				final RepoFile repoFile = repoFileDao.getObjectByIdOrNull(repoFileId);
				if (repoFile == null)
					continue;

				final CryptoRepoFile cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFile(repoFile);
				if (cryptoRepoFile == null)
					continue;

				result.put(repoFileId, converter.toCryptoRepoFileDto(cryptoRepoFile));
			}
			tx.commit();
		}
		return result;
	}

	@Override
	public boolean isPermissionsInherited(final String localPath) {
		try (final LocalRepoTransaction tx = beginReadTransaction();) {
			final Cryptree cryptree = getCryptree(tx);
			final boolean result = cryptree.isPermissionsInherited(localPath);
			tx.commit();
			return result;
		}
	}

	@Override
	public void setPermissionsInherited(final String localPath, final boolean inherited) {
		try (final LocalRepoTransaction tx = beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(tx);
			cryptree.setPermissionsInherited(localPath, inherited);
			tx.commit();
		}
	}

	@Override
	public Uid getOwnerUserRepoKeyId() {
		try (final LocalRepoTransaction tx = beginReadTransaction();) {
			final Cryptree cryptree = getCryptree(tx);
			final Uid result = cryptree.getOwnerUserRepoKeyId();
			tx.commit();
			return result;
		}
	}

	@Override
	public Set<PermissionType> getGrantedPermissionTypes(String localPath, Uid userRepoKeyId) {
		try (final LocalRepoTransaction tx = beginReadTransaction();) {
			final Cryptree cryptree = getCryptree(tx);
			final Set<PermissionType> result = cryptree.getGrantedPermissionTypes(localPath, userRepoKeyId);
			tx.commit();
			return result;
		}
	}

	@Override
	public Set<PermissionType> getEffectivePermissionTypes(String localPath, final Uid userRepoKeyId) {
		final Set<PermissionType> result = EnumSet.noneOf(PermissionType.class);
		try (final LocalRepoTransaction tx = beginReadTransaction();) {
			final Cryptree cryptree = getCryptree(tx);
			for (PermissionType permissionType : PermissionType.values()) {
				try {
					cryptree.assertHasPermission(localPath, userRepoKeyId, permissionType, new Date());
					result.add(permissionType);
				} catch (AccessDeniedException x) { doNothing(); }
			}
			tx.commit();
		}
		return Collections.unmodifiableSet(result);
	}

	@Override
	public Set<PermissionType> getInheritedPermissionTypes(String localPath, final Uid userRepoKeyId) {
		final Set<PermissionType> result = EnumSet.noneOf(PermissionType.class);
		try (final LocalRepoTransaction tx = beginReadTransaction();) {
			final Cryptree cryptree = getCryptree(tx);
			final Uid cryptoRepoFileId = cryptree.getCryptoRepoFileId(localPath);
			final Uid parentCryptoRepoFileId = cryptree.getParentCryptoRepoFileId(cryptoRepoFileId);
			if (parentCryptoRepoFileId == null)
				return Collections.emptySet();

			for (PermissionType permissionType : PermissionType.values()) {
				try {
					cryptree.assertHasPermission(parentCryptoRepoFileId, userRepoKeyId, permissionType, new Date());
					result.add(permissionType);
				} catch (AccessDeniedException x) { doNothing(); }
			}
			tx.commit();
		}
		return Collections.unmodifiableSet(result);
	}

	@Override
	public void revokePermission(String localPath, PermissionType permissionType, final Set<Uid> userRepoKeyIds) {
		try (final LocalRepoTransaction tx = beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(tx);
			cryptree.revokePermission(localPath, permissionType, userRepoKeyIds);
			tx.commit();
		}
	}

	@Override
	public void grantPermission(String localPath, PermissionType permissionType, UserRepoKey.PublicKey publicKey) {
		try (final LocalRepoTransaction tx = beginWriteTransaction();) {
			final Cryptree cryptree = getCryptree(tx);
			cryptree.grantPermission(localPath, permissionType, publicKey);
			tx.commit();
		}
	}

//	@Override
//	public boolean hasPermission(final String localPath, final Uid userRepoKeyId, final PermissionType permissionType) {
//		assertNotNull("localPath", localPath);
//		assertNotNull("userRepoKeyId", userRepoKeyId);
//		assertNotNull("permissionType", permissionType);
//		try (final LocalRepoTransaction tx = beginReadTransaction();) { // *not* committing, because it's a read-only-operation, anyway.
//			final Cryptree cryptree = getCryptree(tx);
//			try {
//				cryptree.assertHasPermission(localPath, userRepoKeyId, permissionType, new Date());
//				return true;
//			} catch (AccessDeniedException x) {
//				return false;
//			}
//		}
//	}

	protected Cryptree getCryptree(LocalRepoTransaction tx) {
		final UUID localRepositoryId = tx.getLocalRepoManager().getRepositoryId();
		final UUID remoteRepositoryId = getRemoteRepositoryId(tx);
		final String remotePathPrefix = ""; //$NON-NLS-1$

		final UserRepoKeyRing userRepoKeyRing = UserRepoKeyRingLookup.Helper.getUserRepoKeyRingLookup().getUserRepoKeyRing(
				new UserRepoKeyRingLookupContext(localRepositoryId, remoteRepositoryId));

		final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(
				tx, remoteRepositoryId, remotePathPrefix, userRepoKeyRing);

		return cryptree;
	}

	protected UUID getRemoteRepositoryId(LocalRepoTransaction tx) {
		UUID result = remoteRepositoryId;
		if (result == null) {
			final RemoteRepositoryDao remoteRepositoryDao = tx.getDao(RemoteRepositoryDao.class);
			final Iterator<RemoteRepository> iterator = remoteRepositoryDao.getObjects().iterator();
			if (! iterator.hasNext())
				throw new IllegalStateException("There is no RemoteRepository!");

			result = iterator.next().getRepositoryId();

			if (iterator.hasNext())
				throw new IllegalStateException("There is more than one RemoteRepository!");

			remoteRepositoryId = result;
		}
		return result;
	}

	@Override
	public boolean isMetaOnly() {
		try (final LocalRepoTransaction tx = getLocalRepoManagerOrFail().beginReadTransaction();) {
			final UUID serverRepositoryId = getRemoteRepositoryId(tx);
			final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(tx, serverRepositoryId);
			return cryptree.isMetaOnly();
		}
	}

	@Override
	public void makeMetaOnly() {
		try (final LocalRepoTransaction tx = getLocalRepoManagerOrFail().beginWriteTransaction();) {
			final UUID serverRepositoryId = getRemoteRepositoryId(tx);
			final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(tx, serverRepositoryId);
			cryptree.makeMetaOnly();

			// We must remove the Cryptree from the transaction, because this Cryptree thinks, it was on the server-side.
			// It does this, because we do not provide a UserRepoKeyRing (which usually never happens on the client-side).
			// This wrong assumption causes the VerifySignableAndWriteProtectedEntityListener to fail.
			tx.removeContextObject(cryptree);
			tx.commit();
		}
	}
}
