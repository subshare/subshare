package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static org.subshare.local.CryptreeNodeUtil.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import javax.jdo.JDOHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.AccessDeniedException;
import org.subshare.core.DataKey;
import org.subshare.core.GrantAccessDeniedException;
import org.subshare.core.ReadAccessDeniedException;
import org.subshare.core.ReadUserIdentityAccessDeniedException;
import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.crypto.CryptoConfigUtil;
import org.subshare.core.dto.CollisionPrivateDto;
import org.subshare.core.dto.CryptoKeyPart;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;
import org.subshare.core.dto.SignatureDto;
import org.subshare.core.dto.SsRepoFileDto;
import org.subshare.core.repo.local.CollisionFilter;
import org.subshare.core.sign.Signable;
import org.subshare.core.sign.WriteProtected;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.crypto.CipherOperationMode;
import org.subshare.local.dto.CollisionDtoConverter;
import org.subshare.local.dto.CollisionPrivateDtoConverter;
import org.subshare.local.dto.HistoCryptoRepoFileDtoConverter;
import org.subshare.local.persistence.Collision;
import org.subshare.local.persistence.CollisionDao;
import org.subshare.local.persistence.CollisionPrivate;
import org.subshare.local.persistence.CollisionPrivateDao;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoKeyDeactivation;
import org.subshare.local.persistence.CryptoLink;
import org.subshare.local.persistence.CryptoLinkDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.CurrentHistoCryptoRepoFile;
import org.subshare.local.persistence.CurrentHistoCryptoRepoFileDao;
import org.subshare.local.persistence.HistoCryptoRepoFile;
import org.subshare.local.persistence.HistoCryptoRepoFileDao;
import org.subshare.local.persistence.HistoFrame;
import org.subshare.local.persistence.HistoFrameDao;
import org.subshare.local.persistence.InvitationUserRepoKeyPublicKey;
import org.subshare.local.persistence.Permission;
import org.subshare.local.persistence.PermissionDao;
import org.subshare.local.persistence.PermissionSet;
import org.subshare.local.persistence.PermissionSetDao;
import org.subshare.local.persistence.PermissionSetInheritance;
import org.subshare.local.persistence.PlainHistoCryptoRepoFile;
import org.subshare.local.persistence.PlainHistoCryptoRepoFileDao;
import org.subshare.local.persistence.PreliminaryDeletion;
import org.subshare.local.persistence.PreliminaryDeletionDao;
import org.subshare.local.persistence.RepositoryOwner;
import org.subshare.local.persistence.UserIdentityLink;
import org.subshare.local.persistence.UserIdentityLinkDao;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKeyDao;
import org.subshare.local.persistence.UserRepoKeyPublicKeyReplacementRequestDao;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.core.util.ISO8601;
import co.codewizards.cloudstore.local.dto.RepoFileDtoConverter;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.RepoFile;
import co.codewizards.cloudstore.local.persistence.RepoFileDao;

public class CryptreeNode {

	private static final Logger logger = LoggerFactory.getLogger(CryptreeNode.class);

	private static final Map<CryptoKeyRole, Class<? extends PlainCryptoKeyFactory>> cryptoKeyRole2PlainCryptoKeyFactory;
	static {
		final Map<CryptoKeyRole, Class<? extends PlainCryptoKeyFactory>> m = new HashMap<>(5);
		m.put(CryptoKeyRole.clearanceKey, PlainCryptoKeyFactory.ClearanceKeyPlainCryptoKeyFactory.class);
		m.put(CryptoKeyRole.subdirKey, PlainCryptoKeyFactory.SubdirKeyPlainCryptoKeyFactory.class);
		m.put(CryptoKeyRole.fileKey, PlainCryptoKeyFactory.FileKeyPlainCryptoKeyFactory.class);
		m.put(CryptoKeyRole.backlinkKey, PlainCryptoKeyFactory.BacklinkKeyPlainCryptoKeyFactory.class);
		m.put(CryptoKeyRole.dataKey, PlainCryptoKeyFactory.DataKeyPlainCryptoKeyFactory.class);
		cryptoKeyRole2PlainCryptoKeyFactory = Collections.unmodifiableMap(m);
	}

	private final CryptreeContext context;
	private CryptreeNode parent; // maybe null - lazily loaded, if there is one
	private RepoFile repoFile; // maybe null - lazily loaded
	private CryptoRepoFile cryptoRepoFile; // maybe null - lazily loaded
	private PermissionSet permissionSet; // maybe null - lazily loaded, if there is one
	private RepoFileDtoConverter repoFileDtoConverter; // maybe null - lazily loaded
	private final List<CryptreeNode> children = new ArrayList<CryptreeNode>(0);
	private boolean childrenLoaded = false;

	private final Set<Permission> permissionsBeingCheckedNow = new HashSet<Permission>();
	private final Set<Permission> permissionsAlreadyCheckedOk = new HashSet<Permission>();

	public CryptreeNode(final CryptreeContext context, final RepoFile repoFile) {
		this(context, repoFile, null);
	}

	public CryptreeNode(final CryptreeContext context, final CryptoRepoFile cryptoRepoFile) {
		this(context, null, cryptoRepoFile);
	}

	private CryptreeNode(final CryptreeContext context, final RepoFile repoFile, final CryptoRepoFile cryptoRepoFile) {
		this(null, null, context, repoFile, cryptoRepoFile);
	}

	public CryptreeNode(final CryptreeNode parent, final CryptreeNode child, final CryptreeContext context, final RepoFile repoFile, final CryptoRepoFile cryptoRepoFile) {
		if (parent == null && child == null && context == null)
			throw new IllegalArgumentException("parent == null && child == null && context == null");

		if (repoFile == null && cryptoRepoFile == null)
			throw new IllegalArgumentException("repoFile == null && cryptoRepoFile == null");

		this.parent = parent;
		this.context = context != null ? context
				: (parent != null ? parent.getContext() : child.getContext());

		this.repoFile = repoFile != null ? repoFile : cryptoRepoFile.getRepoFile();
		this.cryptoRepoFile = cryptoRepoFile;

		if (child != null)
			children.add(child);
	}

	public CryptreeContext getContext() {
		return context;
	}

	public RepoFile getRepoFile() {
		if (repoFile == null) {
			repoFile = getCryptoRepoFile().getRepoFile();
			if (repoFile == null) {
				getRepoFileDto();
				// We expect the line above to throw an AccessDeniedException, if the user is not allowed to access it.
				// But if we checked out a sub-directory, it's a valid state that the parent-RepoFile objects do *not*
				// exist (because we don't have local file representations) and still we can decrypt the RepoFileDto
				// of the parent directories.
				// This RepoFile is therefore clearly optional!
			}
			else
				context.registerCryptreeNode(repoFile, this);
		}
		return repoFile;
	}

	/**
	 * Gets the RepoFileDto containing meta-data only (the current version of it).
	 * @return the DTO or <code>null</code>, if there is no CryptoRepoFile (i.e. {@link #getCryptoRepoFile()} returns <code>null</code>.
	 * @throws AccessDeniedException
	 */
	public RepoFileDto getRepoFileDto() throws AccessDeniedException {
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFile();
		if (cryptoRepoFile == null)
			return null;

		final PlainCryptoKey plainCryptoKey = getPlainCryptoKeyForDecrypting(cryptoRepoFile.getCryptoKey());
		if (plainCryptoKey == null)
			throw new ReadAccessDeniedException(String.format("The CryptoRepoFile with cryptoRepoFileId=%s could not be decrypted! Access rights missing?!",
					cryptoRepoFile.getCryptoRepoFileId()));

		final byte[] plainRepoFileDtoData = assertNotNull("decrypt(...)", decrypt(cryptoRepoFile.getRepoFileDtoData(), plainCryptoKey));
		final RepoFileDto repoFileDto = context.repoFileDtoIo.deserializeWithGz(new ByteArrayInputStream(plainRepoFileDtoData));
		return repoFileDto;
	}

	public RepoFileDto getHistoCryptoRepoFileRepoFileDto(final HistoCryptoRepoFile histoCryptoRepoFile) throws AccessDeniedException {
		assertNotNull("histoCryptoRepoFile", histoCryptoRepoFile);

		final PlainCryptoKey plainCryptoKey = getPlainCryptoKeyForDecrypting(histoCryptoRepoFile.getCryptoKey());
		if (plainCryptoKey == null)
			throw new ReadAccessDeniedException(String.format("The HistoCryptoRepoFile with histoCryptoRepoFileId=%s could not be decrypted! Access rights missing?!",
					histoCryptoRepoFile.getHistoCryptoRepoFileId()));

		final byte[] plainRepoFileDtoData = assertNotNull("decrypt(...)", decrypt(histoCryptoRepoFile.getRepoFileDtoData(), plainCryptoKey));
		final RepoFileDto repoFileDto = context.repoFileDtoIo.deserializeWithGz(new ByteArrayInputStream(plainRepoFileDtoData));
		return repoFileDto;
	}

	public RepoFileDto getRepoFileDtoOnServer() throws AccessDeniedException {
		final CurrentHistoCryptoRepoFile currentHistoCryptoRepoFile = getCurrentHistoCryptoRepoFile();
		if (currentHistoCryptoRepoFile == null)
			return null;

		final HistoCryptoRepoFile histoCryptoRepoFile = currentHistoCryptoRepoFile.getHistoCryptoRepoFile();
		assertNotNull("currentHistoCryptoRepoFile.histoCryptoRepoFile", histoCryptoRepoFile);

		final PlainCryptoKey plainCryptoKey = getPlainCryptoKeyForDecrypting(histoCryptoRepoFile.getCryptoKey());
		if (plainCryptoKey == null)
			throw new ReadAccessDeniedException(String.format("The HistoCryptoRepoFile with cryptoRepoFileId=%s could not be decrypted! Access rights missing?!",
					histoCryptoRepoFile.getCryptoRepoFile().getCryptoRepoFileId()));

		final byte[] plainRepoFileDtoData = assertNotNull("decrypt(...)", decrypt(histoCryptoRepoFile.getRepoFileDtoData(), plainCryptoKey));
		final RepoFileDto repoFileDto = context.repoFileDtoIo.deserializeWithGz(new ByteArrayInputStream(plainRepoFileDtoData));
		return repoFileDto;
	}

	public CryptoRepoFile getCryptoRepoFile() {
		if (cryptoRepoFile == null) {
			if (repoFile == null) // at least one of them must be there!
				throw new IllegalStateException("repoFile == null && cryptoRepoFile == null");

			final CryptoRepoFileDao cryptoRepoFileDao = context.transaction.getDao(CryptoRepoFileDao.class);
			cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFile(repoFile); // may be null!

			if (cryptoRepoFile != null)
				context.registerCryptreeNode(cryptoRepoFile, this);
		}
		return cryptoRepoFile;
	}

