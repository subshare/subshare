package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.assertNotNull;
import static org.subshare.local.CryptreeNodeUtil.createCryptoLink;
import static org.subshare.local.CryptreeNodeUtil.decrypt;
import static org.subshare.local.CryptreeNodeUtil.decryptLarge;
import static org.subshare.local.CryptreeNodeUtil.encrypt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.AccessDeniedException;
import org.subshare.core.GrantAccessDeniedException;
import org.subshare.core.ReadAccessDeniedException;
import org.subshare.core.WriteAccessDeniedException;
import org.subshare.core.crypto.CryptoConfigUtil;
import org.subshare.core.dto.SsRepoFileDto;
import org.subshare.core.dto.CryptoKeyPart;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.SignatureDto;
import org.subshare.core.sign.Signable;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.crypto.CipherOperationMode;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoKeyDeactivation;
import org.subshare.local.persistence.CryptoLink;
import org.subshare.local.persistence.CryptoLinkDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.InvitationUserRepoKeyPublicKey;
import org.subshare.local.persistence.Permission;
import org.subshare.local.persistence.PermissionDao;
import org.subshare.local.persistence.PermissionSet;
import org.subshare.local.persistence.PermissionSetDao;
import org.subshare.local.persistence.PermissionSetInheritance;
import org.subshare.local.persistence.RepositoryOwner;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKeyDao;
import org.subshare.local.persistence.UserRepoKeyPublicKeyReplacementRequestDao;
import org.subshare.local.persistence.WriteProtectedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
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
	private final RepoFileDtoConverter repoFileDtoConverter; // never null
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
		this.repoFileDtoConverter = RepoFileDtoConverter.create(context.transaction);

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

	public RepoFileDto getRepoFileDto() throws AccessDeniedException {
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFile();
		if (cryptoRepoFile == null)
			return null;

		final PlainCryptoKey plainCryptoKey = getPlainCryptoKeyForDecrypting(cryptoRepoFile.getCryptoKey());
		if (plainCryptoKey == null)
			throw new ReadAccessDeniedException(String.format("The CryptoRepoFile with cryptoRepoFileId=%s could not be decrypted! Access rights missing?!",
					cryptoRepoFile.getCryptoRepoFileId()));

		final byte[] plainRepoFileDtoData = assertNotNull("decrypt(...)", decrypt(cryptoRepoFile.getRepoFileDtoData(), plainCryptoKey));
		try {
			final InputStream in = new GZIPInputStream(new ByteArrayInputStream(plainRepoFileDtoData));
			final RepoFileDto repoFileDto = context.repoFileDtoIo.deserialize(in);
			in.close();
			return repoFileDto;
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}
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

			// TODO can we assert here, that this code is invoked on the client-side with the plain-text RepoFile?!

			// No need for local IDs. Because this DTO is shared across all repositories, local IDs make no sense.
			repoFileDtoConverter.setExcludeLocalIds(true);
			final RepoFileDto repoFileDto = repoFileDtoConverter.toRepoFileDto(repoFile, Integer.MAX_VALUE);

			((SsRepoFileDto) repoFileDto).setParentName(null); // only needed for uploading to the server.
			if (((SsRepoFileDto) repoFileDto).getSignature() != null) // must be null on the client - and this method is never called on the server.
				throw new IllegalStateException("repoFileDto.signature != null");

			// Prevent overriding the real name with "", if we checked out a sub-directory. In this case, we cannot
			// change the localName locally and must make sure, it is preserved.
			if (cryptoRepoFile.getLocalName() == null || repoFile.getParent() != null)
				cryptoRepoFile.setLocalName(repoFileDto.getName());
			else
				repoFileDto.setName(cryptoRepoFile.getLocalName());

			// Serialise to XML and compress.
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				final GZIPOutputStream gzOut = new GZIPOutputStream(out);
				context.repoFileDtoIo.serialize(repoFileDto, gzOut);
				gzOut.close();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}

			cryptoRepoFile.setRepoFileDtoData(assertNotNull("encrypt(...)", encrypt(out.toByteArray(), plainCryptoKey)));
			cryptoRepoFile.setLastSyncFromRepositoryId(null);

			sign(cryptoRepoFile);
		}
		return cryptoRepoFile;
	}

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
	 * @return
	 */
	public KeyParameter getDataKeyOrFail() {
		final CryptoRepoFile cryptoRepoFile = getCryptoRepoFile();
		assertNotNull("cryptoRepoFile", cryptoRepoFile);

		// We can use the following method, because it's *symmetric* - thus it works for both decrypting and encrypting!
		final PlainCryptoKey plainCryptoKey = getPlainCryptoKeyForDecrypting(cryptoRepoFile.getCryptoKey());
		if (plainCryptoKey == null)
			throw new ReadAccessDeniedException(String.format("Cannot decrypt dataKey for cryptoRepoFileId=%s!",
					cryptoRepoFile.getCryptoRepoFileId()));

		assertNotNull("plainCryptoKey.cryptoKey", plainCryptoKey.getCryptoKey());

		if (CryptoKeyRole.dataKey != plainCryptoKey.getCryptoKey().getCryptoKeyRole())
			throw new IllegalStateException("CryptoKeyRole.dataKey != plainCryptoKey.getCryptoKey().getCryptoKeyRole()");

		if (CryptoKeyPart.sharedSecret != plainCryptoKey.getCryptoKeyPart())
			throw new IllegalStateException("CryptoKeyPart.sharedSecret != plainCryptoKey.getCryptoKeyPart()");

		return plainCryptoKey.getKeyParameterOrFail();
	}

	public CryptreeNode getParent() {
		if (parent == null) {
			final RepoFile parentRepoFile = repoFile == null ? null : repoFile.getParent();
			final CryptoRepoFile parentCryptoRepoFile =  cryptoRepoFile == null ? null : cryptoRepoFile.getParent();

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
		final UserRepoKeyPublicKeyDao dao = context.transaction.getDao(UserRepoKeyPublicKeyDao.class);
		final UserRepoKeyPublicKey userRepoKeyPublicKey = dao.getUserRepoKeyPublicKeyOrCreate(publicKey);
		return userRepoKeyPublicKey;
	}

	public void grantPermission(final PermissionType permissionType, final UserRepoKey.PublicKey publicKey) {
		assertNotNull("permissionType", permissionType);
		assertNotNull("publicKey", publicKey);

		// It is technically required to have read permission, when having write or grant permission. Therefore,
		// we simply grant it always, here.
		grantReadPermission(publicKey);
		if (PermissionType.read == permissionType)
			return;

		final Uid ownerUserRepoKeyId = context.getRepositoryOwnerOrFail().getUserRepoKeyPublicKey().getUserRepoKeyId();
		if (ownerUserRepoKeyId.equals(publicKey.getUserRepoKeyId()))
			return;

		// It is technically required to have write permission, when having grant permission. Therefore, we
		// grant it here, too.
		if (PermissionType.grant == permissionType)
			grantPermission(PermissionType.write, publicKey);

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

	public void revokePermission(final PermissionType permissionType, final Set<Uid> userRepoKeyIds) {
		assertNotNull("permissionType", permissionType);
		assertNotNull("userRepoKeyIds", userRepoKeyIds);
		if (PermissionType.read == permissionType) {
			// Since it is technically required to have read permission, when having write or grant permission, we
			// revoke grant and write permission, too.
			revokePermission(PermissionType.write, userRepoKeyIds);
			revokePermission(PermissionType.grant, userRepoKeyIds);

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
		if (isOwner(userRepoKeyId))
			return; // The owner always has all permissions.

		final UserRepoKeyPublicKey userRepoKeyPublicKey = context.transaction.getDao(UserRepoKeyPublicKeyDao.class).getUserRepoKeyPublicKeyOrFail(userRepoKeyId);
		if (userRepoKeyPublicKey instanceof InvitationUserRepoKeyPublicKey) {
			final InvitationUserRepoKeyPublicKey invUserRepoKeyPublicKey = (InvitationUserRepoKeyPublicKey) userRepoKeyPublicKey;
			if (timestamp.compareTo(invUserRepoKeyPublicKey.getValidTo()) <= 0) {
				// Using a delegation via the invitiation-key is only allowed, if there is a corresponding replacement-request!
				// This reduces the potential to abuse this possibility (to grant access to other people).
				final UserRepoKeyPublicKeyReplacementRequestDao dao = context.transaction.getDao(UserRepoKeyPublicKeyReplacementRequestDao.class);
				if (dao.getUserRepoKeyPublicKeyReplacementRequestsForOldKey(invUserRepoKeyPublicKey).isEmpty())
					throw new IllegalStateException("There is no UserRepoKeyPublicKeyReplacementRequest for " + invUserRepoKeyPublicKey);

				final Uid signingUserRepoKeyId = invUserRepoKeyPublicKey.getSignature().getSigningUserRepoKeyId();
				assertHasPermission(anyCryptoRepoFile, signingUserRepoKeyId, permissionType, invUserRepoKeyPublicKey.getSignature().getSignatureCreated());
				// Using 'timestamp' here means the signing user must still have permissions, when the invitation was consumed.
				// This is maybe not perfect, but maybe it's exactly what we want... at least it should be OK. Or should be better
				// to use the timestamp of the invitation? This is what we do now.
				return;
			}
		}

		final Set<Permission> permissions = new HashSet<Permission>();
		collectPermissions(permissions, anyCryptoRepoFile, permissionType, userRepoKeyId, timestamp);
		final Set<Permission> permissionsIndicatingBackdatedSignature = extractPermissionsIndicatingBackdatedSignature(permissions);

		if (! permissions.isEmpty())
			return; // all is fine => silently return.

		if (! permissionsIndicatingBackdatedSignature.isEmpty()) {
			final String exceptionMsg = String.format("Found '%s' permission(s) for userRepoKeyId=%s and timestamp=%s, but it (or they) indicates backdating outside of allowed range!", permissionType, userRepoKeyId, timestamp);
			switch (permissionType) {
				case grant:
					throw new GrantAccessDeniedException(exceptionMsg);
				case write:
					throw new WriteAccessDeniedException(exceptionMsg);
				default:
					throw new IllegalArgumentException("Unknown permissionType: " + permissionType);
			}
		}

		final String exceptionMsg = String.format("No '%s' permission found for userRepoKeyId=%s and timestamp=%s!", permissionType, userRepoKeyId, timestamp);
		switch (permissionType) {
			case grant:
				throw new GrantAccessDeniedException(exceptionMsg);
			case write:
				throw new WriteAccessDeniedException(exceptionMsg);
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
	private void collectPermissions(final Set<Permission> permissions, final boolean anyCryptoRepoFile, final PermissionType permissionType, final Uid userRepoKeyId, final Date timestamp) {
		assertNotNull("permissionType", permissionType);
		assertNotNull("userRepoKeyId", userRepoKeyId);
		assertNotNull("timestamp", timestamp);

		// There is no Permission object with *read* permission. Hence, if we ever need to check this
		// here, we have to check it differently (=> tracing back the cryptree's crypto-links)!
		switch (permissionType) {
			case grant:
			case write:
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

	public void assertSignatureOk(final WriteProtectedEntity entity) throws SignatureException, AccessDeniedException {
		assertNotNull("entity", entity);
		final CryptoRepoFile cryptoRepoFileControllingPermissions = entity.getCryptoRepoFileControllingPermissions();
		if (cryptoRepoFileControllingPermissions == null)
			this.assertSignatureOk(entity, true, entity.getPermissionTypeRequiredForWrite());
		else if (cryptoRepoFileControllingPermissions.equals(this.getCryptoRepoFile()))
			this.assertSignatureOk(entity, false, entity.getPermissionTypeRequiredForWrite());
		else {
			final CryptreeNode cryptreeNode = context.getCryptreeNodeOrCreate(cryptoRepoFileControllingPermissions.getCryptoRepoFileId());
			cryptreeNode.assertSignatureOk(entity, false, entity.getPermissionTypeRequiredForWrite());
		}
	}

	public void assertSignatureOk(
			final Signable signable, final boolean anyCryptoRepoFile,
			final PermissionType requiredPermissionType
			) throws SignatureException, AccessDeniedException
	{
		assertNotNull("signable", signable);
		assertNotNull("requiredPermissionType", requiredPermissionType);
		context.signableVerifier.verify(signable);
		final Uid signingUserRepoKeyId = signable.getSignature().getSigningUserRepoKeyId();
		assertHasPermission(anyCryptoRepoFile, signingUserRepoKeyId, requiredPermissionType, signable.getSignature().getSignatureCreated());
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

	public void sign(final WriteProtectedEntity writeProtectedEntity) throws AccessDeniedException {
		assertNotNull("writeProtectedEntity", writeProtectedEntity);
		final CryptoRepoFile cryptoRepoFileControllingPermissions = writeProtectedEntity.getCryptoRepoFileControllingPermissions();
		final UserRepoKey userRepoKey;
		if (cryptoRepoFileControllingPermissions == null)
			userRepoKey = this.getUserRepoKeyOrFail(true, writeProtectedEntity.getPermissionTypeRequiredForWrite());
		else if (cryptoRepoFileControllingPermissions.equals(this.getCryptoRepoFile()))
			userRepoKey = this.getUserRepoKeyOrFail(false, writeProtectedEntity.getPermissionTypeRequiredForWrite());
		else {
			final CryptreeNode cryptreeNode = context.getCryptreeNodeOrCreate(cryptoRepoFileControllingPermissions.getCryptoRepoFileId());
			userRepoKey = cryptreeNode.getUserRepoKeyOrFail(false, writeProtectedEntity.getPermissionTypeRequiredForWrite());
		}
		context.getSignableSigner(userRepoKey).sign(writeProtectedEntity);
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
}
