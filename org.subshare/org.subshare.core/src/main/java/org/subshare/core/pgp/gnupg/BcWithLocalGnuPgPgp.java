package org.subshare.core.pgp.gnupg;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.PropertiesUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyRing;
import org.bouncycastle.openpgp.PGPObjectFactory;
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
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpKeyTrustLevel;
import org.subshare.core.pgp.PgpSignature;
import org.subshare.core.pgp.PgpSignatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;

public class BcWithLocalGnuPgPgp extends AbstractPgp {
	private static final Logger logger = LoggerFactory.getLogger(BcWithLocalGnuPgPgp.class);

	private File pubringFile;
	private File secringFile;

	private long pubringFileLastModified;
	private long secringFileLastModified;

	private Map<PgpKeyId, BcPgpKey> pgpKeyId2bcPgpKey; // all keys
	private Map<PgpKeyId, BcPgpKey> pgpKeyId2masterKey; // only master-keys

	private File gpgPropertiesFile;

	private Properties gpgProperties;

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
	public PgpKey getPgpKey(final PgpKeyId pgpKeyId) {
		assertNotNull("pgpKeyId", pgpKeyId);
		loadIfNeeded();

		if (PgpKey.TEST_DUMMY_PGP_KEY_ID.equals(pgpKeyId))
			return PgpKey.TEST_DUMMY_PGP_KEY;

		final BcPgpKey bcPgpKey = pgpKeyId2bcPgpKey.get(pgpKeyId);
		return bcPgpKey == null ? null : bcPgpKey.getPgpKey();
	}