	public CryptoRepoFile getCryptoRepoFileOrCreate(final boolean update) {
		CryptoRepoFile cryptoRepoFile = getCryptoRepoFile();
		if (cryptoRepoFile == null || update) {
			final CryptoRepoFileDao cryptoRepoFileDao = context.transaction.getDao(CryptoRepoFileDao.class);

			if (cryptoRepoFile == null) {
				cryptoRepoFile = new CryptoRepoFile();

				// We sign *after* we persist below in this *same* *method*, hence we store this
				// dummy value temporarily to avoid allowing NULL. It's set to the real value in
				// the same transaction, anyway. Hence we should never end up with this in the DB.
				cryptoRepoFile.setSignature(new SignatureDto(new Date(0), new Uid(0, 0), new byte[] { 7 }));
			}
			else {
				cryptoRepoFile.setDeleted(null);
				cryptoRepoFile.setDeletedByIgnoreRule(false);
				deletePreliminaryDeletions();
			}

			cryptoRepoFile.setRepoFile(repoFile); // repoFile is guaranteed to be *not* null, because of getCryptoRepoFile() above.
			cryptoRepoFile.setDirectory(repoFile instanceof Directory);

			// getPlainCryptoKeyOrCreate(...) causes this method to be called again. We thus must prevent
			// an eternal recursion by already assigning it to the field *now*.
			// Furthermore, CryptoKey has a reference to this object and will thus persist it, anyway.
			// Thus we explicitly persist it already here.
			this.cryptoRepoFile = cryptoRepoFile = cryptoRepoFileDao.makePersistent(cryptoRepoFile);

			final PlainCryptoKey plainCryptoKey = getActivePlainCryptoKeyOrCreate(CryptoKeyRole.dataKey, CipherOperationMode.ENCRYPT);
			final CryptoKey cryptoKey = assertNotNull("plainCryptoKey", plainCryptoKey).getCryptoKey();
			cryptoRepoFile.setCryptoKey(assertNotNull("plainCryptoKey.cryptoKey", cryptoKey));

			final CryptreeNode parent = getParent();
			cryptoRepoFile.setParent(parent == null ? null : parent.getCryptoRepoFile());

			final byte[] repoFileDtoData = createRepoFileDtoDataForCryptoRepoFile(false);
			cryptoRepoFile.setRepoFileDtoData(assertNotNull("encrypt(...)", encrypt(repoFileDtoData, plainCryptoKey)));
			cryptoRepoFile.setLastSyncFromRepositoryId(null);

			sign(cryptoRepoFile);
			context.transaction.flush(); // we want an early failure - not later during commit.
		}
		return cryptoRepoFile;
	}

	public CurrentHistoCryptoRepoFile getCurrentHistoCryptoRepoFile() {
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFile();
		if (cryptoRepoFile == null)
			return null;

		final CurrentHistoCryptoRepoFileDao chcrfDao = context.transaction.getDao(CurrentHistoCryptoRepoFileDao.class);
		final CurrentHistoCryptoRepoFile currentHistoCryptoRepoFile = chcrfDao.getCurrentHistoCryptoRepoFile(cryptoRepoFile);
		return currentHistoCryptoRepoFile;
	}

	/**
	 * Creates a new current {@link HistoCryptoRepoFile} and assigns it as the current one.
	 */
	public HistoCryptoRepoFile createHistoCryptoRepoFileIfNeeded() {
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFileOrCreate(false);
		assertNotNull("cryptoRepoFile", cryptoRepoFile);

		final CurrentHistoCryptoRepoFileDao chcrfDao = context.transaction.getDao(CurrentHistoCryptoRepoFileDao.class);
		final HistoCryptoRepoFileDao hcrfDao = context.transaction.getDao(HistoCryptoRepoFileDao.class);
		final HistoFrameDao hfDao = context.transaction.getDao(HistoFrameDao.class);
		final HistoFrame histoFrame = hfDao.getUnsealedHistoFrameOrFail(context.localRepositoryId);

		CurrentHistoCryptoRepoFile currentHistoCryptoRepoFile = chcrfDao.getCurrentHistoCryptoRepoFile(cryptoRepoFile);
		if (currentHistoCryptoRepoFile == null) {
			currentHistoCryptoRepoFile = new CurrentHistoCryptoRepoFile();
			currentHistoCryptoRepoFile.setCryptoRepoFile(cryptoRepoFile);
		}

		final HistoCryptoRepoFile previousHistoCryptoRepoFile = currentHistoCryptoRepoFile.getHistoCryptoRepoFile();

		Collection<HistoCryptoRepoFile> histoCryptoRepoFiles = hcrfDao.getHistoCryptoRepoFiles(cryptoRepoFile);
		for (HistoCryptoRepoFile histoCryptoRepoFile : histoCryptoRepoFiles) {
			if (histoFrame.equals(histoCryptoRepoFile.getHistoFrame())) {
//				createCollisionIfNeeded(histoCryptoRepoFile);
				return histoCryptoRepoFile; // TODO is this the right strategy? Or should we better delete and recreate? I encountered this situation when aborting an up-sync and resuming later.
//				throw new IllegalStateException("xxx");
			}
		}

		HistoCryptoRepoFile histoCryptoRepoFile = new HistoCryptoRepoFile();
		histoCryptoRepoFile.setCryptoRepoFile(cryptoRepoFile);
		histoCryptoRepoFile.setPreviousHistoCryptoRepoFile(previousHistoCryptoRepoFile);
		histoCryptoRepoFile.setHistoFrame(histoFrame);
		final Date deleted = cryptoRepoFile.getDeleted();
		histoCryptoRepoFile.setDeleted(deleted);
		histoCryptoRepoFile.setDeletedByIgnoreRule(cryptoRepoFile.isDeletedByIgnoreRule());

		final PlainCryptoKey plainCryptoKey = getActivePlainCryptoKeyOrCreate(CryptoKeyRole.dataKey, CipherOperationMode.ENCRYPT);
		final CryptoKey cryptoKey = assertNotNull("plainCryptoKey", plainCryptoKey).getCryptoKey();
		histoCryptoRepoFile.setCryptoKey(assertNotNull("plainCryptoKey.cryptoKey", cryptoKey));

		if (! cryptoKey.equals(cryptoRepoFile.getCryptoKey())) // sanity check: the key should not have changed inbetween! otherwise we might need a new CryptoChangeSet-upload to the server!!!
			throw new IllegalStateException(String.format("cryptoKey != cryptoRepoFile.cryptoKey :: %s != %s",
					cryptoKey, cryptoRepoFile.getCryptoKey()));

		final byte[] repoFileDtoData;
		if (deleted != null)
//			repoFileDtoData = getRepoFileDtoDataForDeletedCryptoRepoFile(previousHistoCryptoRepoFile);
			repoFileDtoData = serializeRepoFileDto(assertNotNull("getRepoFileDto()", getRepoFileDto()));
		else
			repoFileDtoData = createRepoFileDtoDataForCryptoRepoFile(true);

		histoCryptoRepoFile.setRepoFileDtoData(assertNotNull("encrypt(...)", encrypt(repoFileDtoData, plainCryptoKey)));
		histoCryptoRepoFile.setLastSyncFromRepositoryId(null);

		sign(histoCryptoRepoFile);

		histoCryptoRepoFile = hcrfDao.makePersistent(histoCryptoRepoFile);

		currentHistoCryptoRepoFile.setHistoCryptoRepoFile(histoCryptoRepoFile);
		currentHistoCryptoRepoFile.setLastSyncFromRepositoryId(null);

		sign(currentHistoCryptoRepoFile);

		chcrfDao.makePersistent(currentHistoCryptoRepoFile);

		updatePlainHistoCryptoRepoFile(histoCryptoRepoFile);

//		createCollisionIfNeeded(histoCryptoRepoFile);
		context.transaction.flush(); // for early detection of an error
		return histoCryptoRepoFile;
	}

	public Collision createCollisionIfNeeded(final CryptoRepoFile duplicateCryptoRepoFile, final String localPath, boolean expectedSealedStatus) {
		final LocalRepoTransaction tx = context.transaction;
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFile();

		final CollisionDao cDao = tx.getDao(CollisionDao.class);
		final HistoCryptoRepoFileDao hcrfDao = tx.getDao(HistoCryptoRepoFileDao.class);

		final Collection<HistoCryptoRepoFile> histoCryptoRepoFiles = hcrfDao.getHistoCryptoRepoFiles(cryptoRepoFile);

		final HistoCryptoRepoFile localHistoCryptoRepoFile = getLastHistoCryptoRepoFile(histoCryptoRepoFiles, false);
		final HistoCryptoRepoFile remoteHistoCryptoRepoFile = getLastHistoCryptoRepoFile(histoCryptoRepoFiles, true);

		if (localHistoCryptoRepoFile != null) {
			if (localHistoCryptoRepoFile.getHistoFrame().getSealed() != null && expectedSealedStatus == false)
				throw new IllegalStateException("Why is the local HistoFrame already sealed?!???!!!");

			if (localHistoCryptoRepoFile.getHistoFrame().getSealed() == null && expectedSealedStatus == true)
				throw new IllegalStateException("Why is the local HistoFrame not yet sealed?!???!!!");
		}

		HistoCryptoRepoFile histoCryptoRepoFile1 = localHistoCryptoRepoFile;
		HistoCryptoRepoFile histoCryptoRepoFile2 = remoteHistoCryptoRepoFile;

		if (histoCryptoRepoFile1 == null) {
			histoCryptoRepoFile1 = histoCryptoRepoFile2;
			histoCryptoRepoFile2 = null;
		}

		assertNotNull("histoCryptoRepoFile1", histoCryptoRepoFile1,
				"cryptoRepoFile=%s duplicateCryptoRepoFile=%s localPath=%s localHistoCryptoRepoFile=%s remoteHistoCryptoRepoFile=%s",
				cryptoRepoFile, duplicateCryptoRepoFile, localPath, localHistoCryptoRepoFile, remoteHistoCryptoRepoFile);

		if (duplicateCryptoRepoFile != null) {
			if (duplicateCryptoRepoFile.getCryptoRepoFileId().equals(getCryptoRepoFileId(histoCryptoRepoFile1))) {
				histoCryptoRepoFile1 = histoCryptoRepoFile2;
				histoCryptoRepoFile2 = null;
			}
			else if (histoCryptoRepoFile2 == null || duplicateCryptoRepoFile.getCryptoRepoFileId().equals(getCryptoRepoFileId(histoCryptoRepoFile2)))
				histoCryptoRepoFile2 = null;
			else
				throw new IllegalStateException("duplicateCryptoRepoFile neither matches histoCryptoRepoFile1 nor histoCryptoRepoFile2!\nduplicateCryptoRepoFile=" + duplicateCryptoRepoFile + "\nhistoCryptoRepoFile1=" + histoCryptoRepoFile1 + "\nhistoCryptoRepoFile2=" + histoCryptoRepoFile2);

			if (duplicateCryptoRepoFile.getCryptoRepoFileId().equals(getCryptoRepoFileId(histoCryptoRepoFile1)))
				throw new IllegalStateException("duplicateCryptoRepoFile matches histoCryptoRepoFile1!\nduplicateCryptoRepoFile=" + duplicateCryptoRepoFile + "\nhistoCryptoRepoFile1=" + histoCryptoRepoFile1 + "\nhistoCryptoRepoFile2=" + histoCryptoRepoFile2);
		}
		else
			assertNotNull("histoCryptoRepoFile2", histoCryptoRepoFile2);

		assertNotNull("histoCryptoRepoFile1", histoCryptoRepoFile1);

		final Uid duplicateCryptoRepoFileId = duplicateCryptoRepoFile == null ? null : duplicateCryptoRepoFile.getCryptoRepoFileId();
		Collision collision = cDao.getCollision(histoCryptoRepoFile1, histoCryptoRepoFile2, duplicateCryptoRepoFileId);
		if (collision == null) {
			collision = new Collision();
			collision.setHistoCryptoRepoFile1(histoCryptoRepoFile1);
			collision.setHistoCryptoRepoFile2(histoCryptoRepoFile2);
			collision.setDuplicateCryptoRepoFileId(duplicateCryptoRepoFileId);

			putCollisionPrivateDto(collision, new CollisionPrivateDto()); // signs the Collision
			collision = cDao.makePersistent(collision);
			logger.info("createCollisionIfNeeded: localPath='{}' histoCryptoRepoFile1={} histoCryptoRepoFile2={} duplicateCryptoRepoFileId={} localRevision={}",
					localPath, histoCryptoRepoFile1, histoCryptoRepoFile2, duplicateCryptoRepoFileId, collision.getLocalRevision());
		}

		updateCollisionPrivate(collision);
		context.transaction.flush(); // for early detection of an error
		return collision;
	}

