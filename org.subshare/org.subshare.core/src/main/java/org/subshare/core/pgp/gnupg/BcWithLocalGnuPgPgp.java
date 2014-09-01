package org.subshare.core.pgp.gnupg;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;
import org.subshare.core.pgp.AbstractPgp;
import org.subshare.core.pgp.PgpDecoder;
import org.subshare.core.pgp.PgpEncoder;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyTrustLevel;
import org.subshare.core.pgp.PgpSignature;
import org.subshare.core.pgp.PgpSignatureType;

import co.codewizards.cloudstore.core.oio.File;

public class BcWithLocalGnuPgPgp extends AbstractPgp {

	private File pubringFile;
	private File secringFile;

	private long pubringFileLastModified;
	private long secringFileLastModified;

	private Map<Long, BcPgpKey> pgpKeyId2bcPgpKey; // all keys
	private Map<Long, BcPgpKey> pgpKeyId2masterKey; // only master-keys

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public boolean isSupported() {
		// always supported, because using BC which is in the dependencies.
		return true;
	}

	public File getGnuPgDir() {
		return GnuPgDir.getInstance().getFile();
	}

	@Override
	public Collection<PgpKey> getMasterKeys() {
		loadIfNeeded();
		final List<PgpKey> pgpKeys = new ArrayList<PgpKey>(pgpKeyId2masterKey.size());
		for (final BcPgpKey bcPgpKey : pgpKeyId2masterKey.values())
			pgpKeys.add(bcPgpKey.getPgpKey());

		return Collections.unmodifiableList(pgpKeys);
	}

	@Override
	public PgpKey getPgpKey(final long pgpKeyId) {
		loadIfNeeded();

		if (PgpKey.TEST_DUMMY_PGP_KEY_ID == pgpKeyId)
			return PgpKey.TEST_DUMMY_PGP_KEY;

		final BcPgpKey bcPgpKey = pgpKeyId2bcPgpKey.get(pgpKeyId);
		return bcPgpKey == null ? null : bcPgpKey.getPgpKey();
	}

	public BcPgpKey getBcPgpKeyOrFail(final PgpKey pgpKey) {
		final BcPgpKey bcPgpKey = getBcPgpKey(pgpKey);
		if (bcPgpKey == null)
			throw new IllegalArgumentException("Unknown pgpKey with pgpKeyId=" + pgpKey.getPgpKeyId());

		return bcPgpKey;
	}

	public BcPgpKey getBcPgpKey(final PgpKey pgpKey) {
		final long pgpKeyId = assertNotNull("pgpKey", pgpKey).getPgpKeyId();
		return getBcPgpKey(pgpKeyId);
	}

	public BcPgpKey getBcPgpKey(final long pgpKeyId) {
		loadIfNeeded();
		final BcPgpKey bcPgpKey = pgpKeyId2bcPgpKey.get(pgpKeyId);
		return bcPgpKey;
	}

	@Override
	protected PgpDecoder _createDecoder() {
		return new BcPgpDecoder(this);
	}

	@Override
	protected PgpEncoder _createEncoder() {
		return new BcPgpEncoder(this);
	}

	protected File getPubringFile() {
		if (pubringFile == null)
			pubringFile = createFile(GnuPgDir.getInstance().getFile(), "pubring.gpg");

		return pubringFile;
	}

	protected File getSecringFile() {
		if (secringFile == null)
			secringFile = createFile(GnuPgDir.getInstance().getFile(), "secring.gpg");

		return secringFile;
	}

	protected synchronized void loadIfNeeded() {
		if (pgpKeyId2bcPgpKey == null
				|| getPubringFile().lastModified() != pubringFileLastModified
				|| getSecringFile().lastModified() != secringFileLastModified)
			load();
	}

