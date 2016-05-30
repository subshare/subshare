package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.AccessDeniedException;
import org.subshare.core.Cryptree;
import org.subshare.core.CryptreeFactoryRegistry;
import org.subshare.core.LocalRepoStorage;
import org.subshare.core.LocalRepoStorageFactoryRegistry;
import org.subshare.core.dto.CollisionDto;
import org.subshare.core.dto.CryptoRepoFileDto;
import org.subshare.core.dto.HistoFrameDto;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.repo.local.CollisionFilter;
import org.subshare.core.repo.local.HistoFrameFilter;
import org.subshare.core.repo.local.PlainHistoCryptoRepoFileFilter;
import org.subshare.core.repo.local.SsLocalRepoMetaData;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.core.user.UserRepoKeyRingLookup;
import org.subshare.core.user.UserRepoKeyRingLookupContext;
import org.subshare.local.dto.CollisionDtoConverter;
import org.subshare.local.dto.CryptoRepoFileDtoConverter;
import org.subshare.local.dto.HistoFrameDtoConverter;
import org.subshare.local.persistence.Collision;
import org.subshare.local.persistence.CollisionDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.HistoFrame;
import org.subshare.local.persistence.HistoFrameDao;
import org.subshare.local.persistence.ScheduledReupload;
import org.subshare.local.persistence.ScheduledReuploadDao;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoManager;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.LocalRepoMetaDataImpl;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class SsLocalRepoMetaDataImpl extends LocalRepoMetaDataImpl implements SsLocalRepoMetaData {

	private static final Logger logger = LoggerFactory.getLogger(SsLocalRepoMetaDataImpl.class);

	private UUID remoteRepositoryId;
	private URL remoteRoot;

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
			final Uid cryptoRepoFileId = cryptree.getCryptoRepoFileIdOrFail(localPath);
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

	@Override
	public void scheduleReupload(String localPath) {
		logger.debug("scheduleReupload: localPath='{}' ", localPath);
		try (final LocalRepoTransaction tx = beginWriteTransaction();) {
			final File localRoot = tx.getLocalRepoManager().getLocalRoot();
			final File file = localRoot.createFile(localPath);
			final RepoFile repoFile = tx.getDao(RepoFileDao.class).getRepoFile(localRoot, file);
			if (repoFile != null) {
				ScheduledReuploadDao srDao = tx.getDao(ScheduledReuploadDao.class);
				ScheduledReupload scheduledReupload = srDao.getScheduledReupload(repoFile);
				if (scheduledReupload == null) {
					scheduledReupload = new ScheduledReupload();
					scheduledReupload.setRepoFile(repoFile);
				}
				srDao.makePersistent(scheduledReupload);
			}
			else
				logger.warn("scheduleReupload: localRoot='{}' localPath='{}' ignored, becaue RepoFile not found!", localRoot.getPath(), localPath);

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

	protected UUID getRemoteRepositoryId(final LocalRepoTransaction tx) {
		assertNotNull("tx", tx);
		UUID result = remoteRepositoryId;
		if (result == null) {
			final RemoteRepositoryDao remoteRepositoryDao = tx.getDao(RemoteRepositoryDao.class);
			final Iterator<RemoteRepository> iterator = remoteRepositoryDao.getObjects().iterator();
			if (! iterator.hasNext())
				throw new IllegalStateException("There is no RemoteRepository!");

			final RemoteRepository remoteRepository = iterator.next();
			final URL remoteRoot = remoteRepository.getRemoteRoot();
			result = remoteRepository.getRepositoryId();

			if (iterator.hasNext())
				throw new IllegalStateException("There is more than one RemoteRepository!");

			this.remoteRepositoryId = result;
			this.remoteRoot = remoteRoot;
		}
		return result;
	}

	protected URL getRemoteRoot(final LocalRepoTransaction tx) {
		assertNotNull("tx", tx);
		URL result = remoteRoot;
		if (result == null) {
			getRemoteRepositoryId(tx);
			result = remoteRoot;
			if (result == null)
				throw new IllegalStateException("getRemoteRepositoryId(tx) did not assign remoteRoot!");
		}
		return result;
	}

	@Override
	public UUID getRemoteRepositoryId() {
		UUID result = remoteRepositoryId;
		if (result == null) {
			try (final LocalRepoTransaction tx = getLocalRepoManagerOrFail().beginReadTransaction();) {
				result = getRemoteRepositoryId(tx);
			}
			remoteRepositoryId = result;
		}
		return result;
	}

	@Override
	public URL getRemoteRoot() {
		URL result = remoteRoot;
		if (result == null) {
			getRemoteRepositoryId();
			result = remoteRoot;
			if (result == null)
				throw new IllegalStateException("getRemoteRepositoryId() did not assign remoteRoot!");
		}
		return result;
	}

	@Override
	public boolean isMetaOnly() {
		try (final LocalRepoTransaction tx = getLocalRepoManagerOrFail().beginReadTransaction();) {
			final LocalRepoStorage lrs = LocalRepoStorageFactoryRegistry.getInstance().getLocalRepoStorageFactoryOrFail().getLocalRepoStorageOrCreate(tx);
//			final UUID serverRepositoryId = getRemoteRepositoryId(tx);
//			final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(tx, serverRepositoryId);
			return lrs.isMetaOnly();
		}
	}

	@Override
	public void makeMetaOnly() {
		try (final LocalRepoTransaction tx = getLocalRepoManagerOrFail().beginWriteTransaction();) {
			final LocalRepoStorage lrs = LocalRepoStorageFactoryRegistry.getInstance().getLocalRepoStorageFactoryOrFail().getLocalRepoStorageOrCreate(tx);
//			final UUID serverRepositoryId = getRemoteRepositoryId(tx);
//			final Cryptree cryptree = CryptreeFactoryRegistry.getInstance().getCryptreeFactoryOrFail().getCryptreeOrCreate(tx, serverRepositoryId);
			lrs.makeMetaOnly();

//			// We must remove the Cryptree from the transaction, because this Cryptree thinks, it was on the server-side.
//			// It does this, because we do not provide a UserRepoKeyRing (which usually never happens on the client-side).
//			// This wrong assumption causes the VerifySignableAndWriteProtectedEntityListener to fail.
//			tx.removeContextObject(cryptree);
			tx.commit();
		}
	}

	@Override
	public Collection<HistoFrameDto> getHistoFrameDtos(final HistoFrameFilter filter) {
		assertNotNull("filter", filter);
		final LocalRepoManager localRepoManager = getLocalRepoManagerOrFail();
		try (final LocalRepoTransaction tx = localRepoManager.beginReadTransaction();) {
			final HistoFrameDao hfDao = tx.getDao(HistoFrameDao.class);
			final Collection<HistoFrame> histoFrames = hfDao.getHistoFrames(filter);
			final List<HistoFrameDto> result = new ArrayList<>(histoFrames.size());
			final HistoFrameDtoConverter converter = HistoFrameDtoConverter.create(tx);
			for (final HistoFrame histoFrame : histoFrames) {
				final HistoFrameDto histoFrameDto = converter.toHistoFrameDto(histoFrame);
				result.add(histoFrameDto);
			}
			return result;
		}
	}

	@Override
	public Collection<PlainHistoCryptoRepoFileDto> getPlainHistoCryptoRepoFileDtos(PlainHistoCryptoRepoFileFilter filter) {
		assertNotNull("filter", filter);
		try (final LocalRepoTransaction tx = getLocalRepoManagerOrFail().beginReadTransaction();) {
			return getCryptree(tx).getPlainHistoCryptoRepoFileDtos(filter);
		}
	}

	@Override
	public Collection<CollisionDto> getCollisionDtos(CollisionFilter filter) {
		assertNotNull("filter", filter);
		try (final LocalRepoTransaction tx = getLocalRepoManagerOrFail().beginReadTransaction();) {
			final CollisionDao cDao = tx.getDao(CollisionDao.class);
			final Collection<Collision> collisions = cDao.getCollisions(filter);
			final List<CollisionDto> result = new ArrayList<>(collisions.size());
			final CollisionDtoConverter converter = CollisionDtoConverter.create(tx);
			for (final Collision collision : collisions) {
				final CollisionDto collisionDto = converter.toCollisionDto(collision);
				result.add(collisionDto);
			}
			return result;
		}
	}
}
