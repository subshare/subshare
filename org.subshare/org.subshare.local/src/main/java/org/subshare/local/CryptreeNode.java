package org.subshare.local;

import static co.codewizards.cloudstore.core.util.Util.*;
import static org.subshare.local.CryptreeNodeUtil.*;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.core.dto.CryptoKeyPart;
import org.subshare.core.dto.CryptoKeyRole;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.local.persistence.CryptoKey;
import org.subshare.local.persistence.CryptoKeyDAO;
import org.subshare.local.persistence.CryptoLink;
import org.subshare.local.persistence.CryptoLinkDAO;
import org.subshare.local.persistence.CryptoRepoFile;
import org.subshare.local.persistence.CryptoRepoFileDAO;

import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.dto.jaxb.RepoFileDTOIO;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.dto.RepoFileDTOConverter;
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
	private CryptreeNode parent; // maybe null - lazily loaded, if there is one
	private final LocalRepoTransaction transaction; // never null
	private RepoFile repoFile; // maybe null - lazily loaded
	private CryptoRepoFile cryptoRepoFile; // maybe null - lazily loaded
	private final RepoFileDTOConverter repoFileDTOConverter; // never null
	private final List<CryptreeNode> children = new ArrayList<CryptreeNode>(0);
	private boolean childrenLoaded = false;
	private final RepoFileDTOIO repoFileDTOIO; // never null

	public CryptreeNode(final UserRepoKeyRing userRepoKeyRing, final LocalRepoTransaction transaction, final RepoFile repoFile) {
		this(userRepoKeyRing, transaction, repoFile, null);
	}

	private CryptreeNode(final UserRepoKeyRing userRepoKeyRing, final LocalRepoTransaction transaction, final RepoFile repoFile, final CryptoRepoFile cryptoRepoFile) {
		this(null, null, userRepoKeyRing,
				assertNotNull("transaction", transaction), repoFile, cryptoRepoFile);
	}

	private CryptreeNode(final CryptreeNode parent, final CryptreeNode child, final UserRepoKeyRing userRepoKeyRing, final LocalRepoTransaction transaction, final RepoFile repoFile, final CryptoRepoFile cryptoRepoFile) {
		if (parent == null && child == null && userRepoKeyRing == null)
			throw new IllegalArgumentException("parent == null && child == null && userRepoKeyRing == null");

		if (parent == null && child == null && transaction == null)
			throw new IllegalArgumentException("parent == null && child == null && transaction == null");

		if (repoFile == null && cryptoRepoFile == null)
			throw new IllegalArgumentException("repoFile == null && cryptoRepoFile == null");

		this.parent = parent;
		this.userRepoKeyRing = userRepoKeyRing != null ? userRepoKeyRing
				: (parent != null ? parent.getUserRepoKeyRing() : child.getUserRepoKeyRing());
		this.transaction = transaction != null ? transaction
				: (parent != null ? parent.getTransaction() : child.getTransaction());

		this.repoFile = repoFile != null ? repoFile : cryptoRepoFile.getRepoFile();
		this.cryptoRepoFile = cryptoRepoFile;
		this.repoFileDTOConverter = new RepoFileDTOConverter(transaction);

		this.repoFileDTOIO = parent != null ? parent.repoFileDTOIO
				: (child != null ? child.repoFileDTOIO : new RepoFileDTOIO());

		if (child != null)
			children.add(child);
	}

	protected UserRepoKeyRing getUserRepoKeyRing() {
		return userRepoKeyRing;
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

			final CryptoRepoFileDAO cryptoRepoFileDAO = transaction.getDAO(CryptoRepoFileDAO.class);
			cryptoRepoFile = cryptoRepoFileDAO.getCryptoRepoFile(repoFile);
		}
		return cryptoRepoFile;
	}

	public CryptoRepoFile getCryptoRepoFileOrCreate(final boolean forceUpdate) {
		CryptoRepoFile cryptoRepoFile = getCryptoRepoFile();
		if (cryptoRepoFile == null || forceUpdate) {
			final CryptoRepoFileDAO cryptoRepoFileDAO = transaction.getDAO(CryptoRepoFileDAO.class);

			if (cryptoRepoFile == null)
				cryptoRepoFile = new CryptoRepoFile();

			cryptoRepoFile.setRepoFile(repoFile);

			final PlainCryptoKey plainCryptoKey = getPlainCryptoKeyOrCreate(CryptoKeyRole.dataKey, CryptoKeyPart.sharedSecret);
			final CryptoKey cryptoKey = assertNotNull("plainCryptoKey", plainCryptoKey).getCryptoKey();
			cryptoRepoFile.setCryptoKey(assertNotNull("plainCryptoKey.cryptoKey", cryptoKey));

			final CryptreeNode parent = getParent();
			cryptoRepoFile.setParent(parent == null ? null : parent.getCryptoRepoFile());

			// TODO can we assert here, that this code is invoked on the client-side with the plain-text RepoFile?!
			final RepoFileDTO repoFileDTO = repoFileDTOConverter.toRepoFileDTO(repoFile, Integer.MAX_VALUE);
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			repoFileDTOIO.serialize(repoFileDTO, out);

			cryptoRepoFile.setRepoFileDTOData(encrypt(out.toByteArray(), plainCryptoKey));

			cryptoRepoFileDAO.makePersistent(cryptoRepoFile);
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
		final CryptoLinkDAO cryptoLinkDAO = transaction.getDAO(CryptoLinkDAO.class);
		final Collection<CryptoLink> cryptoLinks = cryptoLinkDAO.getActiveCryptoLinks(getRepoFile(), toCryptoKeyRole, toCryptoKeyPart);
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
				final UserRepoKey userRepoKey = userRepoKeyRing.getUserRepoKey(fromUserRepoKeyId);
				if (userRepoKey != null) {
					final byte[] plain = decrypt(cryptoLink.getToCryptoKeyData(), userRepoKey);
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

			final CryptoKeyDAO cryptoKeyDAO = transaction.getDAO(CryptoKeyDAO.class);
			final CryptoKey cryptoKey = cryptoKeyDAO.makePersistent(plainCryptoKey.getCryptoKey());
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
				parent = new CryptreeNode(null, this, getUserRepoKeyRing(), getTransaction(), parentRepoFile, parentCryptoRepoFile);
		}
		return parent;
	}

}
