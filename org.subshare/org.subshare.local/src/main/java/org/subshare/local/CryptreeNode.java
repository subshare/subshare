package org.subshare.local;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.local.CryptreeNodeUtil.*;

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
import org.subshare.core.dto.SsRepoFileDto;
import org.subshare.core.dto.CryptoKeyPart;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.dto.PermissionType;
import org.subshare.core.dto.SignatureDto;
import org.subshare.core.sign.Signable;
import org.subshare.core.user.UserRepoKey;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoLink;
import org.subshare.local.persistence.CryptoLinkDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;
import org.subshare.local.persistence.Permission;
import org.subshare.local.persistence.PermissionDao;
import org.subshare.local.persistence.PermissionSet;
import org.subshare.local.persistence.PermissionSetDao;
import org.subshare.local.persistence.UserRepoKeyPublicKey;
import org.subshare.local.persistence.UserRepoKeyPublicKeyDao;
import org.subshare.local.persistence.WriteProtectedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;
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

	private CryptreeNode(final CryptreeNode parent, final CryptreeNode child, final CryptreeContext context, final RepoFile repoFile, final CryptoRepoFile cryptoRepoFile) {
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

			final PlainCryptoKey plainCryptoKey = getActivePlainCryptoKeyOrCreate(CryptoKeyRole.dataKey, CryptoKeyPart.sharedSecret);
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
		final PlainCryptoKey plainCryptoKey = getActivePlainCryptoKeyOrCreate(CryptoKeyRole.clearanceKey, CryptoKeyPart.privateKey);
		createCryptoLink(this, getUserRepoKeyPublicKey(publicKey), plainCryptoKey);
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
			makeCryptoKeyAndDescendantsNonActive(cryptoLink.getToCryptoKey(), processedCryptoKeys); // likely the same CryptoKey for all cryptoLinks => deduplicate via Set

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
				clearanceKeyPlainCryptoKey = getActivePlainCryptoKeyOrCreate(CryptoKeyRole.clearanceKey, CryptoKeyPart.privateKey);

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
			getActivePlainCryptoKeyOrCreate(CryptoKeyRole.backlinkKey, CryptoKeyPart.sharedSecret);
	}

	private void makeCryptoKeyAndDescendantsNonActive(final CryptoKey cryptoKey, final Set<CryptoKey> processedCryptoKeys) {
		if (! processedCryptoKeys.add(cryptoKey))
			return;

		cryptoKey.setActive(false);
		sign(cryptoKey);
		for (final CryptoLink cryptoLink : cryptoKey.getOutCryptoLinks())
			makeCryptoKeyAndDescendantsNonActive(cryptoLink.getToCryptoKey(), processedCryptoKeys);
	}

	private void createSubdirKeyAndBacklinkKeyIfNeededChildrenRecursively() {
		// Only directories have a subdirKey (and further children). The backlinkKeys of the files
		// are optional and didn't get dirty, because they are not in the CryptoLink-chain of the parent.
		// The backlinkKey only got dirty, if this is a file. We handle this separately in
		// createBacklinkKeyIfNeededFile().
		if (! isDirectory())
			return;

		getActivePlainCryptoKeyOrCreate(CryptoKeyRole.subdirKey, CryptoKeyPart.sharedSecret);
		getActivePlainCryptoKeyOrCreate(CryptoKeyRole.backlinkKey, CryptoKeyPart.sharedSecret);

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
					children.add(new CryptreeNode(this, null, getContext(), null, childCryptoRepoFile));
			}
			else if (repoFile != null) {
				final RepoFileDao dao = context.transaction.getDao(RepoFileDao.class);
				final Collection<RepoFile> childRepoFiles = dao.getChildRepoFiles(repoFile);
				for (final RepoFile childRepoFile : childRepoFiles)
					children.add(new CryptreeNode(this, null, getContext(), childRepoFile, null));
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

	protected PlainCryptoKey getActivePlainCryptoKey(final CryptoKeyRole toCryptoKeyRole, final CryptoKeyPart toCryptoKeyPart) {
		assertNotNull("toCryptoKeyRole", toCryptoKeyRole);
		assertNotNull("toCryptoKeyPart", toCryptoKeyPart);
		logger.debug("getActivePlainCryptoKey: cryptoRepoFile={} repoFile={} toCryptoKeyRole={} toCryptoKeyPart={}",
				cryptoRepoFile, repoFile, toCryptoKeyRole, toCryptoKeyPart);
		final CryptoLinkDao cryptoLinkDao = context.transaction.getDao(CryptoLinkDao.class);
		final Collection<CryptoLink> cryptoLinks = cryptoLinkDao.getActiveCryptoLinks(getCryptoRepoFile(), toCryptoKeyRole, toCryptoKeyPart);
		return getPlainCryptoKey(cryptoLinks, toCryptoKeyPart);
	}

	protected PlainCryptoKey getPlainCryptoKeyForDecrypting(final CryptoKey cryptoKey) {
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

	protected PlainCryptoKey getActivePlainCryptoKeyOrCreate(final CryptoKeyRole toCryptoKeyRole, final CryptoKeyPart toCryptoKeyPart) {
		assertNotNull("toCryptoKeyRole", toCryptoKeyRole);
		assertNotNull("toCryptoKeyPart", toCryptoKeyPart);
		PlainCryptoKey plainCryptoKey = getActivePlainCryptoKey(toCryptoKeyRole, toCryptoKeyPart);
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
			factory.setCryptoKeyPart(toCryptoKeyPart);
			plainCryptoKey = factory.createPlainCryptoKey();
			assertNotNull(clazz.getName() + ".createPlainCryptoKey()", plainCryptoKey);

			if (plainCryptoKey.getCryptoKey().getCryptoKeyRole() != toCryptoKeyRole)
				throw new IllegalStateException(String.format("plainCryptoKey.cryptoKey.cryptoKeyRole != toCryptoKeyRole :: %s != %s", plainCryptoKey.getCryptoKey().getCryptoKeyRole(), toCryptoKeyRole));

			if (plainCryptoKey.getCryptoKeyPart() != toCryptoKeyPart)
				throw new IllegalStateException(String.format("plainCryptoKey.cryptoKeyPart != toCryptoKeyPart :: %s != %s", plainCryptoKey.getCryptoKeyPart(), toCryptoKeyPart));

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
				parent = new CryptreeNode(null, this, getContext(), parentRepoFile, parentCryptoRepoFile);
		}
		return parent;
	}

//	public UserRepoKeyPublicKey getUserRepoKeyPublicKey() {
//		return userRepoKeyPublicKey;
//	}

	public UserRepoKeyPublicKey getUserRepoKeyPublicKey(final UserRepoKey userRepoKey) {
		assertNotNull("userRepoKey", userRepoKey);
		return getUserRepoKeyPublicKey(userRepoKey.getPublicKey());
	}

	public UserRepoKeyPublicKey getUserRepoKeyPublicKey(final UserRepoKey.PublicKey publicKey) {
		assertNotNull("publicKey", publicKey);
		final UserRepoKeyPublicKeyDao dao = context.transaction.getDao(UserRepoKeyPublicKeyDao.class);
		UserRepoKeyPublicKey userRepoKeyPublicKey = dao.getUserRepoKeyPublicKey(publicKey.getUserRepoKeyId());
		if (userRepoKeyPublicKey == null)
			userRepoKeyPublicKey = dao.makePersistent(new UserRepoKeyPublicKey(publicKey));

		return userRepoKeyPublicKey;
	}

//	public void grantGrantPermission(final UserRepoKey.PublicKey publicKey) {
//		grantPermission(PermissionType.grant, publicKey);
//	}
//
//	public void grantWritePermission(final UserRepoKey.PublicKey publicKey) {
//		grantPermission(PermissionType.write, publicKey);
//	}
//
//	public void revokeGrantPermission(final Set<Uid> userRepoKeyIds) {
//		revokePermission(PermissionType.grant, userRepoKeyIds);
//	}
//
//	public void revokeWritePermission(final Set<Uid> userRepoKeyIds) {
//		revokePermission(PermissionType.write, userRepoKeyIds);
//	}

	public void grantPermission(final PermissionType permissionType, final UserRepoKey.PublicKey publicKey) {
		assertNotNull("permissionType", permissionType);
		assertNotNull("publicKey", publicKey);
		if (PermissionType.read == permissionType) {
			grantReadPermission(publicKey);
			return;
		}

		final Uid ownerUserRepoKeyId = context.getRepositoryOwnerOrFail().getUserRepoKeyPublicKey().getUserRepoKeyId();
		if (ownerUserRepoKeyId.equals(publicKey.getUserRepoKeyId()))
			return;

		final PermissionSet permissionSet = getPermissionSetOrCreate();
		final PermissionDao dao = context.transaction.getDao(PermissionDao.class);
		final UserRepoKeyPublicKey userRepoKeyPublicKey = getUserRepoKeyPublicKey(publicKey);
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
	}

	public void revokePermission(final PermissionType permissionType, final Set<Uid> userRepoKeyIds) {
		assertNotNull("permissionType", permissionType);
		assertNotNull("userRepoKeyIds", userRepoKeyIds);
		if (PermissionType.read == permissionType) {
			revokeReadPermission(userRepoKeyIds);
			return;
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

//	public void assertHasGrantPermission(final Uid userRepoKeyId, final Date timestamp) {
//		assertNotNull("userRepoKeyId", userRepoKeyId);
//		assertNotNull("timestamp", timestamp);
//		assertHasPermission(PermissionType.grant, userRepoKeyId, timestamp);
//	}

	private void assertHasPermission(final PermissionType permissionType, final Uid userRepoKeyId, final Date timestamp) {
		if (hasPermission(permissionType, userRepoKeyId, timestamp))
			return; // all is fine => silently return.

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

	private boolean hasPermission(final PermissionType permissionType, final Uid userRepoKeyId, final Date timestamp) {
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

		if (userRepoKeyId.equals(context.getRepositoryOwnerOrFail().getUserRepoKeyPublicKey().getUserRepoKeyId()))
			return true; // The owner always has all permissions.

		final PermissionSet permissionSet = getPermissionSet();
		if (permissionSet != null) {
			final PermissionDao dao = context.transaction.getDao(PermissionDao.class);
			final Set<Permission> permissions = new HashSet<>(dao.getValidPermissions(permissionSet, permissionType, userRepoKeyId, timestamp));

			permissions.removeAll(permissionsBeingCheckedNow);

			if (!permissions.isEmpty()) {
				for (final Permission permission : permissions)
					assertPermissionOk(permission);

				return true; // We found a valid permission in this directory/file level.
			}
		}

		if (permissionSet == null || permissionSet.isPermissionsInherited()) {
			final CryptreeNode parent = getParent();
			if (parent != null)
				return parent.hasPermission(permissionType, userRepoKeyId, timestamp);
		}

		return false; // If we come here, there is no permission.
	}

	private void assertPermissionOk(final Permission permission) throws SignatureException {
		assertNotNull("permission", permission);
		if (permissionsAlreadyCheckedOk.contains(permission))
			return;

		if (! permissionsBeingCheckedNow.add(permission))
			throw new IllegalStateException("Circular permission check! " + permission);
		try {
			assertSignatureOk(permission, PermissionType.grant);
			permissionsAlreadyCheckedOk.add(permission);
		} finally {
			permissionsBeingCheckedNow.remove(permission);
		}
	}

	public void assertSignatureOk(final Signable signable, final PermissionType requiredPermissionType) throws SignatureException {
		assertNotNull("signable", signable);
		assertNotNull("requiredPermissionType", requiredPermissionType);
		context.signableVerifier.verify(signable);
		final Uid signingUserRepoKeyId = signable.getSignature().getSigningUserRepoKeyId();
		assertHasPermission(requiredPermissionType, signingUserRepoKeyId, signable.getSignature().getSignatureCreated());
	}

	public PermissionSet getPermissionSet() {
		if (permissionSet == null) {
			final PermissionSetDao dao = context.transaction.getDao(PermissionSetDao.class);
			permissionSet = dao.getPermissionSet(getCryptoRepoFileOrCreate(false));

			if (permissionSet != null)
				assertSignatureOk(permissionSet, PermissionType.grant);
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
		}
		return permissionSet;
	}

//	public UserRepoKey getUserRepoKeyForGrant() {
//		return getUserRepoKeyFor(PermissionType.grant);
//	}
//
//	public UserRepoKey getUserRepoKeyForGrantOrFail() {
//		final UserRepoKey userRepoKey = getUserRepoKeyForGrant();
//		if (userRepoKey == null)
//			throw new GrantAccessDeniedException("No 'grant' permission for any UserRepoKey of the current UserRepoKeyRing for: " + (repoFile != null ? repoFile.getPath() : cryptoRepoFile));
//
//		return userRepoKey;
//	}
//
//	public UserRepoKey getUserRepoKeyForWrite() {
//		return getUserRepoKeyFor(PermissionType.write);
//	}
//
//	public UserRepoKey getUserRepoKeyForWriteOrFail() {
//		final UserRepoKey userRepoKey = getUserRepoKeyForWrite();
//		if (userRepoKey == null)
//			throw new WriteAccessDeniedException("No 'write' permission for any UserRepoKey of the current UserRepoKeyRing for: " + (repoFile != null ? repoFile.getPath() : cryptoRepoFile));
//
//		return userRepoKey;
//	}

	public void sign(final WriteProtectedEntity writeProtectedEntity) {
		assertNotNull("writeProtectedEntity", writeProtectedEntity);
		final UserRepoKey userRepoKey = getUserRepoKeyForOrFail(writeProtectedEntity.getPermissionTypeRequiredForWrite());
		context.getSignableSigner(userRepoKey).sign(writeProtectedEntity);
	}

	public UserRepoKey getUserRepoKeyFor(final PermissionType permissionType) {
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
		for (final UserRepoKey userRepoKey : context.userRepoKeyRing.getUserRepoKeys(context.serverRepositoryId)) {
			if (hasPermission(permissionType, userRepoKey.getUserRepoKeyId(), now)) {
				getUserRepoKeyPublicKey(userRepoKey); // Make sure it is persisted in the DB.
				return userRepoKey;
			}
		}
		return null;
	}

	private UserRepoKey getUserRepoKeyForOrFail(final PermissionType permissionType) {
		final UserRepoKey userRepoKey = getUserRepoKeyFor(permissionType);

		if (userRepoKey == null) {
			switch (permissionType) {
				case grant:
					throw new GrantAccessDeniedException("No 'grant' permission for any UserRepoKey of the current UserRepoKeyRing for: " + (repoFile != null ? repoFile.getPath() : cryptoRepoFile));
				case write:
					throw new WriteAccessDeniedException("No 'write' permission for any UserRepoKey of the current UserRepoKeyRing for: " + (repoFile != null ? repoFile.getPath() : cryptoRepoFile));
				default:
					throw new IllegalArgumentException("PermissionType unknown or not allowed here: " + permissionType);
			}
		}
		return userRepoKey;
	}
}