	@Override
	public synchronized void exportPublicKeysWithPrivateKeys(final Set<PgpKey> pgpKeys, OutputStream out) {
		assertNotNull("pgpKeys", pgpKeys);
		assertNotNull("out", out);

		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public synchronized void exportPublicKeys(final Set<PgpKey> pgpKeys, OutputStream out) {
		assertNotNull("pgpKeys", pgpKeys);
		assertNotNull("out", out);

		if (! (out instanceof BCPGOutputStream))
			out = new BCPGOutputStream(out); // seems not necessary, but maybe better (faster for sure, since it doesn't need to be created again and again).

		try {
			for (final PgpKey pgpKey : pgpKeys) {
				final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
				bcPgpKey.getPublicKeyRing().encode(out);
			}
			out.flush();
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	@Override
	public synchronized void importKeys(InputStream in) {
		assertNotNull("in", in);

		boolean modified = false;
		try {
			in = PGPUtil.getDecoderStream(in);
			final PGPObjectFactory pgpF = new PGPObjectFactory(in);

			Object o;
			while ((o = pgpF.nextObject()) != null) {
				if (o instanceof PGPPublicKeyRing)
					modified |= importPublicKeyRing((PGPPublicKeyRing) o);
				else if (o instanceof PGPSecretKeyRing)
					modified |= importSecretKeyRing((PGPSecretKeyRing) o);
				else
					throw new IllegalStateException("Unexpected object in InputStream (only PGPPublicKeyRing and PGPSecretKeyRing are supported): " + o);
			}
		} catch (IOException | PGPException x) {
			throw new RuntimeException(x);
		}

		if (modified) // make sure the localRevision is incremented, even if the timestamp does not change (e.g. because the time resolution of the file system is too low).
			incLocalRevision();
	}

	private boolean importPublicKeyRing(final PGPPublicKeyRing publicKeyRing) throws IOException, PGPException {
		assertNotNull("publicKeyRing", publicKeyRing);

		final File pubringFile = getPubringFile();
		if (!pubringFile.isFile())
			throw new IllegalStateException("There is no public key-ring! You must first initialise your personal key-ring!");

		PGPPublicKeyRingCollection oldPublicKeyRingCollection;
		try (InputStream in = new BufferedInputStream(pubringFile.createInputStream());) {
			oldPublicKeyRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(in));
		}

		PGPPublicKeyRingCollection newPublicKeyRingCollection = oldPublicKeyRingCollection;
		newPublicKeyRingCollection = mergePublicKeyRing(newPublicKeyRingCollection, publicKeyRing);

		if (oldPublicKeyRingCollection != newPublicKeyRingCollection) {
			final File tmpFile = createFile(pubringFile.getParentFile(), pubringFile.getName() + ".tmp");
			try (OutputStream out = new BufferedOutputStream(tmpFile.createOutputStream());) {
				newPublicKeyRingCollection.encode(out);
			}
			pubringFile.delete();
			tmpFile.renameTo(pubringFile);

			// ensure that it's re-loaded.
			pubringFileLastModified = 0;
			return true;
		}
		return false;
	}

	private PGPPublicKeyRingCollection mergePublicKeyRing(PGPPublicKeyRingCollection publicKeyRingCollection, final PGPPublicKeyRing publicKeyRing) throws PGPException {
		assertNotNull("publicKeyRingCollection", publicKeyRingCollection);
		assertNotNull("publicKeyRing", publicKeyRing);

		PGPPublicKeyRing oldPublicKeyRing = publicKeyRingCollection.getPublicKeyRing(publicKeyRing.getPublicKey().getKeyID());
		if (oldPublicKeyRing == null)
			publicKeyRingCollection = PGPPublicKeyRingCollection.addPublicKeyRing(publicKeyRingCollection, publicKeyRing);
		else {
			PGPPublicKeyRing newPublicKeyRing = oldPublicKeyRing;
			for (final Iterator<?> it = publicKeyRing.getPublicKeys(); it.hasNext(); ) {
				PGPPublicKey publicKey = (PGPPublicKey) it.next();
				newPublicKeyRing = mergePublicKey(newPublicKeyRing, publicKey);
			}

			if (newPublicKeyRing != oldPublicKeyRing) {
				publicKeyRingCollection = PGPPublicKeyRingCollection.removePublicKeyRing(publicKeyRingCollection, oldPublicKeyRing);
				publicKeyRingCollection = PGPPublicKeyRingCollection.addPublicKeyRing(publicKeyRingCollection, newPublicKeyRing);
			}
		}
		return publicKeyRingCollection;
	}

	private PGPPublicKeyRing mergePublicKey(PGPPublicKeyRing publicKeyRing, final PGPPublicKey publicKey) {
		assertNotNull("publicKeyRing", publicKeyRing);
		assertNotNull("publicKey", publicKey);

		PGPPublicKey oldPublicKey = publicKeyRing.getPublicKey(publicKey.getKeyID());
		if (oldPublicKey == null)
			publicKeyRing = PGPPublicKeyRing.insertPublicKey(publicKeyRing, publicKey);
		else {
			PGPPublicKey newPublicKey = oldPublicKey;
			for (final Iterator<?> it = publicKey.getSignatures(); it.hasNext(); ) {
				PGPSignature signature = (PGPSignature) it.next();
				newPublicKey = mergeSignature(newPublicKey, signature);
			}

			if (newPublicKey != oldPublicKey) {
				publicKeyRing = PGPPublicKeyRing.removePublicKey(publicKeyRing, oldPublicKey);
				publicKeyRing = PGPPublicKeyRing.insertPublicKey(publicKeyRing, newPublicKey);
			}
		}
		return publicKeyRing;
	}


	private PGPPublicKey mergeSignature(PGPPublicKey publicKey, final PGPSignature signature) {
		assertNotNull("publicKey", publicKey);
		assertNotNull("signature", signature);

		PGPSignature oldSignature = getSignature(publicKey, signature);
		if (oldSignature == null)
			publicKey = PGPPublicKey.addCertification(publicKey, signature);

		return publicKey;
	}

	private static PGPSignature getSignature(final PGPPublicKey publicKey, final PGPSignature signature) {
		assertNotNull("publicKey", publicKey);
		assertNotNull("signature", signature);

		for (final Iterator<?> it = publicKey.getSignatures(); it.hasNext(); ) {
			final PGPSignature s = (PGPSignature) it.next();
			if (isSignatureEqual(s, signature))
				return s;
		}
		return null;
	}

	private static boolean isSignatureEqual(final PGPSignature one, final PGPSignature two) {
		return equal(one.getKeyID(), two.getKeyID())
				&& equal(one.getCreationTime(), two.getCreationTime())
				&& equal(one.getHashAlgorithm(), two.getHashAlgorithm())
				&& equal(one.getKeyAlgorithm(), two.getKeyAlgorithm())
				&& equal(one.getSignatureType(), two.getSignatureType());
	}

	private boolean importSecretKeyRing(PGPSecretKeyRing secretKeyRing) throws IOException, PGPException {
		// TODO implement this!
		throw new UnsupportedOperationException("Importing secret keys is not yet supported!");
	}

	public BcPgpKey getBcPgpKeyOrFail(final PgpKey pgpKey) {
		final BcPgpKey bcPgpKey = getBcPgpKey(pgpKey);
		if (bcPgpKey == null)
			throw new IllegalArgumentException("Unknown pgpKey with pgpKeyId=" + pgpKey.getPgpKeyId());

		return bcPgpKey;
	}

	public BcPgpKey getBcPgpKey(final PgpKey pgpKey) {
		final PgpKeyId pgpKeyId = assertNotNull("pgpKey", pgpKey).getPgpKeyId();
		return getBcPgpKey(pgpKeyId);
	}

	public BcPgpKey getBcPgpKey(final PgpKeyId pgpKeyId) {
		assertNotNull("pgpKeyId", pgpKeyId);
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
				|| getSecringFile().lastModified() != secringFileLastModified) {
			logger.debug("loadIfNeeded: invoking load().");
			load();
		}
		else
			logger.debug("loadIfNeeded: *not* invoking load().");
	}

	protected synchronized void load() {
		final Map<PgpKeyId, BcPgpKey> pgpKeyId2bcPgpKey = new HashMap<PgpKeyId, BcPgpKey>();
		final Map<PgpKeyId, BcPgpKey> pgpKeyId2masterKey = new HashMap<PgpKeyId, BcPgpKey>();

		final long pubringFileLastModified;
		final long secringFileLastModified;
		try {
			final File secringFile = getSecringFile();
			logger.debug("load: secringFile='{}'", secringFile);
			secringFileLastModified = secringFile.lastModified();
			if (secringFile.isFile()) {
				final PGPSecretKeyRingCollection pgpSecretKeyRingCollection;
				try (InputStream in = new BufferedInputStream(secringFile.createInputStream());) {
					pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(in));
				}
				BcPgpKey lastMasterKey = null;
				for (final Iterator<?> it1 = pgpSecretKeyRingCollection.getKeyRings(); it1.hasNext(); ) {
					final PGPSecretKeyRing keyRing = (PGPSecretKeyRing) it1.next();
					for (final Iterator<?> it2 = keyRing.getPublicKeys(); it2.hasNext(); ) {
						final PGPPublicKey publicKey = (PGPPublicKey) it2.next();
						lastMasterKey = enlistPublicKey(pgpKeyId2bcPgpKey,
								pgpKeyId2masterKey, lastMasterKey, keyRing, publicKey);
					}

					for (final Iterator<?> it3 = keyRing.getSecretKeys(); it3.hasNext(); ) {
						final PGPSecretKey secretKey = (PGPSecretKey) it3.next();
						final PgpKeyId pgpKeyId = new PgpKeyId(secretKey.getKeyID());
						final BcPgpKey bcPgpKey = pgpKeyId2bcPgpKey.get(pgpKeyId);
						if (bcPgpKey == null)
							throw new IllegalStateException("Secret key does not have corresponding public key in secret key ring! pgpKeyId=" + pgpKeyId);

						bcPgpKey.setSecretKey(secretKey);
						logger.debug("load: read secretKey with pgpKeyId={}", pgpKeyId);
					}
				}
			}

			final File pubringFile = getPubringFile();
			logger.debug("load: pubringFile='{}'", pubringFile);
			pubringFileLastModified = pubringFile.lastModified();
			if (pubringFile.isFile()) {
				final PGPPublicKeyRingCollection pgpPublicKeyRingCollection;
				try (InputStream in = new BufferedInputStream(pubringFile.createInputStream());) {
					pgpPublicKeyRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(in));
				}

				BcPgpKey lastMasterKey = null;
				for (final Iterator<?> it1 = pgpPublicKeyRingCollection.getKeyRings(); it1.hasNext(); ) {
					final PGPPublicKeyRing keyRing = (PGPPublicKeyRing) it1.next();
					for (final Iterator<?> it2 = keyRing.getPublicKeys(); it2.hasNext(); ) {
						final PGPPublicKey publicKey = (PGPPublicKey) it2.next();
						lastMasterKey = enlistPublicKey(pgpKeyId2bcPgpKey,
								pgpKeyId2masterKey, lastMasterKey, keyRing, publicKey);
					}
				}
			}
		} catch (IOException | PGPException x) {
			throw new RuntimeException(x);
		}