	protected synchronized void load() {
		final Map<Long, BcPgpKey> pgpKeyId2bcPgpKey = new HashMap<Long, BcPgpKey>();
		final Map<Long, BcPgpKey> pgpKeyId2masterKey = new HashMap<Long, BcPgpKey>();

		final long pubringFileLastModified;
		final long secringFileLastModified;
		try {
			final File secringFile = getSecringFile();
			secringFileLastModified = secringFile.lastModified();
			if (secringFile.isFile()) {
				PGPSecretKeyRingCollection pgpSecretKeyRingCollection;
				try (InputStream in = new BufferedInputStream(secringFile.createInputStream());) {
					pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(in));
				}
				BcPgpKey lastMasterKey = null;
				for (final Iterator<?> it1 = pgpSecretKeyRingCollection.getKeyRings(); it1.hasNext(); ) {
					final PGPSecretKeyRing keyRing = (PGPSecretKeyRing) it1.next();
					for (final Iterator<?> it2 = keyRing.getPublicKeys(); it2.hasNext(); ) {
						final PGPPublicKey publicKey = (PGPPublicKey) it2.next();
						lastMasterKey = enlistPublicKey(pgpKeyId2bcPgpKey,
								pgpKeyId2masterKey, lastMasterKey, publicKey);
					}

					for (final Iterator<?> it3 = keyRing.getSecretKeys(); it3.hasNext(); ) {
						final PGPSecretKey secretKey = (PGPSecretKey) it3.next();
						final long pgpKeyId = secretKey.getKeyID();
						final BcPgpKey bcPgpKey = pgpKeyId2bcPgpKey.get(pgpKeyId);
						if (bcPgpKey == null)
							throw new IllegalStateException("Secret key does not have corresponding public key in secret key ring! pgpKeyId=" + Long.toHexString(pgpKeyId));

						bcPgpKey.setSecretKey(secretKey);
					}
				}
			}

			final File pubringFile = getPubringFile();
			pubringFileLastModified = pubringFile.lastModified();
			if (pubringFile.isFile()) {
				PGPPublicKeyRingCollection pgpPublicKeyRingCollection;
				try (InputStream in = new BufferedInputStream(pubringFile.createInputStream());) {
					pgpPublicKeyRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(in));
				}

				BcPgpKey lastMasterKey = null;
				for (final Iterator<?> it1 = pgpPublicKeyRingCollection.getKeyRings(); it1.hasNext(); ) {
					final PGPPublicKeyRing keyRing = (PGPPublicKeyRing) it1.next();
					for (final Iterator<?> it2 = keyRing.getPublicKeys(); it2.hasNext(); ) {
						final PGPPublicKey publicKey = (PGPPublicKey) it2.next();
						lastMasterKey = enlistPublicKey(pgpKeyId2bcPgpKey,
								pgpKeyId2masterKey, lastMasterKey, publicKey);
					}
				}
			}
		} catch (IOException | PGPException x) {
			throw new RuntimeException(x);
		}

		this.secringFileLastModified = secringFileLastModified;
		this.pubringFileLastModified = pubringFileLastModified;
		this.pgpKeyId2bcPgpKey = pgpKeyId2bcPgpKey;
		this.pgpKeyId2masterKey = pgpKeyId2masterKey;
	}

	@Override
	public Collection<PgpSignature> getSignatures(final PgpKey pgpKey) {
		final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
		final List<PgpSignature> result = new ArrayList<PgpSignature>();
		for (final Iterator<?> it = bcPgpKey.getPublicKey().getSignatures(); it.hasNext(); ) {
			final PGPSignature bcPgpSignature = (PGPSignature) it.next();
			final PgpSignature pgpSignature = new PgpSignature();
			pgpSignature.setPgpKeyId(bcPgpSignature.getKeyID());
			pgpSignature.setCreated(bcPgpSignature.getCreationTime());
			pgpSignature.setSignatureType(signatureTypeToEnum(bcPgpSignature.getSignatureType()));
			result.add(pgpSignature);
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public Collection<PgpKey> getMasterKeysWithPrivateKey() {
		final List<PgpKey> result = new ArrayList<PgpKey>();
		final Collection<PgpKey> masterKeys = getMasterKeys();
		for (final PgpKey pgpKey : masterKeys) {
			if (pgpKey.isPrivateKeyAvailable())
				result.add(pgpKey);
		}
		return Collections.unmodifiableList(result);
	}

	@Override
	public boolean isTrusted(final PgpKey pgpKey) {
		return getKeyTrustLevel(pgpKey).compareTo(PgpKeyTrustLevel.TRUSTED_EXPIRED) >= 0;
	}

	@Override
	public PgpKeyTrustLevel getKeyTrustLevel(final PgpKey pgpKey) {
		// PGPPublicKey.getTrustData() returns *always* null, hence we must calculate it ourselves :-(
		// TODO we must manage the "owner-trust", too!
		// TODO we should improve this algorithm and store the results in a database, too!
		// TODO is it expired?!
		// For now, we take only direct trust relations into account.
		final Set<PgpKey> masterKeysWithPrivateKey = new HashSet<PgpKey>(getMasterKeysWithPrivateKey());
		if (masterKeysWithPrivateKey.contains(pgpKey))
			return PgpKeyTrustLevel.ULTIMATE;

		for (final PgpSignature signature : getSignatures(pgpKey)) {
			if (signature.getSignatureType().getTrustLevel() < PgpSignatureType.CASUAL_CERTIFICATION.getTrustLevel())
				continue;

			final PgpKey signingKey = getPgpKey(signature.getPgpKeyId());
			if (masterKeysWithPrivateKey.contains(signingKey))
				return PgpKeyTrustLevel.TRUSTED;
		}
		return PgpKeyTrustLevel.NOT_TRUSTED;
	}

	private BcPgpKey enlistPublicKey(final Map<Long, BcPgpKey> pgpKeyId2bcPgpKey,
			final Map<Long, BcPgpKey> pgpKeyId2masterKey,
			BcPgpKey lastMasterKey, final PGPPublicKey publicKey)
	{
		final long pgpKeyId = publicKey.getKeyID();
		BcPgpKey bcPgpKey = pgpKeyId2bcPgpKey.get(pgpKeyId);
		if (bcPgpKey == null) {
			bcPgpKey = new BcPgpKey(pgpKeyId);
			pgpKeyId2bcPgpKey.put(pgpKeyId, bcPgpKey);
		}
		bcPgpKey.setPublicKey(publicKey);

		if (publicKey.isMasterKey()) {
			lastMasterKey = bcPgpKey;
			pgpKeyId2masterKey.put(bcPgpKey.getPgpKeyId(), bcPgpKey);
		}
		else {
			if (lastMasterKey == null)
				throw new IllegalStateException("First key is a non-master key!");

			lastMasterKey.getSubKeys().add(bcPgpKey);
		}
		return lastMasterKey;
	}

	private PgpSignatureType signatureTypeToEnum(final int signatureType) {
		switch (signatureType) {
			case PGPSignature.BINARY_DOCUMENT:
				return PgpSignatureType.BINARY_DOCUMENT;
			case PGPSignature.CANONICAL_TEXT_DOCUMENT:
				return PgpSignatureType.CANONICAL_TEXT_DOCUMENT;
			case PGPSignature.STAND_ALONE:
				return PgpSignatureType.STAND_ALONE;

			case PGPSignature.DEFAULT_CERTIFICATION:
				return PgpSignatureType.DEFAULT_CERTIFICATION;
			case PGPSignature.NO_CERTIFICATION:
				return PgpSignatureType.NO_CERTIFICATION;
			case PGPSignature.CASUAL_CERTIFICATION:
				return PgpSignatureType.CASUAL_CERTIFICATION;
			case PGPSignature.POSITIVE_CERTIFICATION:
				return PgpSignatureType.POSITIVE_CERTIFICATION;

			case PGPSignature.SUBKEY_BINDING:
				return PgpSignatureType.SUBKEY_BINDING;
			case PGPSignature.PRIMARYKEY_BINDING:
				return PgpSignatureType.PRIMARYKEY_BINDING;
			case PGPSignature.DIRECT_KEY:
				return PgpSignatureType.DIRECT_KEY;
			case PGPSignature.KEY_REVOCATION:
				return PgpSignatureType.KEY_REVOCATION;
			case PGPSignature.SUBKEY_REVOCATION:
				return PgpSignatureType.SUBKEY_REVOCATION;
			case PGPSignature.CERTIFICATION_REVOCATION:
				return PgpSignatureType.CERTIFICATION_REVOCATION;
			case PGPSignature.TIMESTAMP:
				return PgpSignatureType.TIMESTAMP;

			default:
				throw new IllegalArgumentException("Unknown signatureType: " + signatureType);
		}
	}
}