	public void putCollisionPrivateDto(final Collision collision, final CollisionPrivateDto collisionPrivateDto) {
		assertNotNull("collision", collision);
		assertNotNull("collisionPrivateDto", collisionPrivateDto);

		if (collisionPrivateDto.getCollisionId() == null)
			collisionPrivateDto.setCollisionId(collision.getCollisionId());
		else if (! collisionPrivateDto.getCollisionId().equals(collision.getCollisionId()))
			throw new IllegalArgumentException("collisionPrivateDto.collisionId != collision.collisionId");

		final PlainCryptoKey plainCryptoKey = getActivePlainCryptoKeyOrCreate(CryptoKeyRole.dataKey, CipherOperationMode.ENCRYPT);
		final CryptoKey cryptoKey = assertNotNull("plainCryptoKey", plainCryptoKey).getCryptoKey();
		collision.setCryptoKey(assertNotNull("plainCryptoKey.cryptoKey", cryptoKey));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		context.collisionPrivateDtoIo.serializeWithGz(collisionPrivateDto, out);

		collision.setCollisionPrivateDtoData(assertNotNull("encrypt(...)", encrypt(out.toByteArray(), plainCryptoKey)));
		sign(collision);

		updateCollisionPrivate(collision);
	}

	public CollisionPrivateDto getCollisionPrivateDto(final Collision collision) {
		assertNotNull("collision", collision);

		final PlainCryptoKey plainCryptoKey = getPlainCryptoKeyForDecrypting(collision.getCryptoKey());
		if (plainCryptoKey == null)
			throw new ReadAccessDeniedException(String.format("The Collision with collisionId=%s could not be decrypted! Access rights missing?!",
					collision.getCollisionId()));

		final byte[] plainDtoData = assertNotNull("decrypt(...)", decrypt(collision.getCollisionPrivateDtoData(), plainCryptoKey));
		CollisionPrivateDto collisionPrivateDto = context.collisionPrivateDtoIo.deserializeWithGz(new ByteArrayInputStream(plainDtoData));
		return collisionPrivateDto;
	}

	private static Uid getCryptoRepoFileId(final HistoCryptoRepoFile histoCryptoRepoFile) {
		if (histoCryptoRepoFile == null)
			return null;

		return histoCryptoRepoFile.getCryptoRepoFile().getCryptoRepoFileId();
	}

	// TODO replace this method by a specific, optimized query!
	private HistoCryptoRepoFile getLastHistoCryptoRepoFile(final Collection<HistoCryptoRepoFile> histoCryptoRepoFiles, boolean remote) {
		assertNotNull("histoCryptoRepoFiles", histoCryptoRepoFiles);
		final UUID localRepositoryId = context.transaction.getLocalRepoManager().getRepositoryId();
		HistoCryptoRepoFile result = null;
		for (HistoCryptoRepoFile histoCryptoRepoFile : histoCryptoRepoFiles) {
			final HistoFrame histoFrame = histoCryptoRepoFile.getHistoFrame();
			if (remote) {
				if (localRepositoryId.equals(histoFrame.getFromRepositoryId()))
					continue;
			} else {
				if (! localRepositoryId.equals(histoFrame.getFromRepositoryId()))
					continue;
			}

			if (result == null || result.getSignature().getSignatureCreated().compareTo(histoCryptoRepoFile.getSignature().getSignatureCreated()) < 0)
				result = histoCryptoRepoFile;
		}

//		if (result == null)
//			throw new IllegalStateException("No matching HistoCryptoRepoFile found!");

		return result;
	}

	public CollisionPrivate updateCollisionPrivate(final Collision collision) {
		assertNotNull("collision", collision);

		if (! getCryptoRepoFile().equals(collision.getHistoCryptoRepoFile1().getCryptoRepoFile()))
			throw new IllegalArgumentException("this.cryptoRepoFile != collision.histoCryptoRepoFile1.cryptoRepoFile");

		final CollisionPrivateDao cpDao = getContext().transaction.getDao(CollisionPrivateDao.class);

		CollisionPrivate cp = cpDao.getCollisionPrivate(collision);

		final CollisionPrivateDto collisionPrivateDto = tryDecryptCollisionPrivateDto(collision);
		if (collisionPrivateDto == null) {
			if (cp != null)
				cpDao.deletePersistent(cp);
		}
		else
			cp = CollisionPrivateDtoConverter.create(getContext().transaction).putCollisionPrivateDto(collision, collisionPrivateDto);

		updatePlainHistoCryptoRepoFile(collision.getHistoCryptoRepoFile1());

		if (collision.getHistoCryptoRepoFile2() != null) {
			getContext().getCryptreeNodeOrCreate(collision.getHistoCryptoRepoFile2().getCryptoRepoFile().getCryptoRepoFileId())
			.updatePlainHistoCryptoRepoFile(collision.getHistoCryptoRepoFile2());
		}
		return cp;
	}

	public PlainHistoCryptoRepoFile updatePlainHistoCryptoRepoFile(final HistoCryptoRepoFile histoCryptoRepoFile) {
		assertNotNull("histoCryptoRepoFile", histoCryptoRepoFile);
		final PlainHistoCryptoRepoFileDao phcrfDao = getContext().transaction.getDao(PlainHistoCryptoRepoFileDao.class);
		PlainHistoCryptoRepoFile phcrf = phcrfDao.getPlainHistoCryptoRepoFile(histoCryptoRepoFile);
		if (phcrf == null) {
			phcrf = new PlainHistoCryptoRepoFile();
			phcrf.setHistoCryptoRepoFile(histoCryptoRepoFile);
		}
		phcrf.setPlainHistoCryptoRepoFileDto(createPlainHistoCryptoRepoFileDto(histoCryptoRepoFile));
		phcrf = phcrfDao.makePersistent(phcrf);
		return phcrf;
	}

	/**
	 * Creates a {@link PlainHistoCryptoRepoFileDto} which is then stored (as gzipped XML) in a {@link PlainHistoCryptoRepoFile}.
	 * This allows for very fast loading of the history (there might be *many* items especially in the first HistoFrame).
	 * @param histoCryptoRepoFile the {@link HistoCryptoRepoFile} to read and (try to) decrypt. Must not be <code>null</code>.
	 * @return the {@link PlainHistoCryptoRepoFileDto} - never <code>null</code>.
	 */
	private PlainHistoCryptoRepoFileDto createPlainHistoCryptoRepoFileDto(final HistoCryptoRepoFile histoCryptoRepoFile) {
		assertNotNull("histoCryptoRepoFile", histoCryptoRepoFile);

		final HistoCryptoRepoFileDtoConverter converter = HistoCryptoRepoFileDtoConverter.create(getContext().transaction);

		final PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto = new PlainHistoCryptoRepoFileDto();
		plainHistoCryptoRepoFileDto.setHistoCryptoRepoFileDto(converter.toHistoCryptoRepoFileDto(histoCryptoRepoFile));

		final Uid cryptoRepoFileId = histoCryptoRepoFile.getCryptoRepoFile().getCryptoRepoFileId();
		plainHistoCryptoRepoFileDto.setCryptoRepoFileId(cryptoRepoFileId);
		final CryptoRepoFile parentCryptoRepoFile = histoCryptoRepoFile.getCryptoRepoFile().getParent();
		plainHistoCryptoRepoFileDto.setParentCryptoRepoFileId(parentCryptoRepoFile == null ? null : parentCryptoRepoFile.getCryptoRepoFileId());

		final RepoFileDto repoFileDto = tryDecryptHistoCryptoRepoFile(histoCryptoRepoFile);

//		if (repoFileDto instanceof SsNormalFileDto) // no need to store these *LARGE* details in the PlainHistoCryptoRepoFile.
//			((SsNormalFileDto) repoFileDto).getFileChunkDtos().clear(); // YES, they ARE NEEDED! for the history export!

		plainHistoCryptoRepoFileDto.setRepoFileDto(repoFileDto);

		populateCollisionDtos(plainHistoCryptoRepoFileDto, histoCryptoRepoFile);

		return plainHistoCryptoRepoFileDto;
	}

	private void populateCollisionDtos(final PlainHistoCryptoRepoFileDto plainHistoCryptoRepoFileDto, final HistoCryptoRepoFile histoCryptoRepoFile) {
		assertNotNull("histoCryptoRepoFile", histoCryptoRepoFile);
		final CollisionDao cDao = getContext().transaction.getDao(CollisionDao.class);
		final CollisionPrivateDao cpDao = getContext().transaction.getDao(CollisionPrivateDao.class);
		final CollisionDtoConverter collisionDtoConverter = CollisionDtoConverter.create(getContext().transaction);
		final CollisionPrivateDtoConverter cpDtoConverter = CollisionPrivateDtoConverter.create(getContext().transaction);

		final CollisionFilter collisionFilter = new CollisionFilter();
		collisionFilter.setHistoCryptoRepoFileId(histoCryptoRepoFile.getHistoCryptoRepoFileId());
		final Collection<Collision> collisions = cDao.getCollisions(collisionFilter);

		for (final Collision collision : collisions) {
			plainHistoCryptoRepoFileDto.getCollisionDtos().add(collisionDtoConverter.toCollisionDto(collision));

			final CollisionPrivate collisionPrivate = cpDao.getCollisionPrivate(collision);
			if (collisionPrivate != null)
				plainHistoCryptoRepoFileDto.getCollisionPrivateDtos().add(cpDtoConverter.toCollisionPrivateDto(collisionPrivate));
		}
	}

	private RepoFileDto tryDecryptHistoCryptoRepoFile(final HistoCryptoRepoFile histoCryptoRepoFile) {
		assertNotNull("histoCryptoRepoFile", histoCryptoRepoFile);

		RepoFileDto repoFileDto = null;
		try {
			repoFileDto = getHistoCryptoRepoFileRepoFileDto(histoCryptoRepoFile);
		} catch (ReadAccessDeniedException x) {
			if (logger.isDebugEnabled())
				logger.info("tryDecryptHistoCryptoRepoFile: " + x, x);
			else
				logger.info("tryDecryptHistoCryptoRepoFile: " + x);
		}
		return repoFileDto;
	}

