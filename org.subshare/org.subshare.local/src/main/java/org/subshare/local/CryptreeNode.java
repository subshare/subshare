package org.subshare.local;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.local.CryptreeNodeUtil.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.dto.CryptoKeyPart;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDao;
import org.subshare.local.persistence.CryptoLink;
import org.subshare.local.persistence.CryptoLinkDao;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDao;

import co.codewizards.cloudstore.core.dto.RepoFileDto;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.dto.jaxb.RepoFileDtoIo;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.dto.RepoFileDtoConverter;
import co.codewizards.cloudstore.local.persistence.Directory;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class CryptreeNode {

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

	private final UserRepoKeyRing userRepoKeyRing; // never null
	private final UserRepoKey userRepoKey; // never null
	private CryptreeNode parent; // maybe null - lazily loaded, if there is one
	private final LocalRepoTransaction transaction; // never null
	private RepoFile repoFile; // maybe null - lazily loaded
	private CryptoRepoFile cryptoRepoFile; // maybe null - lazily loaded
	private final RepoFileDtoConverter repoFileDtoConverter; // never null
	private final List<CryptreeNode> children = new ArrayList<CryptreeNode>(0);
	private boolean childrenLoaded = false;
	private final RepoFileDtoIo repoFileDtoIo; // never null

	public CryptreeNode(final UserRepoKey userRepoKey, final LocalRepoTransaction transaction, final RepoFile repoFile) {
		this(userRepoKey, transaction, repoFile, null);
	}

	private CryptreeNode(final UserRepoKey userRepoKey, final LocalRepoTransaction transaction, final RepoFile repoFile, final CryptoRepoFile cryptoRepoFile) {
		this(null, null, userRepoKey,
				assertNotNull("transaction", transaction), repoFile, cryptoRepoFile);
	}

	private CryptreeNode(final CryptreeNode parent, final CryptreeNode child, final UserRepoKey userRepoKey, final LocalRepoTransaction transaction, final RepoFile repoFile, final CryptoRepoFile cryptoRepoFile) {
		if (parent == null && child == null && userRepoKey == null)
			throw new IllegalArgumentException("parent == null && child == null && userRepoKey == null");

		if (parent == null && child == null && transaction == null)
			throw new IllegalArgumentException("parent == null && child == null && transaction == null");

		if (repoFile == null && cryptoRepoFile == null)
			throw new IllegalArgumentException("repoFile == null && cryptoRepoFile == null");

		this.parent = parent;
		this.userRepoKey = userRepoKey != null ? userRepoKey
				: (parent != null ? parent.getUserRepoKey() : child.getUserRepoKey());
		this.userRepoKeyRing = assertNotNull("userRepoKey.userRepoKeyRing", userRepoKey.getUserRepoKeyRing());
		this.transaction = transaction != null ? transaction
				: (parent != null ? parent.getTransaction() : child.getTransaction());

		this.repoFile = repoFile != null ? repoFile : cryptoRepoFile.getRepoFile();
		this.cryptoRepoFile = cryptoRepoFile;
		this.repoFileDtoConverter = new RepoFileDtoConverter(transaction);

		this.repoFileDtoIo = parent != null ? parent.repoFileDtoIo
				: (child != null ? child.repoFileDtoIo : new RepoFileDtoIo());

		if (child != null)
			children.add(child);
	}

	protected UserRepoKeyRing getUserRepoKeyRing() {
		return userRepoKeyRing;
	}

	protected UserRepoKey getUserRepoKey() {
		return userRepoKey;
	}

	protected LocalRepoTransaction getTransaction() {
		return transaction;
	}

	public RepoFile getRepoFile() {
		if (repoFile == null) {


			repoFile = null; // TODO find/create!
			throw new UnsupportedOperationException("NYI!");
		}
		return repoFile;
	}

	public CryptoRepoFile getCryptoRepoFile() {
		if (cryptoRepoFile == null) {
			if (repoFile == null) // at least one of them must be there!
				throw new IllegalArgumentException("repoFile == null && cryptoRepoFile == null");

			final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);
			cryptoRepoFile = cryptoRepoFileDao.getCryptoRepoFile(repoFile);
		}
		return cryptoRepoFile;
	}

	public CryptoRepoFile getCryptoRepoFileOrCreate(final boolean update) {
		CryptoRepoFile cryptoRepoFile = getCryptoRepoFile();
		if (cryptoRepoFile == null || update) {
			final CryptoRepoFileDao cryptoRepoFileDao = transaction.getDao(CryptoRepoFileDao.class);

			if (cryptoRepoFile == null)
				cryptoRepoFile = new CryptoRepoFile();

			cryptoRepoFile.setRepoFile(repoFile);

			// getPlainCryptoKeyOrCreate(...) causes this method to be called again. We thus must prevent
			// an eternal recursion by already assigning it to the field *now*.
			// Furthermore, CryptoKey has a reference to this object and will thus persist it, anyway.
			// Thus we explicitly persist it already here.
			this.cryptoRepoFile = cryptoRepoFile = cryptoRepoFileDao.makePersistent(cryptoRepoFile);

			final PlainCryptoKey plainCryptoKey = getPlainCryptoKeyOrCreate(CryptoKeyRole.dataKey, CryptoKeyPart.sharedSecret);
			final CryptoKey cryptoKey = assertNotNull("plainCryptoKey", plainCryptoKey).getCryptoKey();
			cryptoRepoFile.setCryptoKey(assertNotNull("plainCryptoKey.cryptoKey", cryptoKey));

			final CryptreeNode parent = getParent();
			cryptoRepoFile.setParent(parent == null ? null : parent.getCryptoRepoFile());

			// TODO can we assert here, that this code is invoked on the client-side with the plain-text RepoFile?!

			// No need for local IDs. Because this DTO is shared across all repositories, local IDs make no sense.
			repoFileDtoConverter.setExcludeLocalIds(true);
			final RepoFileDto repoFileDto = repoFileDtoConverter.toRepoFileDto(repoFile, Integer.MAX_VALUE);

			// Serialise to XML and compress.
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				final GZIPOutputStream gzOut = new GZIPOutputStream(out);
				repoFileDtoIo.serialize(repoFileDto, gzOut);
				gzOut.close();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}

			cryptoRepoFile.setRepoFileDtoData(encrypt(out.toByteArray(), plainCryptoKey));
			cryptoRepoFile.setLastSyncFromRepositoryId(null);
		}
		return cryptoRepoFile;
	}

	public Collection<CryptreeNode> getChildren() {
		if (! childrenLoaded) {
			// TODO load children!

			childrenLoaded = true;
		}
		return Collections.unmodifiableList(children);
	}

	protected boolean isDirectory() {
		return getRepoFile() instanceof Directory;
	}

	protected PlainCryptoKey getPlainCryptoKey(final CryptoKeyRole toCryptoKeyRole, final CryptoKeyPart toCryptoKeyPart) {
		assertNotNull("toCryptoKeyRole", toCryptoKeyRole);
		assertNotNull("toCryptoKeyPart", toCryptoKeyPart);
		final CryptoLinkDao cryptoLinkDao = transaction.getDao(CryptoLinkDao.class);
		final Collection<CryptoLink> cryptoLinks = cryptoLinkDao.getActiveCryptoLinks(getCryptoRepoFile(), toCryptoKeyRole, toCryptoKeyPart);
		return getPlainCryptoKey(cryptoLinks, toCryptoKeyPart);
	}

	private PlainCryptoKey getPlainCryptoKey(final Collection<CryptoLink> cryptoLinks, final CryptoKeyPart toCryptoKeyPart) {
		assertNotNull("cryptoLinks", cryptoLinks);
		assertNotNull("toCryptoKeyPart", toCryptoKeyPart);
		for (final CryptoLink cryptoLink : cryptoLinks) {
			if (toCryptoKeyPart != cryptoLink.getToCryptoKeyPart())
				continue;

			final Uid fromUserRepoKeyId = cryptoLink.getFromUserRepoKeyId();
			if (fromUserRepoKeyId != null) {
				final UserRepoKey userRepoKey = getUserRepoKeyRing().getUserRepoKey(fromUserRepoKeyId);
				if (userRepoKey != null) {
					final byte[] plain = decryptLarge(cryptoLink.getToCryptoKeyData(), userRepoKey);
					return new PlainCryptoKey(cryptoLink.getToCryptoKey(), cryptoLink.getToCryptoKeyPart(), plain);
				}
			}
			else if (cryptoLink.getFromCryptoKey() == null) {
				// *not* encrypted
				return new PlainCryptoKey(cryptoLink.getToCryptoKey(), cryptoLink.getToCryptoKeyPart(), cryptoLink.getToCryptoKeyData());
			}
		}

		for (final CryptoLink cryptoLink : cryptoLinks) {
			if (toCryptoKeyPart != cryptoLink.getToCryptoKeyPart())
				continue;

			final CryptoKey fromCryptoKey = cryptoLink.getFromCryptoKey();
			if (fromCryptoKey != null) {
				final PlainCryptoKey plainFromCryptoKey = getPlainCryptoKey(
						fromCryptoKey.getInCryptoLinks(), getCryptoKeyPartForDecrypting(fromCryptoKey));
				if (plainFromCryptoKey != null) {
					final byte[] plain = decrypt(
							cryptoLink.getToCryptoKeyData(),
							plainFromCryptoKey);
					return new PlainCryptoKey(cryptoLink.getToCryptoKey(), cryptoLink.getToCryptoKeyPart(), plain);
				}
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

	protected PlainCryptoKey getPlainCryptoKeyOrCreate(final CryptoKeyRole toCryptoKeyRole, final CryptoKeyPart toCryptoKeyPart) {
		assertNotNull("toCryptoKeyRole", toCryptoKeyRole);
		assertNotNull("toCryptoKeyPart", toCryptoKeyPart);
		PlainCryptoKey plainCryptoKey = getPlainCryptoKey(toCryptoKeyRole, toCryptoKeyPart);
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

			final CryptoKeyDao cryptoKeyDao = transaction.getDao(CryptoKeyDao.class);
			final CryptoKey cryptoKey = cryptoKeyDao.makePersistent(plainCryptoKey.getCryptoKey());
			plainCryptoKey = new PlainCryptoKey(cryptoKey, plainCryptoKey.getCryptoKeyPart(), plainCryptoKey.getCipherParameters());
		}
		return plainCryptoKey;
	}

	public KeyParameter getDataKey() {
		final PlainCryptoKey plainCryptoKey = getPlainCryptoKeyOrCreate(CryptoKeyRole.dataKey, CryptoKeyPart.sharedSecret);
		return plainCryptoKey.getKeyParameterOrFail();
	}

	public CryptreeNode getParent() {
		if (parent == null) {
			final RepoFile parentRepoFile = repoFile == null ? null : repoFile.getParent();
			final CryptoRepoFile parentCryptoRepoFile =  cryptoRepoFile == null ? null : cryptoRepoFile.getParent();

			if (parentRepoFile != null || parentCryptoRepoFile != null)
				parent = new CryptreeNode(null, this, getUserRepoKey(), getTransaction(), parentRepoFile, parentCryptoRepoFile);
		}
		return parent;
	}

}