		for (final BcPgpKey bcPgpKey : pgpKeyId2bcPgpKey.values()) {
			if (bcPgpKey.getPublicKey() == null)
				throw new IllegalStateException("bcPgpKey.publicKey == null :: keyId = " + bcPgpKey.getPgpKeyId());

			if (bcPgpKey.getPublicKeyRing() == null)
				throw new IllegalStateException("bcPgpKey.publicKeyRing == null :: keyId = " + bcPgpKey.getPgpKeyId());
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
			pgpSignature.setPgpKeyId(new PgpKeyId(bcPgpSignature.getKeyID()));
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

	private BcPgpKey enlistPublicKey(final Map<PgpKeyId, BcPgpKey> pgpKeyId2bcPgpKey,
			final Map<PgpKeyId, BcPgpKey> pgpKeyId2masterKey,
			BcPgpKey lastMasterKey, final PGPKeyRing keyRing, final PGPPublicKey publicKey)
	{
		final PgpKeyId pgpKeyId = new PgpKeyId(publicKey.getKeyID());
		BcPgpKey bcPgpKey = pgpKeyId2bcPgpKey.get(pgpKeyId);
		if (bcPgpKey == null) {
			bcPgpKey = new BcPgpKey(pgpKeyId);
			pgpKeyId2bcPgpKey.put(pgpKeyId, bcPgpKey);
		}

		if (keyRing instanceof PGPSecretKeyRing)
			bcPgpKey.setSecretKeyRing((PGPSecretKeyRing)keyRing);
		else if (keyRing instanceof PGPPublicKeyRing)
			bcPgpKey.setPublicKeyRing((PGPPublicKeyRing)keyRing);
		else
			throw new IllegalArgumentException("keyRing is neither an instance of PGPSecretKeyRing nor PGPPublicKeyRing!");

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

	private File getGpgPropertiesFile() {
		if (gpgPropertiesFile == null)
			gpgPropertiesFile = createFile(ConfigDir.getInstance().getFile(), "gpg.properties");

		return gpgPropertiesFile;
	}

	@Override
	public long getLocalRevision() {
		final Properties gpgProperties = getGpgProperties();

		loadIfNeeded();
		long pubringFileLastModified = this.pubringFileLastModified;
		long secringFileLastModified = this.secringFileLastModified;

		boolean needIncLocalRevision = false;
		long localRevision;
		synchronized (gpgProperties) {
			localRevision = getPropertyValueAsLong(gpgProperties, PGP_PROPERTY_KEY_LOCAL_REVISION, -1L);
			if (localRevision < 0)
				needIncLocalRevision = true;
			else {
				long oldPubringFileLastModified = getPropertyValueAsLong(gpgProperties, PGP_PROPERTY_KEY_PUBRING_FILE_LAST_MODIFIED, 0L);
				long oldSecringFileLastModified = getPropertyValueAsLong(gpgProperties, PGP_PROPERTY_KEY_SECRING_FILE_LAST_MODIFIED, 0L);

				if (oldPubringFileLastModified != pubringFileLastModified || oldSecringFileLastModified != secringFileLastModified)
					needIncLocalRevision = true;
			}
		}
		if (needIncLocalRevision)
			return incLocalRevision();
		else
			return localRevision;
	}

	private long incLocalRevision() {
		final Properties gpgProperties = getGpgProperties();

		loadIfNeeded();
		long pubringFileLastModified = this.pubringFileLastModified;
		long secringFileLastModified = this.secringFileLastModified;

		final long localRevision;
		synchronized (gpgProperties) {
			localRevision = getPropertyValueAsLong(gpgProperties, PGP_PROPERTY_KEY_LOCAL_REVISION, -1L) + 1;
			gpgProperties.setProperty(PGP_PROPERTY_KEY_LOCAL_REVISION, Long.toString(localRevision));
			gpgProperties.setProperty(PGP_PROPERTY_KEY_PUBRING_FILE_LAST_MODIFIED, Long.toString(pubringFileLastModified));
			gpgProperties.setProperty(PGP_PROPERTY_KEY_SECRING_FILE_LAST_MODIFIED, Long.toString(secringFileLastModified));
			writeGpgProperties();
		}
		return localRevision;
	}

	private static final String PGP_PROPERTY_KEY_PUBRING_FILE_LAST_MODIFIED = "pubringFileLastModified";
	private static final String PGP_PROPERTY_KEY_SECRING_FILE_LAST_MODIFIED = "secringFileLastModified";
	private static final String PGP_PROPERTY_KEY_LOCAL_REVISION = "localRevision";

	private Properties getGpgProperties() {
		if (gpgProperties == null) {
			try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getGpgPropertiesFile(), 30000);) {
				final Lock lock = lockFile.getLock();
				lock.lock();
				try {
					if (gpgProperties == null) {
						final Properties p = new Properties();
						try (final InputStream in = lockFile.createInputStream();) {
							p.load(in);
						}
						gpgProperties = p;
					}
				} finally {
					lock.unlock();
				}
			} catch (final IOException x) {
				throw new RuntimeException(x);
			}
		}
		return gpgProperties;
	}

	private void writeGpgProperties() {
		final Properties gpgProperties = getGpgProperties();
		synchronized (gpgProperties) {
			try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getGpgPropertiesFile(), 30000);) {
				try (final OutputStream out = lockFile.createOutputStream();) { // acquires LockFile.lock implicitly
					gpgProperties.store(out, null);
				}
			} catch (final IOException x) {
				throw new RuntimeException(x);
			}
		}
	}
}