	private CollisionPrivateDto tryDecryptCollisionPrivateDto(final Collision collision) {
		assertNotNull("collision", collision);

		CollisionPrivateDto collisionPrivateDto = null;
		try {
			collisionPrivateDto = getCollisionPrivateDto(collision);
		} catch (ReadAccessDeniedException x) {
			if (logger.isDebugEnabled())
				logger.info("tryDecryptCollisionPrivateDto: " + x, x);
			else
				logger.info("tryDecryptCollisionPrivateDto: " + x);
		}
		return collisionPrivateDto;
	}

//	private byte[] getRepoFileDtoDataForDeletedCryptoRepoFile(final HistoCryptoRepoFile previousHistoCryptoRepoFile) {
//		// previousHistoCryptoRepoFile may be null, if it was never completely uploaded - hmmmm... why don't we *always* use the info from getRepoFileDto()?!
//
//		final RepoFileDto repoFileDto = previousHistoCryptoRepoFile == null ? getRepoFileDto() : getHistoCryptoRepoFileRepoFileDto(previousHistoCryptoRepoFile);
//		if (repoFileDto instanceof SsDirectoryDto)
//			; // nothing to do
//		else if (repoFileDto instanceof SsNormalFileDto) {
//			SsNormalFileDto normalFileDto = (SsNormalFileDto) repoFileDto;
//			normalFileDto.setFileChunkDtos(null);
//			normalFileDto.setTempFileChunkDtos(null); // they should always be null, anyway.
//		}
//		else if (repoFileDto instanceof SsSymlinkDto)
//			; // nothing to do
//		else
//			throw new IllegalStateException("Unexpected repoFileDto type: " + repoFileDto);
//
//		return serializeRepoFileDto(repoFileDto);
//	}

	private void grantReadPermission(final UserRepoKey.PublicKey publicKey) {
		assertNotNull("publicKey", publicKey);

		// TODO Make sure previously invisible sub-directories & files pop up in the client repositories!
		// If we grant access to a sub-directory later (either by (re)enabling the inheritance here or by
		// explicitly granting read access), the sub-directory (and all files) are not modified and therefore
		// are normally not synced (again). We somehow need to make sure that they indeed are re-synced.

		final CryptoLinkDao cryptoLinkDao = context.transaction.getDao(CryptoLinkDao.class);
		final Collection<CryptoLink> cryptoLinks = cryptoLinkDao.getActiveCryptoLinks(
				getCryptoRepoFileOrCreate(false), CryptoKeyRole.clearanceKey, CryptoKeyPart.privateKey);

		if (containsFromUserRepoKeyId(cryptoLinks, Collections.singleton(publicKey.getUserRepoKeyId())))
			return; // There is already an active key which is accessible to the given user. Thus no need to generate a new crypto-link.

		final PlainCryptoKey plainCryptoKey = getActivePlainCryptoKeyOrCreate(CryptoKeyRole.clearanceKey, CipherOperationMode.DECRYPT);
		createCryptoLink(this, getUserRepoKeyPublicKeyOrCreate(publicKey), plainCryptoKey);
	}

	private RepoFileDtoConverter getRepoFileDtoConverter() {
		if (repoFileDtoConverter == null)
			repoFileDtoConverter = RepoFileDtoConverter.create(context.transaction);

		repoFileDtoConverter.setExcludeLocalIds(false);
		repoFileDtoConverter.setExcludeMutableData(false);
		return repoFileDtoConverter;
	}

	private byte[] createRepoFileDtoDataForCryptoRepoFile(final boolean forHisto) {
		// TODO can we assert here, that this code is invoked on the client-side with the plain-text RepoFile?!

		final RepoFileDtoConverter repoFileDtoConverter = getRepoFileDtoConverter();
		// No need for local IDs. Because this DTO is shared across all repositories, local IDs make no sense.
		repoFileDtoConverter.setExcludeLocalIds(true);

		// Erase information like last-modified, hash and length, if not used in HistoCryptoRepoFile!
		repoFileDtoConverter.setExcludeMutableData(! forHisto);
		final RepoFileDto repoFileDto = repoFileDtoConverter.toRepoFileDto(repoFile, forHisto ? Integer.MAX_VALUE : 0);

		((SsRepoFileDto) repoFileDto).setParentName(null); // only needed for uploading to the server.
		if (((SsRepoFileDto) repoFileDto).getSignature() != null) // must be null on the client - and this method is never called on the server.
			throw new IllegalStateException("repoFileDto.signature != null");

		// Prevent overriding the real name with "", if we checked out a sub-directory. In this case, we cannot
		// change the localName locally and must make sure, it is preserved.
		if (cryptoRepoFile.getLocalName() == null || repoFile.getParent() != null)
			cryptoRepoFile.setLocalName(repoFileDto.getName());
		else
			repoFileDto.setName(cryptoRepoFile.getLocalName());

		return serializeRepoFileDto(repoFileDto);
	}

	private byte[] serializeRepoFileDto(final RepoFileDto repoFileDto) {
		assertNotNull("repoFileDto", repoFileDto);

		// Serialise to XML and compress.
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			try (final GZIPOutputStream gzOut = new GZIPOutputStream(out);) {
				context.repoFileDtoIo.serialize(repoFileDto, gzOut);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		return out.toByteArray();
	}

	private void revokeReadPermission(final Set<Uid> userRepoKeyIds) {
		assertNotNull("userRepoKeyIds", userRepoKeyIds);
		if (userRepoKeyIds.isEmpty())
			return;

		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFile();
		if (cryptoRepoFile == null)
			return; // There is no CryptoRepoFile, thus there can be no read-access which could be revoked.

		final CryptoLinkDao cryptoLinkDao = context.transaction.getDao(CryptoLinkDao.class);
		final Collection<CryptoLink> cryptoLinks = cryptoLinkDao.getActiveCryptoLinks(
				cryptoRepoFile, CryptoKeyRole.clearanceKey, CryptoKeyPart.privateKey);

		if (! containsFromUserRepoKeyId(cryptoLinks, userRepoKeyIds))
			return; // There is no active key which is accessible to any of the given users. Thus no need to generate a new key.

		// There should be only one single *active* key, but due to collisions (multiple repos grant the same user
		// access), it might happen, that there are multiple. We therefore de-activate all we find.
		final Set<CryptoKey> processedCryptoKeys = new HashSet<CryptoKey>();
		for (final CryptoLink cryptoLink : cryptoLinks)
			deactivateCryptoKeyAndDescendants(cryptoLink.getToCryptoKey(), processedCryptoKeys); // likely the same CryptoKey for all cryptoLinks => deduplicate via Set

		// Make sure the changes are written to the DB, so that a new active clearance key is generated in the following.
		context.transaction.flush();

		// Create a *new* clearance key *immediately*, but only if needed.
		PlainCryptoKey clearanceKeyPlainCryptoKey = null;

		// And grant read access to everyone except for the users that should be removed.
		for (final CryptoLink cryptoLink : cryptoLinks) {
			final UserRepoKeyPublicKey fromUserRepoKeyPublicKey = cryptoLink.getFromUserRepoKeyPublicKey();
			final Uid fromUserRepoKeyId = fromUserRepoKeyPublicKey == null ? null : fromUserRepoKeyPublicKey.getUserRepoKeyId();
			if (fromUserRepoKeyId == null || userRepoKeyIds.contains(fromUserRepoKeyId))
				continue;

			if (clearanceKeyPlainCryptoKey == null)
				clearanceKeyPlainCryptoKey = getActivePlainCryptoKeyOrCreate(CryptoKeyRole.clearanceKey, CipherOperationMode.DECRYPT);

			// The current user is already granted access when the clearing key was created above.
			// We thus need to check, if the current fromUserRepoKeyPublicKey still needs a CryptoLink.
			if (fromUserRepoKeyPublicKey != null
					&& ! cryptoLinkDao.getActiveCryptoLinks(
							cryptoRepoFile, CryptoKeyRole.clearanceKey, CryptoKeyPart.privateKey, fromUserRepoKeyPublicKey).isEmpty())
				continue;

			createCryptoLink(this, fromUserRepoKeyPublicKey, clearanceKeyPlainCryptoKey);
		}

		if (clearanceKeyPlainCryptoKey != null)
			createBacklinkKeyForFile();

		createSubdirKeyAndBacklinkKeyIfNeededChildrenRecursively();
//		createBacklinkKeyIfNeededParentsRecursively();
	}

	private void createBacklinkKeyForFile() {
		if (! isDirectory())
			getActivePlainCryptoKeyOrCreate(CryptoKeyRole.backlinkKey, CipherOperationMode.DECRYPT);
	}

	private void deactivateCryptoKeyAndDescendants(final CryptoKey cryptoKey, final Set<CryptoKey> processedCryptoKeys) {
		if (! processedCryptoKeys.add(cryptoKey))
			return;

		if (cryptoKey.getCryptoKeyDeactivation() == null)
			deactivateCryptoKey(cryptoKey);

		for (final CryptoLink cryptoLink : cryptoKey.getOutCryptoLinks())
			deactivateCryptoKeyAndDescendants(cryptoLink.getToCryptoKey(), processedCryptoKeys);
	}

	private void deactivateCryptoKey(final CryptoKey cryptoKey) {
		final CryptoKeyDeactivation cryptoKeyDeactivation = new CryptoKeyDeactivation();
		cryptoKeyDeactivation.setCryptoKey(cryptoKey);
		sign(cryptoKeyDeactivation);
		cryptoKey.setCryptoKeyDeactivation(cryptoKeyDeactivation);
		cryptoKey.setLastSyncFromRepositoryId(null);
	}

	private void createSubdirKeyAndBacklinkKeyIfNeededChildrenRecursively() {
		// Only directories have a subdirKey (and further children). The backlinkKeys of the files
		// are optional and didn't get dirty, because they are not in the CryptoLink-chain of the parent.
		// The backlinkKey only got dirty, if this is a file. We handle this separately in
		// createBacklinkKeyIfNeededFile().
		if (! isDirectory())
			return;

		getActivePlainCryptoKeyOrCreate(CryptoKeyRole.subdirKey, CipherOperationMode.DECRYPT);
		getActivePlainCryptoKeyOrCreate(CryptoKeyRole.backlinkKey, CipherOperationMode.DECRYPT);

		for (final CryptreeNode child : getChildren())
			child.createSubdirKeyAndBacklinkKeyIfNeededChildrenRecursively();
	}

//	private void createBacklinkKeyIfNeededParentsRecursively() {
//		// If this is a file, the backlinkKey is optional - and created in createBacklinkKeyIfNeededFile().
//		if (isDirectory())
//			getActivePlainCryptoKeyOrCreate(CryptoKeyRole.backlinkKey, CryptoKeyPart.sharedSecret);
//
//		final CryptreeNode parent = getParent();
//		if (parent != null)
//			parent.createBacklinkKeyIfNeededParentsRecursively();
//	}

	private boolean containsFromUserRepoKeyId(final Collection<CryptoLink> cryptoLinks, final Set<Uid> fromUserRepoKeyIds) {
		assertNotNull("cryptoLinks", cryptoLinks);
		assertNotNull("fromUserRepoKeyIds", fromUserRepoKeyIds);
		for (final CryptoLink cryptoLink : cryptoLinks) {
			final UserRepoKeyPublicKey fromUserRepoKeyPublicKey = cryptoLink.getFromUserRepoKeyPublicKey();
			final Uid fromUserRepoKeyId = fromUserRepoKeyPublicKey == null ? null : fromUserRepoKeyPublicKey.getUserRepoKeyId();
			if (fromUserRepoKeyId != null && fromUserRepoKeyIds.contains(fromUserRepoKeyId))
				return true;
		}
		return false;
	}

	public Collection<CryptreeNode> getChildren() {
		if (! childrenLoaded) {
			if (cryptoRepoFile != null) {
				final CryptoRepoFileDao dao = context.transaction.getDao(CryptoRepoFileDao.class);
				final Collection<CryptoRepoFile> childCryptoRepoFiles = dao.getChildCryptoRepoFiles(cryptoRepoFile);
				for (final CryptoRepoFile childCryptoRepoFile : childCryptoRepoFiles)
					children.add(context.getCryptreeNodeOrCreate(this, null, null, childCryptoRepoFile));
			}
			else if (repoFile != null) {
				final RepoFileDao dao = context.transaction.getDao(RepoFileDao.class);
				final Collection<RepoFile> childRepoFiles = dao.getChildRepoFiles(repoFile);
				for (final RepoFile childRepoFile : childRepoFiles)
					children.add(context.getCryptreeNodeOrCreate(this, null, childRepoFile, null));
			}
			else
				throw new IllegalStateException("repoFile == null && cryptoRepoFile == null");

			childrenLoaded = true;
		}
		return Collections.unmodifiableList(children);
	}

	protected boolean isDirectory() {
		if (repoFile != null)
			return repoFile instanceof Directory;

		return cryptoRepoFile.isDirectory();
	}

	protected PlainCryptoKey getActivePlainCryptoKey(final CryptoKeyRole toCryptoKeyRole, final CipherOperationMode cipherOperationMode) {
		assertNotNull("toCryptoKeyRole", toCryptoKeyRole);
		assertNotNull("cipherOperationMode", cipherOperationMode);
		logger.debug("getActivePlainCryptoKey: cryptoRepoFile={} repoFile={} toCryptoKeyRole={} cipherOperationMode={}",
				cryptoRepoFile, repoFile, toCryptoKeyRole, cipherOperationMode);
		final CryptoLinkDao cryptoLinkDao = context.transaction.getDao(CryptoLinkDao.class);

		final CryptoKeyPart[] toCryptoKeyParts = toCryptoKeyRole.getCryptoKeyParts(cipherOperationMode);

		for (final CryptoKeyPart toCryptoKeyPart : toCryptoKeyParts) {
			final Collection<CryptoLink> cryptoLinks = cryptoLinkDao.getActiveCryptoLinks(getCryptoRepoFile(), toCryptoKeyRole, toCryptoKeyPart);
			final PlainCryptoKey plainCryptoKey = getPlainCryptoKey(cryptoLinks, toCryptoKeyPart);
			if (plainCryptoKey != null)
				return plainCryptoKey;
		}
		return null;
	}

	public PlainCryptoKey getPlainCryptoKeyForDecrypting(final CryptoKey cryptoKey) {
		assertNotNull("cryptoKey", cryptoKey);
		logger.debug("getPlainCryptoKeyForDecrypting: cryptoRepoFile={} repoFile={} cryptoKey={}",
				cryptoRepoFile, repoFile, cryptoKey);
		final PlainCryptoKey plainCryptoKey = getPlainCryptoKey(cryptoKey.getInCryptoLinks(), getCryptoKeyPartForDecrypting(cryptoKey));
		return plainCryptoKey; // may be null!
	}

	private PlainCryptoKey getPlainCryptoKey(final Collection<CryptoLink> cryptoLinks, final CryptoKeyPart toCryptoKeyPart) {
		assertNotNull("cryptoLinks", cryptoLinks);
		assertNotNull("toCryptoKeyPart", toCryptoKeyPart);
		for (final CryptoLink cryptoLink : cryptoLinks) {
			if (toCryptoKeyPart != cryptoLink.getToCryptoKeyPart())
				continue;

			final UserRepoKeyPublicKey fromUserRepoKeyPublicKey = cryptoLink.getFromUserRepoKeyPublicKey();
			if (fromUserRepoKeyPublicKey != null) {
				logger.debug("getPlainCryptoKey: >>> cryptoRepoFile={} repoFile={} cryptoLink={} fromUserRepoKeyPublicKey={}",
						cryptoRepoFile, repoFile, cryptoLink, fromUserRepoKeyPublicKey);
				final Uid userRepoKeyId = fromUserRepoKeyPublicKey.getUserRepoKeyId();
				final UserRepoKey userRepoKey = context.userRepoKeyRing.getUserRepoKey(userRepoKeyId);
				if (userRepoKey != null) {
					logger.debug("getPlainCryptoKey: <<< cryptoRepoFile={} repoFile={} cryptoLink={} fromUserRepoKeyPublicKey={}: DECRYPTED!",
							cryptoRepoFile, repoFile, cryptoLink, fromUserRepoKeyPublicKey);
					final byte[] plain = decryptLarge(cryptoLink.getToCryptoKeyData(), userRepoKey);
					return new PlainCryptoKey(cryptoLink.getToCryptoKey(), cryptoLink.getToCryptoKeyPart(), plain);
				}
				else
					logger.debug("getPlainCryptoKey: <<< cryptoRepoFile={} repoFile={} cryptoLink={} fromUserRepoKeyPublicKey={}: FAILED TO DECRYPT!",
							cryptoRepoFile, repoFile, cryptoLink, fromUserRepoKeyPublicKey);
			}
			else if (cryptoLink.getFromCryptoKey() == null) {
				// *not* encrypted
				logger.debug("getPlainCryptoKey: *** cryptoRepoFile={} repoFile={} cryptoLink={}: PLAIN!",
						cryptoRepoFile, repoFile, cryptoLink);
				return new PlainCryptoKey(cryptoLink.getToCryptoKey(), cryptoLink.getToCryptoKeyPart(), cryptoLink.getToCryptoKeyData());
			}
		}

		for (final CryptoLink cryptoLink : cryptoLinks) {
			if (toCryptoKeyPart != cryptoLink.getToCryptoKeyPart())
				continue;

			final CryptoKey fromCryptoKey = cryptoLink.getFromCryptoKey();
			if (fromCryptoKey != null) {
				logger.debug("getPlainCryptoKey: >>> cryptoRepoFile={} repoFile={} cryptoLink={} fromCryptoKey={}",
						cryptoRepoFile, repoFile, cryptoLink, fromCryptoKey);
				final PlainCryptoKey plainFromCryptoKey = getPlainCryptoKey(
						fromCryptoKey.getInCryptoLinks(), getCryptoKeyPartForDecrypting(fromCryptoKey));
				if (plainFromCryptoKey != null) {
					logger.debug("getPlainCryptoKey: <<< cryptoRepoFile={} repoFile={} cryptoLink={} fromCryptoKey={}: DECRYPTED!",
							cryptoRepoFile, repoFile, cryptoLink, fromCryptoKey);
					final byte[] plain = decrypt(
							cryptoLink.getToCryptoKeyData(),
							plainFromCryptoKey);
					return new PlainCryptoKey(cryptoLink.getToCryptoKey(), cryptoLink.getToCryptoKeyPart(), plain);
				}
				else
					logger.debug("getPlainCryptoKey: <<< cryptoRepoFile={} repoFile={} cryptoLink={} fromCryptoKey={}: FAILED TO DECRYPT!",
							cryptoRepoFile, repoFile, cryptoLink, fromCryptoKey);
			}
		}
		return null;
	}

	private CryptoKeyPart getCryptoKeyPartForDecrypting(final CryptoKey cryptoKey) {
		switch (cryptoKey.getCryptoKeyType()) {
			case asymmetric:
				return CryptoKeyPart.privateKey;
			case symmetric:
				return CryptoKeyPart.sharedSecret;
			default:
				throw new IllegalStateException("Unknown cryptoKey.cryptoKeyType: " + cryptoKey.getCryptoKeyType());
		}
	}

	protected PlainCryptoKey getActivePlainCryptoKeyOrCreate(final CryptoKeyRole toCryptoKeyRole, final CipherOperationMode cipherOperationMode) {
		assertNotNull("toCryptoKeyRole", toCryptoKeyRole);
		assertNotNull("cipherOperationMode", cipherOperationMode);
		PlainCryptoKey plainCryptoKey = getActivePlainCryptoKey(toCryptoKeyRole, cipherOperationMode);
		if (plainCryptoKey == null) {
			final Class<? extends PlainCryptoKeyFactory> clazz = cryptoKeyRole2PlainCryptoKeyFactory.get(toCryptoKeyRole);
			assertNotNull(String.format("cryptoKeyRole2PlainCryptoKeyFactory[%s]", toCryptoKeyRole), clazz);

			final PlainCryptoKeyFactory factory;
			try {
				factory = clazz.newInstance();
			} catch (final Exception e) {
				throw new RuntimeException(String.format("Creating new instance of class %s failed: %s", clazz.getName(), e), e);
			}

			factory.setCryptreeNode(this);
			factory.setCipherOperationMode(cipherOperationMode);
			plainCryptoKey = factory.createPlainCryptoKey();
			assertNotNull(clazz.getName() + ".createPlainCryptoKey()", plainCryptoKey);

			if (plainCryptoKey.getCryptoKey().getCryptoKeyRole() != toCryptoKeyRole)
				throw new IllegalStateException(String.format("plainCryptoKey.cryptoKey.cryptoKeyRole != toCryptoKeyRole :: %s != %s", plainCryptoKey.getCryptoKey().getCryptoKeyRole(), toCryptoKeyRole));

			final CryptoKeyDao cryptoKeyDao = context.transaction.getDao(CryptoKeyDao.class);
			final CryptoKey cryptoKey = cryptoKeyDao.makePersistent(plainCryptoKey.getCryptoKey());
			plainCryptoKey = new PlainCryptoKey(cryptoKey, plainCryptoKey.getCryptoKeyPart(), plainCryptoKey.getCipherParameters());
		}
		return plainCryptoKey;
	}

	/**
	 * Gets the current data key as indicated by {@link CryptoRepoFile#getCryptoKey()}.
	 * @return the current data key. Never <code>null</code>.
	 */
	public DataKey getDataKeyOrFail() {
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFile();
		assertNotNull("cryptoRepoFile", cryptoRepoFile);
		return getDataKeyOrFail(cryptoRepoFile.getCryptoKey());
	}

	/**
	 * Gets the data key identified by the given {@ code cryptoKeyId}.
	 * @param cryptoKeyId the unique ID of the {@link CryptoKey} from which to extract the plain key material.
	 * @return the data key. Never <code>null</code>.
	 */
	public DataKey getDataKeyOrFail(final Uid cryptoKeyId) {
		assertNotNull("cryptoKeyId", cryptoKeyId);
		final CryptoKeyDao cryptoKeyDao = context.transaction.getDao(CryptoKeyDao.class);
		final CryptoKey cryptoKey = cryptoKeyDao.getCryptoKeyOrFail(cryptoKeyId);
		return getDataKeyOrFail(cryptoKey);
	}

	protected DataKey getDataKeyOrFail(final CryptoKey cryptoKey) {
		assertNotNull("cryptoKey", cryptoKey);

		// We can use the following method, because it's *symmetric* - thus it works for both decrypting and encrypting!
		final PlainCryptoKey plainCryptoKey = getPlainCryptoKeyForDecrypting(cryptoKey);
		if (plainCryptoKey == null)
			throw new ReadAccessDeniedException(String.format("Cannot decrypt dataKey for cryptoKeyID=%s (cryptoRepoFileId=%s)!",
					cryptoKey.getCryptoKeyId(), cryptoKey.getCryptoRepoFile().getCryptoRepoFileId()));

		assertNotNull("plainCryptoKey.cryptoKey", plainCryptoKey.getCryptoKey());

		if (CryptoKeyRole.dataKey != plainCryptoKey.getCryptoKey().getCryptoKeyRole())
			throw new IllegalStateException("CryptoKeyRole.dataKey != plainCryptoKey.getCryptoKey().getCryptoKeyRole()");

		if (CryptoKeyPart.sharedSecret != plainCryptoKey.getCryptoKeyPart())
			throw new IllegalStateException("CryptoKeyPart.sharedSecret != plainCryptoKey.getCryptoKeyPart()");

		return new DataKey(cryptoKey.getCryptoKeyId(), plainCryptoKey.getKeyParameterOrFail());
	}

	public CryptreeNode getParent() {
		if (parent == null) {
			if (repoFile != null && JDOHelper.isDeleted(repoFile)) {
				getCryptoRepoFile();
				assertNotNull("cryptoRepoFile", cryptoRepoFile);
			}

			final RepoFile parentRepoFile = repoFile == null || JDOHelper.isDeleted(repoFile) ? null : repoFile.getParent();
			final CryptoRepoFile parentCryptoRepoFile = cryptoRepoFile == null ? null : cryptoRepoFile.getParent();

			if (parentRepoFile != null || parentCryptoRepoFile != null)
				parent = context.getCryptreeNodeOrCreate(null, this, parentRepoFile, parentCryptoRepoFile);
		}
		return parent;
	}

	public UserRepoKeyPublicKey getUserRepoKeyPublicKeyOrCreate(final UserRepoKey userRepoKey) {
		assertNotNull("userRepoKey", userRepoKey);
		return getUserRepoKeyPublicKeyOrCreate(userRepoKey.getPublicKey());
	}

	public UserRepoKeyPublicKey getUserRepoKeyPublicKeyOrCreate(final UserRepoKey.PublicKey publicKey) {
		assertNotNull("publicKey", publicKey);
		return new UserRepoKeyPublicKeyHelper(getContext()).getUserRepoKeyPublicKeyOrCreate(publicKey);
	}

	/**
	 * Grant a permission on the directory/file represented by this node.
	 * <p>
	 * <p>
	 * If there is nothing to be granted, this method is a noop.
	 * <p>
	 * Or in other words:
	 * If the user (more specifically, his repository-dependent {@code publicKey}) already has the permission
	 * which is to be granted, this method returns silently without any effect.
	 * @param permissionType the type of the permission to be granted. Must not be <code>null</code>.
	 * @param publicKey the user's repository-dependent public-key. Must not be <code>null</code>.
	 * @see #getGrantedPermissionTypes(Uid)
	 * @see #revokePermission(PermissionType, Set)
	 */
	public void grantPermission(final PermissionType permissionType, final UserRepoKey.PublicKey publicKey) {
		assertNotNull("permissionType", permissionType);
		assertNotNull("publicKey", publicKey);

		if (isOwner(publicKey.getUserRepoKeyId()))
			return; // the owner always has all permissions - no need to grant anything!

		if (PermissionType.readUserIdentity == permissionType) {
			final CryptreeNode parent = getParent();
			if (parent != null) {
				parent.grantPermission(permissionType, publicKey);
				return;
			}
		}

		// It is technically required to have read permission, when having write or grant permission. Therefore,
		// we simply grant it here.
		if (permissionType == PermissionType.read || permissionType == PermissionType.write || permissionType == PermissionType.grant)
			grantReadPermission(publicKey);

		if (PermissionType.read == permissionType)
			return;

		final Uid ownerUserRepoKeyId = context.getRepositoryOwnerOrFail().getUserRepoKeyPublicKey().getUserRepoKeyId();
		if (ownerUserRepoKeyId.equals(publicKey.getUserRepoKeyId()))
			return;

		// It is technically required to have write permission, when having grant permission. Therefore, we
		// grant it here, too. Additionally, we grant readUserIdentity permission, as it makes no sense to manage
		// users, i.e. grant access to them, without seeing them ;-)
		if (PermissionType.grant == permissionType) {
			grantPermission(PermissionType.write, publicKey);
			grantPermission(PermissionType.readUserIdentity, publicKey);
		}

		final PermissionSet permissionSet = getPermissionSetOrCreate();
		final PermissionDao dao = context.transaction.getDao(PermissionDao.class);
		final UserRepoKeyPublicKey userRepoKeyPublicKey = getUserRepoKeyPublicKeyOrCreate(publicKey);
		final Collection<Permission> permissions = dao.getNonRevokedPermissions(permissionSet, permissionType, userRepoKeyPublicKey);
		if (permissions.isEmpty()) {
			Permission permission = new Permission();
			permission.setPermissionSet(permissionSet);
			permission.setPermissionType(permissionType);
			permission.setUserRepoKeyPublicKey(userRepoKeyPublicKey);
			sign(permission);
			permission = dao.makePersistent(permission);
			assertPermissionOk(permission);
		}

		if (PermissionType.grant == permissionType)
			ensureParentHasAsymmetricActiveSubdirKey();

		if (PermissionType.readUserIdentity == permissionType)
			createUserIdentityLinksVisibleFor(userRepoKeyPublicKey);
	}

	public void setPermissionsInherited(final boolean inherited) {
		if (inherited == isPermissionsInherited())
			return;

		final PermissionSet permissionSet = getPermissionSetOrCreate();
		if (inherited) {
			// TODO Make sure previously invisible sub-directories & files pop up in the client repositories!
			// If we grant access to a sub-directory later (either by (re)enabling the inheritance here or by
			// explicitly granting read access), the sub-directory (and all files) are not modified and therefore
			// are normally not synced (again). We somehow need to make sure that they indeed are re-synced.
			final PermissionSetInheritance permissionSetInheritance = new PermissionSetInheritance();
			permissionSetInheritance.setPermissionSet(permissionSet);
			sign(permissionSetInheritance);

			permissionSet.getPermissionSetInheritances().add(permissionSetInheritance);
		}
		else {
			final RepositoryOwner repositoryOwner = context.getRepositoryOwnerOrFail();
			final PublicKey ownerPublicKey = repositoryOwner.getUserRepoKeyPublicKey().getPublicKey();
			grantReadPermission(ownerPublicKey);

			final boolean currentUserIsOwner = context.userRepoKeyRing.getUserRepoKey(ownerPublicKey.getUserRepoKeyId()) != null;
			if (! currentUserIsOwner) {
				// TODO since the inheritance is interrupted, the current user does not have grant access anymore.
				// We need to either extend the grant access chain (better) or make this operation only available to the owner.
				logger.warn("This is not yet cleanly implemented and likely causes an error.", new UnsupportedOperationException("NYI"));
			}

			for (final PermissionSetInheritance permissionSetInheritance : permissionSet.getPermissionSetInheritances()) {
				if (permissionSetInheritance.getRevoked() == null) {
					permissionSetInheritance.setRevoked(new Date());
					sign(permissionSetInheritance);
				}
			}

			// There should be only one single *active* key, but due to collisions (multiple repos grant the same user
			// access), it might happen, that there are multiple. We therefore de-activate all we find.
			final Set<CryptoKey> processedCryptoKeys = new HashSet<CryptoKey>();
			final Collection<CryptoKey> subdirKeys = context.transaction.getDao(CryptoKeyDao.class).getActiveCryptoKeys(getCryptoRepoFileOrCreate(false), CryptoKeyRole.subdirKey);
			for (final CryptoKey subdirKey : subdirKeys)
				deactivateCryptoKeyAndDescendants(subdirKey, processedCryptoKeys);

//			createBacklinkKeyForFile(); // TODO do we need this - especially for the children?

			createSubdirKeyAndBacklinkKeyIfNeededChildrenRecursively();
		}
	}

	public boolean isPermissionsInherited() {
		final PermissionSet permissionSet = getPermissionSet();
		if (permissionSet == null)
			return true;

		// This method is for the UI, hence we use the revoked state - not the validTo.
		for (final PermissionSetInheritance permissionSetInheritance : permissionSet.getPermissionSetInheritances()) {
			if (permissionSetInheritance.getRevoked() == null)
				return true;
		}
		return false;
	}

	private void ensureParentHasAsymmetricActiveSubdirKey() {
		final CryptreeNode parent = getParent();
		final CryptoRepoFile parentCryptoRepoFile = parent == null ? null : parent.getCryptoRepoFileOrCreate(false);
		if (parentCryptoRepoFile == null)
			return;

		final CryptoKeyDao cryptoKeyDao = context.transaction.getDao(CryptoKeyDao.class);
		final Collection<CryptoKey> activeSubdirKeys = cryptoKeyDao.getActiveCryptoKeys(parentCryptoRepoFile, CryptoKeyRole.subdirKey);
		boolean hasAsymmetryActiveSubdirKey = false;
		for (final CryptoKey activeSubdirKey : activeSubdirKeys) {
			switch (activeSubdirKey.getCryptoKeyType()) {
				case asymmetric:
					hasAsymmetryActiveSubdirKey = true;
					break;
				case symmetric:
					deactivateCryptoKey(activeSubdirKey);
					break;
				default:
					throw new IllegalStateException("Unknown CryptoKeyType: " + activeSubdirKey.getCryptoKeyType());
			}
		}

		if (! hasAsymmetryActiveSubdirKey)
			parent.getActivePlainCryptoKeyOrCreate(CryptoKeyRole.subdirKey, CipherOperationMode.DECRYPT);
	}

	/**
	 * Revoke a permission on the directory/file represented by this node.
	 * <p>
	 * If there is nothing to be revoked, this method is a noop.
	 * <p>
	 * Or in other words:
	 * If none of the users (more specifically, their repository-dependent keys) have the permission which is
	 * to be revoked, this method returns silently without any effect. The same happens when the {@code userRepoKeyIds}
	 * is empty.
	 * @param permissionType the type of the permission to be revoked. Must not be <code>null</code>.
	 * @param userRepoKeyIds identifiers of all those keys from which to revoke the permission. Must not be <code>null</code>.
	 * May be empty, which causes this method to return without any effect.
	 * @see #getGrantedPermissionTypes(Uid)
	 * @see #grantPermission(PermissionType, PublicKey)
	 */
	public void revokePermission(final PermissionType permissionType, final Set<Uid> userRepoKeyIds) {
		assertNotNull("permissionType", permissionType);
		assertNotNull("userRepoKeyIds", userRepoKeyIds);

		if (PermissionType.readUserIdentity == permissionType) {
			final CryptreeNode parent = getParent();
			if (parent != null) {
				parent.revokePermission(permissionType, userRepoKeyIds);
				return;
			}

			// Revoke grant permission - technically not really needed but it makes no sense to manage invisible/unknown users.
			revokeGrantPermissionOfAllCryptoRepoFiles(userRepoKeyIds);
		}

		if (PermissionType.read == permissionType) {
			// Since it is technically required to have read permission, when having write or grant permission, we
			// revoke grant and write permission, too.
			revokePermission(PermissionType.write, userRepoKeyIds);
//			revokePermission(PermissionType.grant, userRepoKeyIds); // is implicitly done by revoking 'write' permission.

			revokeReadPermission(userRepoKeyIds);
			return;
		}

		if (PermissionType.write == permissionType) {
			// grant requires write, hence we must revoke grant, too, if we want to revoke write.
			revokePermission(PermissionType.grant, userRepoKeyIds);
		}

		final PermissionSet permissionSet = getPermissionSet();
		if (permissionSet == null)
			return;

		final PermissionDao dao = context.transaction.getDao(PermissionDao.class);
		final Collection<Permission> permissions = dao.getNonRevokedPermissions(permissionSet, permissionType, userRepoKeyIds);

		for (final Permission permission : permissions) {
			permissionsAlreadyCheckedOk.remove(permission);
			permission.setRevoked(new Date());
			sign(permission);
			assertPermissionOk(permission);
		}

		if (PermissionType.readUserIdentity == permissionType) {
			if (existsAtLeastOneUserIdentityLinkFor(userRepoKeyIds))
				new UserRepoKeyPublicKeyHelper(getContext()).removeUserIdentityLinksAfterRevokingReadUserIdentityPermission();
		}
	}

	/**
	 * Gets the {@link PermissionType}s granted to the directory/file represented by this node.
	 * <p>
	 * <b>Important:</b> In contrast to {@link #assertHasPermission(boolean, Uid, PermissionType, Date) assertHasPermission(...)} this
	 * method operates on the current node, only! It does not take parents / inheritance into account.
	 * <p>
	 * <b>Important:</b> If the specified user has {@link PermissionType#readUserIdentity readUserIdentity}, this
	 * {@code PermissionType} is always part of the result, no matter on which node this method is invoked! This is,
	 * because {@code readUserIdentity} is not associated with a directory - it's global! Technically, it is assigned
	 * to the root (at least right now - this might change later), but semantically, it is not associated with any.
	 *
	 * @param userRepoKeyId the user-key's identifier for which to determine the permissions granted.
	 * @return the {@link PermissionType}s assigned to this node. Never <code>null</code>, but maybe empty!
	 * @see #grantPermission(PermissionType, PublicKey)
	 * @see #revokePermission(PermissionType, Set)
	 * @see #assertHasPermission(boolean, Uid, PermissionType, Date)
	 */
	public Set<PermissionType> getGrantedPermissionTypes(final Uid userRepoKeyId) {
		assertNotNull("userRepoKeyId", userRepoKeyId);
		final Set<PermissionType> result = EnumSet.noneOf(PermissionType.class);

		if (isOwner(userRepoKeyId)) // the owner has *all* permissions - always!
			result.addAll(Arrays.asList(PermissionType.values()));
		else {
			final PermissionDao pDao = context.transaction.getDao(PermissionDao.class);
			if (! pDao.getNonRevokedPermissions(PermissionType.readUserIdentity, userRepoKeyId).isEmpty())
				result.add(PermissionType.readUserIdentity);

			final PermissionSet permissionSet = getPermissionSet();
			if (permissionSet != null) {
				if (! pDao.getNonRevokedPermissions(permissionSet, PermissionType.grant, userRepoKeyId).isEmpty())
					result.add(PermissionType.grant);

				if (! pDao.getNonRevokedPermissions(permissionSet, PermissionType.write, userRepoKeyId).isEmpty())
					result.add(PermissionType.write);
			}

			if (hasReadPermissionHere(userRepoKeyId))
				result.add(PermissionType.read);
		}
		return result;
	}

	private boolean hasReadPermissionHereOrInherited(final Uid userRepoKeyId) {
		if (hasReadPermissionHere(userRepoKeyId))
			return true;

		if (isPermissionsInherited()) {
			final CryptreeNode parent = getParent();
			if (parent != null)
				return parent.hasReadPermissionHereOrInherited(userRepoKeyId);
		}
		return false;
	}

	private boolean hasReadPermissionHere(final Uid userRepoKeyId) {
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFile();
		if (cryptoRepoFile != null) { // If there is no CryptoRepoFile, there can be no read-access.
			final UserRepoKeyPublicKeyDao urkpkDao = context.transaction.getDao(UserRepoKeyPublicKeyDao.class);
			final UserRepoKeyPublicKey userRepoKeyPublicKey = urkpkDao.getUserRepoKeyPublicKey(userRepoKeyId);

			if (userRepoKeyPublicKey != null) { // if there is no UserRepoKeyPublicKey, there can be no read-access!
				final CryptoLinkDao cryptoLinkDao = context.transaction.getDao(CryptoLinkDao.class);
				final Collection<CryptoLink> cryptoLinks = cryptoLinkDao.getActiveCryptoLinks(
						cryptoRepoFile, CryptoKeyRole.clearanceKey, CryptoKeyPart.privateKey, userRepoKeyPublicKey);

				if (! cryptoLinks.isEmpty())
					return true;
			}
		}
		return false;
	}

	private void createUserIdentityLinksVisibleFor(final UserRepoKeyPublicKey userRepoKeyPublicKey) {
		// Not necessary, because UserRepoKeyPublicKeyHelper.createMissingUserIdentities() is invoked in every sync.
	}

	private boolean existsAtLeastOneUserIdentityLinkFor(final Set<Uid> userRepoKeyIds) {
		assertNotNull("userRepoKeyIds", userRepoKeyIds);

		final UserRepoKeyPublicKeyDao urkpkDao = getContext().transaction.getDao(UserRepoKeyPublicKeyDao.class);
		final UserIdentityLinkDao uilDao = getContext().transaction.getDao(UserIdentityLinkDao.class);

		for (Uid userRepoKeyId : userRepoKeyIds) {
			final UserRepoKeyPublicKey forUserRepoKeyPublicKey = urkpkDao.getUserRepoKeyPublicKeyOrFail(userRepoKeyId);
			final Collection<UserIdentityLink> userIdentityLinks = uilDao.getUserIdentityLinksFor(forUserRepoKeyPublicKey);
			if (! userIdentityLinks.isEmpty())
				return true;
		}

		return false;
	}

	private void revokeGrantPermissionOfAllCryptoRepoFiles(final Set<Uid> userRepoKeyIds) {
		assertNotNull("userRepoKeyIds", userRepoKeyIds);
		final PermissionDao dao = context.transaction.getDao(PermissionDao.class);

		final Set<CryptoRepoFile> cryptoRepoFiles = new HashSet<>();
		final Collection<Permission> permissions = dao.getNonRevokedPermissions(PermissionType.grant, userRepoKeyIds);
		for (final Permission permission : permissions)
			cryptoRepoFiles.add(permission.getPermissionSet().getCryptoRepoFile());

		for (final CryptoRepoFile cryptoRepoFile : cryptoRepoFiles)
			getContext().getCryptreeNodeOrCreate(cryptoRepoFile.getCryptoRepoFileId()).revokePermission(PermissionType.grant, userRepoKeyIds);
	}

	/**
	 * @param anyCryptoRepoFile <code>true</code> to indicate that the {@link Permission} does not need to be available on the
	 * level of this node's {@link CryptoRepoFile} (it is thus independent from this context); <code>false</code> to indicate
	 * that is must be available here on this level (i.e. granted directly or inherited from a parent).
	 * @param userRepoKeyId
	 * @param permissionType
	 * @param timestamp
	 */
	public void assertHasPermission(
			final boolean anyCryptoRepoFile, final Uid userRepoKeyId,
			final PermissionType permissionType, final Date timestamp
			) throws AccessDeniedException
	{
		assertNotNull("userRepoKeyId", userRepoKeyId);
		assertNotNull("permissionType", permissionType);
		assertNotNull("timestamp", timestamp);

		if (isOwner(userRepoKeyId))
			return; // The owner always has all permissions.

		String additionalExceptionMsg = null;
		final UserRepoKeyPublicKey userRepoKeyPublicKey = context.transaction.getDao(UserRepoKeyPublicKeyDao.class).getUserRepoKeyPublicKeyOrFail(userRepoKeyId);
		if (userRepoKeyPublicKey instanceof InvitationUserRepoKeyPublicKey) {
			final InvitationUserRepoKeyPublicKey invUserRepoKeyPublicKey = (InvitationUserRepoKeyPublicKey) userRepoKeyPublicKey;
			if (timestamp.compareTo(invUserRepoKeyPublicKey.getValidTo()) <= 0) {
				// Using a delegation via the invitiation-key is only allowed, if there is a corresponding replacement-request!
				// This reduces the potential to abuse this possibility (to grant access to other people).
				final UserRepoKeyPublicKeyReplacementRequestDao dao = context.transaction.getDao(UserRepoKeyPublicKeyReplacementRequestDao.class);
				if (dao.getUserRepoKeyPublicKeyReplacementRequestsForOldKey(invUserRepoKeyPublicKey).isEmpty())
					throwAccessDeniedException(permissionType, "There is no UserRepoKeyPublicKeyReplacementRequest for " + invUserRepoKeyPublicKey);

				final Uid signingUserRepoKeyId = invUserRepoKeyPublicKey.getSignature().getSigningUserRepoKeyId();
				assertHasPermission(anyCryptoRepoFile, signingUserRepoKeyId, permissionType, invUserRepoKeyPublicKey.getSignature().getSignatureCreated());
				// Using 'timestamp' here means the signing user must still have permissions, when the invitation was consumed.
				// This is maybe not perfect, but maybe it's exactly what we want... at least it should be OK. Or should be better
				// to use the timestamp of the invitation? This is what we do now.
				return;
			}
			else
				additionalExceptionMsg = String.format(
						"userRepoKeyPublicKey is an InvitationUserRepoKeyPublicKey, but it expired on '%s', which is before the given timestamp '%s'!",
						ISO8601.formatDate(invUserRepoKeyPublicKey.getValidTo()), ISO8601.formatDate(timestamp));
		}

		final Set<Permission> permissions = new HashSet<Permission>();

		if (PermissionType.read == permissionType) {
			long timeDifferenceToNow = Math.abs(System.currentTimeMillis() - timestamp.getTime());
			if (timeDifferenceToNow > 5 * 60 * 1000)
				throw new UnsupportedOperationException("assertHasPermission(...) does not yet support permissionType 'read' combined with a timestamp that is not *now*!");

			if (hasReadPermissionHereOrInherited(userRepoKeyId))
				return;
		}
		else {
			collectPermissions(permissions, anyCryptoRepoFile, permissionType, userRepoKeyId, timestamp);

			final Set<Permission> permissionsIndicatingBackdatedSignature = extractPermissionsIndicatingBackdatedSignature(permissions);

			if (! permissions.isEmpty())
				return; // all is fine => silently return.

			if (! permissionsIndicatingBackdatedSignature.isEmpty()) {
				final String exceptionMsg = String.format("Found '%s' permission(s) for userRepoKeyId=%s and timestamp=%s, but it (or they) indicates backdating outside of allowed range!", permissionType, userRepoKeyId, timestamp);
				throwAccessDeniedException(permissionType, exceptionMsg);
			}
		}

		final String exceptionMsg = String.format("No '%s' permission found for userRepoKeyId=%s and timestamp=%s!", permissionType, userRepoKeyId, ISO8601.formatDate(timestamp))
				+ (isEmpty(additionalExceptionMsg) ? "" : (" " + additionalExceptionMsg));
		throwAccessDeniedException(permissionType, exceptionMsg);
	}

	private void throwAccessDeniedException(final PermissionType permissionType, final String exceptionMessage) throws AccessDeniedException {
		switch (permissionType) {
			case grant:
				throw new GrantAccessDeniedException(exceptionMessage);
			case write:
				throw new WriteAccessDeniedException(exceptionMessage);
			case read:
				throw new ReadAccessDeniedException(exceptionMessage);
			case readUserIdentity:
				throw new ReadUserIdentityAccessDeniedException(exceptionMessage);
			default:
				throw new IllegalArgumentException("Unknown permissionType: " + permissionType);
		}
	}

	private boolean isOwner(final Uid userRepoKeyId) {
		assertNotNull("userRepoKeyId", userRepoKeyId);
		return userRepoKeyId.equals(context.getRepositoryOwnerOrFail().getUserRepoKeyPublicKey().getUserRepoKeyId());
	}

	/**
	 * @param anyCryptoRepoFile <code>true</code> to indicate that the {@link Permission} does not need to be available on the
	 * level of this node's {@link CryptoRepoFile} (it is thus independent from this context); <code>false</code> to indicate
	 * that is must be available here on this level (i.e. granted directly or inherited from a parent).
	 * @param permissionType
	 * @param userRepoKeyId
	 * @param timestamp
	 * @return
	 */
	private void collectPermissions(final Set<Permission> permissions, boolean anyCryptoRepoFile, final PermissionType permissionType, final Uid userRepoKeyId, final Date timestamp) {
		assertNotNull("permissionType", permissionType);
		assertNotNull("userRepoKeyId", userRepoKeyId);
		assertNotNull("timestamp", timestamp);

		// There is no Permission object with *read* permission. Hence, if we ever need to check this
		// here, we have to check it differently (=> tracing back the cryptree's crypto-links)!
		switch (permissionType) {
			case grant:
			case write:
				break;
			case readUserIdentity:
				anyCryptoRepoFile = true; // it's global!!! it cannot be assigned on a directory/file level!
				break;
			default:
				throw new IllegalArgumentException("PermissionType unknown or not allowed here: " + permissionType);
		}

//		final UserRepoKeyPublicKey userRepoKeyPublicKey = context.transaction.getDao(UserRepoKeyPublicKeyDao.class).getUserRepoKeyPublicKeyOrFail(userRepoKeyId);
//		if (userRepoKeyPublicKey instanceof InvitationUserRepoKeyPublicKey) {
//			final InvitationUserRepoKeyPublicKey invUserRepoKeyPublicKey = (InvitationUserRepoKeyPublicKey) userRepoKeyPublicKey;
//			if (timestamp.compareTo(invUserRepoKeyPublicKey.getValidTo()) <= 0) {
//				final Uid signingUserRepoKeyId = invUserRepoKeyPublicKey.getSignature().getSigningUserRepoKeyId();
//				collectPermissions(permissions, anyCryptoRepoFile, permissionType, signingUserRepoKeyId, invUserRepoKeyPublicKey.getSignature().getSignatureCreated());
//				// Using 'timestamp' here means the signing user must still have permissions, when the invitation was consumed.
//				// This is maybe not perfect, but maybe it's exactly what we want... at least it should be OK. Or should be better
//				// use the timestamp of the invitation?
//			}
//		}

		final PermissionSet permissionSet = anyCryptoRepoFile ? null : getPermissionSet();
		if (anyCryptoRepoFile || permissionSet != null) {
			final PermissionDao dao = context.transaction.getDao(PermissionDao.class);
			final Set<Permission> ps = anyCryptoRepoFile
					? new HashSet<>(dao.getValidPermissions(permissionType, userRepoKeyId, timestamp))
							: new HashSet<>(dao.getValidPermissions(permissionSet, permissionType, userRepoKeyId, timestamp));

			ps.removeAll(permissionsBeingCheckedNow);

			if (!ps.isEmpty()) {
				for (final Permission permission : ps)
					assertPermissionOk(permission); // TODO is this necessary? isn't it sufficient to check when it's written into the DB?
			}

			permissions.addAll(ps);
		}

		if (! anyCryptoRepoFile && (permissionSet == null || permissionSet.isPermissionsInherited(timestamp))) {
			final CryptreeNode parent = getParent();
			if (parent != null)
				parent.collectPermissions(permissions, anyCryptoRepoFile, permissionType, userRepoKeyId, timestamp);
		}
	}

	private Set<Permission> extractPermissionsIndicatingBackdatedSignature(final Set<Permission> permissions) {
		final Set<Permission> result = new HashSet<Permission>(permissions);
		Date backdatingOldestPermissionValidTo = null;

		if (getContext().isOnServer) { // We prevent backdating only on the server, because it is likely that a client didn't sync for ages
			// TODO not sure, if this is the best location for this test. but I think this code here is only called, if a new thingy is *written* on the server (never on read).
			for (final Iterator<Permission> it = permissions.iterator(); it.hasNext(); ) {
				final Permission permission = it.next();

				if (permission.getValidTo() != null) {
					if (backdatingOldestPermissionValidTo == null) {
						// We make it configurable per repository - not (yet) per subdir/file.
						final File file = getContext().transaction.getLocalRepoManager().getLocalRoot();
						backdatingOldestPermissionValidTo = new Date(System.currentTimeMillis() - CryptoConfigUtil.getBackdatingMaxPermissionValidToAge(file));
					}

					if (permission.getValidTo().before(backdatingOldestPermissionValidTo)) {
						result.add(permission);
						it.remove();
					}
				}
			}
		}
		return result;
	}

	private void assertPermissionOk(final Permission permission) throws SignatureException, AccessDeniedException {
		assertNotNull("permission", permission);

		if (permissionsAlreadyCheckedOk.contains(permission))
			return;

		if (! permissionsBeingCheckedNow.add(permission))
			throw new IllegalStateException("Circular permission check! " + permission);
		try {
			assertSignatureOk(permission);
			permissionsAlreadyCheckedOk.add(permission);
		} finally {
			permissionsBeingCheckedNow.remove(permission);
		}
	}

	public void assertSignatureOk(final WriteProtected entity) throws SignatureException, AccessDeniedException {
		assertNotNull("entity", entity);
		final Uid crfIdControllingPermissions = entity.getCryptoRepoFileIdControllingPermissions();
		if (crfIdControllingPermissions == null)
			this.assertSignatureOk(entity, true, entity.getPermissionTypeRequiredForWrite());
		else if (crfIdControllingPermissions.equals(this.getCryptoRepoFile().getCryptoRepoFileId()))
			this.assertSignatureOk(entity, false, entity.getPermissionTypeRequiredForWrite());
		else {
			final CryptreeNode cryptreeNode = context.getCryptreeNodeOrCreate(crfIdControllingPermissions);
			cryptreeNode.assertSignatureOk(entity, false, entity.getPermissionTypeRequiredForWrite());
		}
	}

	public void assertSignatureOk(
			final Signable signable, final boolean anyCryptoRepoFile,
			final PermissionType requiredPermissionType
			) throws SignatureException, AccessDeniedException
	{
		assertNotNull("signable", signable);
		// requiredPermissionType may be null, because a write-protected entity may not require any permission-type, but only behave like an ordinary Signable.

		// *always* verify signature!
		context.signableVerifier.verify(signable);

		if (requiredPermissionType != null) { // only check, if a requiredPermissionType was given.
			final Uid signingUserRepoKeyId = signable.getSignature().getSigningUserRepoKeyId();
			assertHasPermission(anyCryptoRepoFile, signingUserRepoKeyId, requiredPermissionType, signable.getSignature().getSignatureCreated());
		}
	}

	public PermissionSet getPermissionSet() {
		if (permissionSet == null) {
			final PermissionSetDao dao = context.transaction.getDao(PermissionSetDao.class);
			permissionSet = dao.getPermissionSet(getCryptoRepoFileOrCreate(false));

			if (permissionSet != null)
				assertSignatureOk(permissionSet);
		}
		return permissionSet;
	}

	public PermissionSet getPermissionSetOrCreate() {
		PermissionSet permissionSet = getPermissionSet();
		if (permissionSet == null) {
			permissionSet = new PermissionSet();
			permissionSet.setCryptoRepoFile(assertNotNull("getCryptoRepoFile()", getCryptoRepoFile()));
			sign(permissionSet);

			final PermissionSetDao dao = context.transaction.getDao(PermissionSetDao.class);
			this.permissionSet = permissionSet = dao.makePersistent(permissionSet);

			setPermissionsInherited(true); // this should be the default for a new PermissionSet
		}
		return permissionSet;
	}

	public void sign(final WriteProtected writeProtected) throws AccessDeniedException {
		assertNotNull("writeProtectedEntity", writeProtected);
		final Uid crfIdControllingPermissions = writeProtected.getCryptoRepoFileIdControllingPermissions();
		final UserRepoKey userRepoKey;
		if (crfIdControllingPermissions == null)
			userRepoKey = this.getUserRepoKeyOrFail(true, writeProtected.getPermissionTypeRequiredForWrite());
		else if (crfIdControllingPermissions.equals(this.getCryptoRepoFile().getCryptoRepoFileId()))
			userRepoKey = this.getUserRepoKeyOrFail(false, writeProtected.getPermissionTypeRequiredForWrite());
		else {
			final CryptreeNode cryptreeNode = context.getCryptreeNodeOrCreate(crfIdControllingPermissions);
			userRepoKey = cryptreeNode.getUserRepoKeyOrFail(false, writeProtected.getPermissionTypeRequiredForWrite());
		}
		context.getSignableSigner(userRepoKey).sign(writeProtected);
	}

	/**
	 * @param anyCryptoRepoFile <code>true</code> to indicate that the {@link Permission} does not need to be available on the
	 * level of this node's {@link CryptoRepoFile} (it is thus independent from this context); <code>false</code> to indicate
	 * that is must be available here on this level (i.e. granted directly or inherited from a parent).
	 * @param permissionType
	 * @return
	 */
	public UserRepoKey getUserRepoKey(final boolean anyCryptoRepoFile, final PermissionType permissionType) {
		assertNotNull("permissionType", permissionType);

		// There is no Permission object with *read* permission. Hence, if we ever need to check this
		// here, we have to check it differently (=> tracing back the cryptree's crypto-links)!
		switch (permissionType) {
			case grant:
			case write:
				break;
			default:
				throw new IllegalArgumentException("PermissionType unknown or not allowed here: " + permissionType);
		}


		final Date now = new Date();
		for (final UserRepoKey userRepoKey : context.userRepoKeyRing.getPermanentUserRepoKeys(context.serverRepositoryId)) {
			final boolean owner = isOwner(userRepoKey.getUserRepoKeyId());
			final Set<Permission> permissions = new HashSet<Permission>();
			if (! owner)
				collectPermissions(permissions, anyCryptoRepoFile, permissionType, userRepoKey.getUserRepoKeyId(), now);

			if (owner || ! permissions.isEmpty()) {
				getUserRepoKeyPublicKeyOrCreate(userRepoKey); // Make sure it is persisted in the DB.
				return userRepoKey;
			}
		}
		return null;
	}

	private UserRepoKey getUserRepoKeyOrFail(final boolean anyCryptoRepoFile, final PermissionType permissionType) {
		final UserRepoKey userRepoKey = getUserRepoKey(anyCryptoRepoFile, permissionType);

		if (userRepoKey == null) {
			final String message = String.format("No '%s' permission for any UserRepoKey of the current UserRepoKeyRing for: %s",
					permissionType, (repoFile != null ? repoFile.getPath() : cryptoRepoFile));
			switch (permissionType) {
				case grant:
					throw new GrantAccessDeniedException(message);
				case read:
					throw new ReadAccessDeniedException(message);
				case write:
					throw new WriteAccessDeniedException(message);
				default:
					throw new IllegalArgumentException("Unknown PermissionType: " + permissionType);
			}
		}
		return userRepoKey;
	}

	public void clearCryptoRepoFileDeleted() {
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFile();
		assertNotNull("cryptoRepoFile", cryptoRepoFile);

		deletePreliminaryDeletions();

		if (cryptoRepoFile.getDeleted() != null) {
			cryptoRepoFile.setDeleted(null);
			cryptoRepoFile.setDeletedByIgnoreRule(false);
			cryptoRepoFile.setLastSyncFromRepositoryId(null);
			sign(cryptoRepoFile);
		}
	}

	protected void deletePreliminaryDeletions() {
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFile();
		assertNotNull("cryptoRepoFile", cryptoRepoFile);

		PreliminaryDeletionDao pdDao = getContext().transaction.getDao(PreliminaryDeletionDao.class);
		PreliminaryDeletion preliminaryDeletion = pdDao.getPreliminaryDeletion(cryptoRepoFile);
		if (preliminaryDeletion != null)
			pdDao.deletePersistent(preliminaryDeletion);
	}
}
