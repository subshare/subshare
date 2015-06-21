package org.subshare.core.pgp.gnupg;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static co.codewizards.cloudstore.core.util.PropertiesUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static co.codewizards.cloudstore.core.util.Util.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.generators.DSAParametersGenerator;
import org.bouncycastle.crypto.generators.ElGamalParametersGenerator;
import org.bouncycastle.crypto.params.DSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.DSAParameters;
import org.bouncycastle.crypto.params.ElGamalKeyGenerationParameters;
import org.bouncycastle.crypto.params.ElGamalParameters;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyRing;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.subshare.core.pgp.AbstractPgp;
import org.subshare.core.pgp.CreatePgpKeyParam;
import org.subshare.core.pgp.PgpDecoder;
import org.subshare.core.pgp.PgpEncoder;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpKeyTrustLevel;
import org.subshare.core.pgp.PgpSignature;
import org.subshare.core.pgp.PgpSignatureType;
import org.subshare.crypto.CryptoRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;

public class BcWithLocalGnuPgPgp extends AbstractPgp {
	private static final Logger logger = LoggerFactory.getLogger(BcWithLocalGnuPgPgp.class);

	private File configDir;
	private File gnuPgDir;
	private File pubringFile;
	private File secringFile;

	private long pubringFileLastModified;
	private long secringFileLastModified;

	private Map<PgpKeyId, BcPgpKey> pgpKeyId2bcPgpKey; // all keys
	private Map<PgpKeyId, BcPgpKey> pgpKeyId2masterKey; // only master-keys

	private Properties gpgProperties;
	private final Map<String, Object> pgpKeyIdRange2Mutex = new HashMap<>();
	private final Map<String, Properties> pgpKeyIdRange2LocalRevisionProperties = Collections.synchronizedMap(new HashMap<String, Properties>());

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
		if (gnuPgDir == null)
			gnuPgDir = GnuPgDir.getInstance().getFile();

		return gnuPgDir;
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

		PGPPublicKeyRingCollection oldPublicKeyRingCollection;

		final File pubringFile = getPubringFile();
		if (!pubringFile.isFile())
			oldPublicKeyRingCollection = new PGPPublicKeyRingCollection(new ByteArrayInputStream(new byte[0]));
		else {
			try (InputStream in = new BufferedInputStream(pubringFile.createInputStream());) {
				oldPublicKeyRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(in));
			}
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

	private boolean importSecretKeyRing(final PGPSecretKeyRing secretKeyRing) throws IOException, PGPException {
		assertNotNull("secretKeyRing", secretKeyRing);

		PGPSecretKeyRingCollection oldSecretKeyRingCollection;

		final File secringFile = getSecringFile();
		if (!secringFile.isFile())
			oldSecretKeyRingCollection = new PGPSecretKeyRingCollection(new ByteArrayInputStream(new byte[0]));
		else {
			try (InputStream in = new BufferedInputStream(secringFile.createInputStream());) {
				oldSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(in));
			}
		}

		PGPSecretKeyRingCollection newSecretKeyRingCollection = oldSecretKeyRingCollection;
		newSecretKeyRingCollection = mergeSecretKeyRing(newSecretKeyRingCollection, secretKeyRing);

		if (oldSecretKeyRingCollection != newSecretKeyRingCollection) {
			final File tmpFile = createFile(secringFile.getParentFile(), secringFile.getName() + ".tmp");
			try (OutputStream out = new BufferedOutputStream(tmpFile.createOutputStream());) {
				newSecretKeyRingCollection.encode(out);
			}
			secringFile.delete();
			tmpFile.renameTo(secringFile);

			// ensure that it's re-loaded.
			secringFileLastModified = 0;
			return true;
		}
		return false;
	}

	private PGPSecretKeyRingCollection mergeSecretKeyRing(PGPSecretKeyRingCollection secretKeyRingCollection, final PGPSecretKeyRing secretKeyRing) throws PGPException {
		assertNotNull("secretKeyRingCollection", secretKeyRingCollection);
		assertNotNull("secretKeyRing", secretKeyRing);

		PGPSecretKeyRing oldSecretKeyRing = secretKeyRingCollection.getSecretKeyRing(secretKeyRing.getSecretKey().getKeyID());
		if (oldSecretKeyRing == null)
			secretKeyRingCollection = PGPSecretKeyRingCollection.addSecretKeyRing(secretKeyRingCollection, secretKeyRing);
		else {
			PGPSecretKeyRing newSecretKeyRing = oldSecretKeyRing;
			for (final Iterator<?> it = secretKeyRing.getSecretKeys(); it.hasNext(); ) {
				PGPSecretKey secretKey = (PGPSecretKey) it.next();
				newSecretKeyRing = mergeSecretKey(newSecretKeyRing, secretKey);
			}

			if (newSecretKeyRing != oldSecretKeyRing) {
				secretKeyRingCollection = PGPSecretKeyRingCollection.removeSecretKeyRing(secretKeyRingCollection, oldSecretKeyRing);
				secretKeyRingCollection = PGPSecretKeyRingCollection.addSecretKeyRing(secretKeyRingCollection, newSecretKeyRing);
			}
		}
		return secretKeyRingCollection;
	}

	private PGPSecretKeyRing mergeSecretKey(PGPSecretKeyRing secretKeyRing, final PGPSecretKey secretKey) {
		assertNotNull("secretKeyRing", secretKeyRing);
		assertNotNull("secretKey", secretKey);

		PGPSecretKey oldSecretKey = secretKeyRing.getSecretKey(secretKey.getKeyID());
		if (oldSecretKey == null)
			secretKeyRing = PGPSecretKeyRing.insertSecretKey(secretKeyRing, secretKey);
		// else: there is nothing to merge - a secret key is immutable. btw. it contains a public key - but without signatures.

		return secretKeyRing;
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
		if (pubringFile == null) {
			final File gnuPgDir = getGnuPgDir();
			gnuPgDir.mkdirs();
			pubringFile = createFile(gnuPgDir, "pubring.gpg");
		}
		return pubringFile;
	}

	protected File getSecringFile() {
		if (secringFile == null) {
			final File gnuPgDir = getGnuPgDir();
			gnuPgDir.mkdirs();
			secringFile = createFile(gnuPgDir, "secring.gpg");
		}
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
	public Collection<PgpSignature> getCertifications(final PgpKey pgpKey) {
		final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
		final PGPPublicKey publicKey = bcPgpKey.getPublicKey();
		final List<PgpSignature> result = new ArrayList<PgpSignature>();

		final IdentityHashMap<PGPSignature, PGPSignature> bcPgpSignatures = new IdentityHashMap<>();

		final List<String> userIds = new LinkedList<String>();
		for (Iterator<?> itUserId = publicKey.getUserIDs(); itUserId.hasNext(); ) {
			final String userId = (String) itUserId.next();
			userIds.add(userId);

			final Iterator<?> itSig = publicKey.getSignaturesForID(userId);
			if (itSig == null)
				continue;

			while (itSig.hasNext()) {
				final PGPSignature bcPgpSignature = (PGPSignature) itSig.next();
				bcPgpSignatures.put(bcPgpSignature, bcPgpSignature);
				final PgpSignature pgpSignature = createPgpSignature(bcPgpSignature);

				// all of them should be certifications, but we still check to make 100% sure
				if (! pgpSignature.getSignatureType().isCertification())
					continue;

				pgpSignature.setUserId(userId);
				result.add(pgpSignature);
			}
		}

		// It seems, there are both: certifications for individual user-ids and certifications for the
		// entire key. I therefore first take the individual ones (above) and then I emulate the ones for the entire key
		// as if they were individual ones (below).

		final Iterator<?> itAllSigs = publicKey.getSignatures();
		if (itAllSigs != null) {
			while (itAllSigs.hasNext()) {
				final PGPSignature bcPgpSignature = (PGPSignature) itAllSigs.next();
				if (bcPgpSignatures.containsKey(bcPgpSignature))
					continue;

				final PgpSignatureType signatureType = signatureTypeToEnum(bcPgpSignature.getSignatureType());
				if (! signatureType.isCertification())
					continue;

				for (String userId : userIds) {
					final PgpSignature pgpSignature = createPgpSignature(bcPgpSignature);
					pgpSignature.setUserId(userId);
					result.add(pgpSignature);
				}
			}
		}

//		for (final Iterator<?> it = publicKey.getSignatures(); it.hasNext(); ) {
//			final PGPSignature bcPgpSignature = (PGPSignature) it.next();
//			result.add(createPgpSignature(bcPgpSignature));
//		}
		return Collections.unmodifiableList(result);
	}

	public PgpSignature createPgpSignature(final PGPSignature bcPgpSignature) {
		final PgpSignature pgpSignature = new PgpSignature();
		pgpSignature.setPgpKeyId(new PgpKeyId(bcPgpSignature.getKeyID()));
		pgpSignature.setCreated(bcPgpSignature.getCreationTime());
		pgpSignature.setSignatureType(signatureTypeToEnum(bcPgpSignature.getSignatureType()));
		return pgpSignature;
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

		for (final PgpSignature signature : getCertifications(pgpKey)) {
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
			bcPgpKey = new BcPgpKey(this, pgpKeyId);
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

			bcPgpKey.setMasterKey(lastMasterKey);

			// It may already be in the lastMasterKey.subKeys, because we enlist from both the
			// secret *and* public key ring collection. Therefore, we now use a LinkedHashSet (instead of an ArrayList).
			// And to make sure the one we have in the subkeys is the same instance, we first remove and then re-add (as
			// a set does not add, if it is contained, while a Map [pgpKeyId2bcPgpKey] does overwrite).
			lastMasterKey.getSubKeyIds().add(bcPgpKey.getPgpKeyId());
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

	private int signatureTypeFromEnum(final PgpSignatureType signatureType) {
		switch (assertNotNull("signatureType", signatureType)) {
			case BINARY_DOCUMENT:
				return PGPSignature.BINARY_DOCUMENT;
			case CANONICAL_TEXT_DOCUMENT:
				return PGPSignature.CANONICAL_TEXT_DOCUMENT;
			case STAND_ALONE:
				return PGPSignature.STAND_ALONE;

			case DEFAULT_CERTIFICATION:
				return PGPSignature.DEFAULT_CERTIFICATION;
			case NO_CERTIFICATION:
				return PGPSignature.NO_CERTIFICATION;
			case CASUAL_CERTIFICATION:
				return PGPSignature.CASUAL_CERTIFICATION;
			case POSITIVE_CERTIFICATION:
				return PGPSignature.POSITIVE_CERTIFICATION;

			case SUBKEY_BINDING:
				return PGPSignature.SUBKEY_BINDING;
			case PRIMARYKEY_BINDING:
				return PGPSignature.PRIMARYKEY_BINDING;
			case DIRECT_KEY:
				return PGPSignature.DIRECT_KEY;
			case KEY_REVOCATION:
				return PGPSignature.KEY_REVOCATION;
			case SUBKEY_REVOCATION:
				return PGPSignature.SUBKEY_REVOCATION;
			case CERTIFICATION_REVOCATION:
				return PGPSignature.CERTIFICATION_REVOCATION;
			case TIMESTAMP:
				return PGPSignature.TIMESTAMP;

			default:
				throw new IllegalArgumentException("Unknown signatureType: " + signatureType);
		}
	}

	private File getConfigDir() {
		if (configDir == null)
			configDir = ConfigDir.getInstance().getFile();

		return configDir;
	}

	private File getGpgPropertiesFile() {
		return createFile(getConfigDir(), "gpg.properties");
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
		firePropertyChange(PropertyEnum.localRevision, localRevision - 1, localRevision);
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

	private Properties getLocalRevisionProperties(final PgpKeyId pgpKeyId) {
		final String pgpKeyIdRange = getPgpKeyIdRange(pgpKeyId);
		synchronized (getPgpKeyIdRangeMutex(pgpKeyIdRange)) {
			Properties properties = pgpKeyIdRange2LocalRevisionProperties.get(pgpKeyIdRange);
			if (properties == null) {
				properties = new Properties();

				try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getLocalRevisionPropertiesFile(pgpKeyIdRange), 30000);) {
					try (final InputStream in = lockFile.createInputStream();) {
						properties.load(in);
					}
				} catch (final IOException x) {
					throw new RuntimeException(x);
				}

				pgpKeyIdRange2LocalRevisionProperties.put(pgpKeyIdRange, properties);
			}
			return properties;
		}
	}

	private void writeLocalRevisionProperties(final PgpKeyId pgpKeyId) {
		final String pgpKeyIdRange = getPgpKeyIdRange(pgpKeyId);
		synchronized (getPgpKeyIdRangeMutex(pgpKeyIdRange)) {
			Properties properties = pgpKeyIdRange2LocalRevisionProperties.get(pgpKeyIdRange);
			if (properties != null) {
				try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getLocalRevisionPropertiesFile(pgpKeyIdRange), 30000);) {
					try (final OutputStream out = lockFile.createOutputStream();) {
						properties.store(out, null);
					}
				} catch (final IOException x) {
					throw new RuntimeException(x);
				}
			}
		}
	}

	private File getLocalRevisionPropertiesFile(final String pgpKeyIdRange) {
		assertNotNull("pgpKeyIdRange", pgpKeyIdRange);
		final File dir = createFile(getConfigDir(), "gpgLocalRevision");
		final File file = createFile(dir, pgpKeyIdRange + ".properties");
		file.getParentFile().mkdirs();
		return file;
	}

	private Object getPgpKeyIdRangeMutex(final String pgpKeyIdRange) {
		assertNotNull("pgpKeyIdRange", pgpKeyIdRange);
		synchronized (pgpKeyIdRange2Mutex) {
			Object mutex = pgpKeyIdRange2Mutex.get(pgpKeyIdRange);
			if (mutex == null) {
				mutex = pgpKeyIdRange;
				pgpKeyIdRange2Mutex.put(pgpKeyIdRange, mutex);
			}
			return mutex;
		}
	}

	private String getPgpKeyIdRange(final PgpKeyId pgpKeyId) {
		assertNotNull("pgpKeyId", pgpKeyId);
		final int range1 = ((int) pgpKeyId.longValue()) & 0xff;
		final int range2 = ((int) (pgpKeyId.longValue() >>> 8)) & 0xff;
		return encodeHexStr(new byte[] { (byte)range2 }) + '/' + encodeHexStr(new byte[] { (byte)range1 });
//		return Integer.toHexString(range2) + '/' + Integer.toHexString(range1);
	}

	@Override
	public long getLocalRevision(final PgpKey pgpKey) {
		final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
		final PgpKeyId pgpKeyId = pgpKey.getPgpKeyId();
		final String pgpKeyIdRange = getPgpKeyIdRange(pgpKeyId);
		final long globalLocalRevision = getLocalRevision();

		synchronized (getPgpKeyIdRangeMutex(pgpKeyIdRange)) {
			final Properties localRevisionProperties = getLocalRevisionProperties(pgpKeyId);

			final String propertyKeyPrefix = pgpKeyId.toString() + '.';
			final String globalLocalRevisionPropertyKey = propertyKeyPrefix + "globalLocalRevision";
			final String localRevisionPropertyKey = propertyKeyPrefix + "localRevision";

			final long oldGlobalLocalRevision = getPropertyValueAsLong(localRevisionProperties, globalLocalRevisionPropertyKey, -1);
			long localRevision = getPropertyValueAsLong(localRevisionProperties, localRevisionPropertyKey, -1);

			if (globalLocalRevision != oldGlobalLocalRevision || localRevision < 0) {
				final String publicKeySha1PropertyKey = propertyKeyPrefix + "publicKeySha1";
				final String secretKeySha1PropertyKey = propertyKeyPrefix + "secretKeySha1";

				final String oldPublicKeySha1 = localRevisionProperties.getProperty(publicKeySha1PropertyKey);
				final String oldSecretKeySha1 = localRevisionProperties.getProperty(secretKeySha1PropertyKey);

				final String publicKeySha1;
				final String secretKeySha1;
				try {
					publicKeySha1 = sha1(bcPgpKey.getPublicKey().getEncoded());
					secretKeySha1 = bcPgpKey.getSecretKey() == null ? null : sha1(bcPgpKey.getSecretKey().getEncoded());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				// if no change, we only need to set the new globalLocalRevision (we always need to update this).
				localRevisionProperties.setProperty(globalLocalRevisionPropertyKey, Long.toString(globalLocalRevision));
				if (!equal(oldPublicKeySha1, publicKeySha1) || !equal(oldSecretKeySha1, secretKeySha1) || localRevision < 0) {
					localRevisionProperties.setProperty(publicKeySha1PropertyKey, publicKeySha1);

					if (isEmpty(secretKeySha1))
						localRevisionProperties.remove(secretKeySha1PropertyKey);
					else
						localRevisionProperties.setProperty(secretKeySha1PropertyKey, secretKeySha1);

					// It was changed, hence we set this key's localRevision to the current global localRevision.
					localRevision = globalLocalRevision;
					localRevisionProperties.setProperty(localRevisionPropertyKey, Long.toString(localRevision));
				}
				writeLocalRevisionProperties(pgpKeyId);
			}
			return localRevision;
		}
	}

	@Override
	public void testPassphrase(final PgpKey pgpKey, final char[] passphrase) throws IllegalArgumentException, SecurityException {
		assertNotNull("pgpKey", pgpKey);
		final BcPgpKey bcPgpKey = getBcPgpKeyOrFail(pgpKey);
		final PGPSecretKey secretKey = bcPgpKey.getSecretKey();
		if (secretKey == null)
			throw new IllegalArgumentException("pgpKey has no secret key!");

		try {
			secretKey.extractPrivateKey(new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(passphrase));
		} catch (PGPException e) {
			throw new SecurityException(e);
		}
	}

	@Override
	public PgpKey createPgpKey(final CreatePgpKeyParam createPgpKeyParam) {
		assertNotNull("createPgpKeyParam", createPgpKeyParam);
		try {
			final Pair<PGPPublicKeyRing, PGPSecretKeyRing> pair = createPGPSecretKeyRing(createPgpKeyParam);
			final PGPPublicKeyRing pgpPublicKeyRing = pair.a;
			final PGPSecretKeyRing pgpSecretKeyRing = pair.b;
			importPublicKeyRing(pgpPublicKeyRing);
			importSecretKeyRing(pgpSecretKeyRing);

			final PGPSecretKey secretKey = pgpSecretKeyRing.getSecretKey();
			final PgpKey pgpKey = getPgpKey(new PgpKeyId(secretKey.getKeyID()));
			assertNotNull("pgpKey", pgpKey);
			return pgpKey;
		} catch (IOException | NoSuchAlgorithmException | PGPException e) {
			throw new RuntimeException(e);
		}
	}

	private static final class Pair<A, B> {
		public final A a;
		public final B b;

		public Pair(A a, B b) {
			this.a = a;
			this.b = b;
		}
	}

	private Pair<PGPPublicKeyRing, PGPSecretKeyRing> createPGPSecretKeyRing(final CreatePgpKeyParam createPgpKeyParam) throws PGPException, NoSuchAlgorithmException {
		assertNotNull("createPgpKeyParam", createPgpKeyParam);
		final String primaryUserId = createPgpKeyParam.getUserIds().get(0).toString();
		logger.info("createPGPSecretKeyRing: Creating PGP key: primaryUserId='{}' algorithm='{}' strength={}",
				primaryUserId, createPgpKeyParam.getAlgorithm(), createPgpKeyParam.getStrength());

		final Date now = new Date();

		final int masterKeyAlgorithm = getMasterKeyAlgorithm(createPgpKeyParam);
		final int subKey1Algorithm = getSubKey1Algorithm(createPgpKeyParam);
		final int secretKeyEncryptionAlgorithm = SymmetricKeyAlgorithmTags.TWOFISH;
		final PgpSignatureType certificationLevel = PgpSignatureType.POSITIVE_CERTIFICATION;

		// TODO additional user-ids!

		final int[] preferredHashAlgorithms = new int[] { // TODO configurable?!
				HashAlgorithmTags.SHA512,
				HashAlgorithmTags.SHA384,
				HashAlgorithmTags.SHA256,
				HashAlgorithmTags.SHA1
		};

		final int[] preferredSymmetricAlgorithms = new int[] { // TODO configurable?!
				SymmetricKeyAlgorithmTags.TWOFISH,
				SymmetricKeyAlgorithmTags.AES_256,
				SymmetricKeyAlgorithmTags.BLOWFISH
		};

		// null causes an exception - empty is possible, though
		final char[] passphrase = createPgpKeyParam.getPassphrase() == null ? new char[0] : createPgpKeyParam.getPassphrase();

		logger.info("createPGPSecretKeyRing: Creating masterKeyPairGenerator...");
		final AsymmetricCipherKeyPairGenerator masterKeyPairGenerator = createAsymmetricCipherKeyPairGenerator(createPgpKeyParam, 0);

		logger.info("createPGPSecretKeyRing: Creating sub1KeyPairGenerator...");
		final AsymmetricCipherKeyPairGenerator sub1KeyPairGenerator = createAsymmetricCipherKeyPairGenerator(createPgpKeyParam, 1);


		/* Create the master (signing-only) key. */
		logger.info("createPGPSecretKeyRing: Creating masterKeyPair...");
		final BcPGPKeyPair masterKeyPair = new BcPGPKeyPair(masterKeyAlgorithm, masterKeyPairGenerator.generateKeyPair(), now);

		final PGPSignatureSubpacketGenerator masterSubpckGen = new PGPSignatureSubpacketGenerator();

		// Using KeyFlags instead of PGPKeyFlags, because the latter seem incomplete.
		masterSubpckGen.setKeyFlags(false, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER | KeyFlags.AUTHENTICATION);
		masterSubpckGen.setPreferredSymmetricAlgorithms(false, preferredSymmetricAlgorithms);
		masterSubpckGen.setPreferredHashAlgorithms(false, preferredHashAlgorithms);
		masterSubpckGen.setPreferredCompressionAlgorithms(false, new int[] { CompressionAlgorithmTags.ZIP });
		masterSubpckGen.setKeyExpirationTime(false, createPgpKeyParam.getValiditySeconds());


		/* Create an encryption sub-key. */
		logger.info("createPGPSecretKeyRing: Creating sub1KeyPair...");
		final BcPGPKeyPair sub1KeyPair = new BcPGPKeyPair(subKey1Algorithm, sub1KeyPairGenerator.generateKeyPair(), now);

		final PGPSignatureSubpacketGenerator sub1SubpckGen = new PGPSignatureSubpacketGenerator();

		sub1SubpckGen.setKeyFlags(false, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE);
		sub1SubpckGen.setPreferredSymmetricAlgorithms(false, preferredSymmetricAlgorithms);
		sub1SubpckGen.setPreferredHashAlgorithms(false, preferredHashAlgorithms);
		sub1SubpckGen.setPreferredCompressionAlgorithms(false, new int[] { CompressionAlgorithmTags.ZIP });
		sub1SubpckGen.setKeyExpirationTime(false, createPgpKeyParam.getValiditySeconds());


		/* Create the key ring. */
		logger.info("createPGPSecretKeyRing: Creating keyRingGenerator...");
		final BcPGPDigestCalculatorProvider digestCalculatorProvider = new BcPGPDigestCalculatorProvider();
		final BcPGPContentSignerBuilder signerBuilder = new BcPGPContentSignerBuilder(masterKeyAlgorithm, HashAlgorithmTags.SHA512);
		final BcPBESecretKeyEncryptorBuilder pbeSecretKeyEncryptorBuilder = new BcPBESecretKeyEncryptorBuilder(
				secretKeyEncryptionAlgorithm, digestCalculatorProvider.get(HashAlgorithmTags.SHA512));

		// Tried SHA512 for checksumCalculator => org.bouncycastle.openpgp.PGPException: only SHA1 supported for key checksum calculations.
		final PGPDigestCalculator checksumCalculator = digestCalculatorProvider.get(HashAlgorithmTags.SHA1);

		PGPKeyRingGenerator keyRingGenerator = new PGPKeyRingGenerator(
				signatureTypeFromEnum(certificationLevel),
				masterKeyPair,
				primaryUserId,
				checksumCalculator,
				masterSubpckGen.generate(),
				null,
				signerBuilder,
				pbeSecretKeyEncryptorBuilder.build(passphrase));


		/* Add encryption subkey. */
		keyRingGenerator.addSubKey(sub1KeyPair, sub1SubpckGen.generate(), null);


		/* Generate the key ring. */
		logger.info("createPGPSecretKeyRing: generateSecretKeyRing...");
		final PGPSecretKeyRing secretKeyRing = keyRingGenerator.generateSecretKeyRing();

		logger.info("createPGPSecretKeyRing: generatePublicKeyRing...");
		final PGPPublicKeyRing publicKeyRing = keyRingGenerator.generatePublicKeyRing();

		logger.info("createPGPSecretKeyRing: all done!");
		return new Pair<>(publicKeyRing,  secretKeyRing);
	}

	private int getMasterKeyAlgorithm(final CreatePgpKeyParam createPgpKeyParam) {
		switch (createPgpKeyParam.getAlgorithm()) {
			case DSA_AND_EL_GAMAL:
				return PublicKeyAlgorithmTags.DSA;
			case RSA:
				return PublicKeyAlgorithmTags.RSA_SIGN;
			default:
				throw new IllegalStateException("Unknown algorithm: " + createPgpKeyParam.getAlgorithm());
		}
	}

	private int getSubKey1Algorithm(final CreatePgpKeyParam createPgpKeyParam) {
		switch (createPgpKeyParam.getAlgorithm()) {
			case DSA_AND_EL_GAMAL:
				return PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT; // ELGAMAL_GENERAL does not work - Thunderbird/Enigmail says it wouldn't find a suitable sub-key.
			case RSA:
				return PublicKeyAlgorithmTags.RSA_ENCRYPT; // RSA_GENERAL and RSA_ENCRYPT both work.
			default:
				throw new IllegalStateException("Unknown algorithm: " + createPgpKeyParam.getAlgorithm());
		}
	}

	private AsymmetricCipherKeyPairGenerator createAsymmetricCipherKeyPairGenerator(final CreatePgpKeyParam createPgpKeyParam, final int keyIndex) throws NoSuchAlgorithmException {
		final CryptoRegistry cryptoRegistry = CryptoRegistry.getInstance();
		final AsymmetricCipherKeyPairGenerator keyPairGenerator;
		switch (createPgpKeyParam.getAlgorithm()) {
			case DSA_AND_EL_GAMAL:
				if (keyIndex == 0) { // master-key
					keyPairGenerator = cryptoRegistry.createKeyPairGenerator("DSA", false);
					keyPairGenerator.init(createDsaKeyGenerationParameters(createPgpKeyParam));
				}
				else { // sub-key 1
					keyPairGenerator = cryptoRegistry.createKeyPairGenerator("ElGamal", false);
					keyPairGenerator.init(createElGamalKeyGenerationParameters(createPgpKeyParam));
				}
				break;
			case RSA:
				keyPairGenerator = cryptoRegistry.createKeyPairGenerator("RSA", false);
				keyPairGenerator.init(createRsaKeyGenerationParameters(createPgpKeyParam));
				break;
			default:
				throw new IllegalStateException("Unknown algorithm: " + createPgpKeyParam.getAlgorithm());
		}
		return keyPairGenerator;
	}

	private DSAKeyGenerationParameters createDsaKeyGenerationParameters(final CreatePgpKeyParam createPgpKeyParam) {
		/*
		 * How certain do we want to be that the chosen primes are really primes.
		 * <p>
		 * The higher this number, the more tests are done to make sure they are primes (and not composites).
		 * <p>
		 * See: <a href="http://crypto.stackexchange.com/questions/3114/what-is-the-correct-value-for-certainty-in-rsa-key-pair-generation">What is the correct value for “certainty” in RSA key pair generation?</a>
		 * and
		 * <a href="http://crypto.stackexchange.com/questions/3126/does-a-high-exponent-compensate-for-a-low-degree-of-certainty?lq=1">Does a high exponent compensate for a low degree of certainty?</a>
		 */
		final int certainty = 12;

		final SecureRandom random = createSecureRandom();

		final DSAParametersGenerator pGen = new DSAParametersGenerator();
		pGen.init(createPgpKeyParam.getStrength(), certainty, random);
		final DSAParameters dsaParameters = pGen.generateParameters();
		return new DSAKeyGenerationParameters(random, dsaParameters);
	}

	private ElGamalKeyGenerationParameters createElGamalKeyGenerationParameters(final CreatePgpKeyParam createPgpKeyParam) {
		/*
		 * How certain do we want to be that the chosen primes are really primes.
		 * <p>
		 * The higher this number, the more tests are done to make sure they are primes (and not composites).
		 * <p>
		 * See: <a href="http://crypto.stackexchange.com/questions/3114/what-is-the-correct-value-for-certainty-in-rsa-key-pair-generation">What is the correct value for “certainty” in RSA key pair generation?</a>
		 * and
		 * <a href="http://crypto.stackexchange.com/questions/3126/does-a-high-exponent-compensate-for-a-low-degree-of-certainty?lq=1">Does a high exponent compensate for a low degree of certainty?</a>
		 */
		final int certainty = 8; // 12 takes ages - and DSA+El-Gamal is anyway a bad idea and discouraged. Reducing this to make it bearable.

		final SecureRandom random = createSecureRandom();

		ElGamalParametersGenerator pGen = new ElGamalParametersGenerator();
		pGen.init(createPgpKeyParam.getStrength(), certainty, random);
		ElGamalParameters elGamalParameters = pGen.generateParameters();

		// Maybe we should generate our "DH safe primes" only once and store them somewhere? Or maybe we should provide a long list
		// of them in the resources? DHParametersHelper.generateSafePrimes(size, certainty, random); takes really really very long.
		// BUT BEWARE: Attacks on El Gamal can re-use expensively calculated stuff, if p (one of the "safe primes) is the same.
		// However, it is still not *so* easy. Hmmm... don't know. Security is really important here.

		return new ElGamalKeyGenerationParameters(random, elGamalParameters);
	}

	private RSAKeyGenerationParameters createRsaKeyGenerationParameters(final CreatePgpKeyParam createPgpKeyParam) {
		/*
		 * This value should be a Fermat number. 0x10001 (F4) is current recommended value. 3 (F1) is known to be safe also.
		 * 3, 5, 17, 257, 65537, 4294967297, 18446744073709551617,
		 * <p>
		 * Practically speaking, Windows does not tolerate public exponents which do not fit in a 32-bit unsigned integer.
		 * Using e=3 or e=65537 works "everywhere".
		 * <p>
		 * See: <a href="http://stackoverflow.com/questions/11279595/rsa-public-exponent-defaults-to-65537-what-should-this-value-be-what-are-the">stackoverflow: RSA Public exponent defaults to 65537. ... What are the impacts of my choices?</a>
		 */
		final BigInteger publicExponent = BigInteger.valueOf(0x10001);

		/*
		 * How certain do we want to be that the chosen primes are really primes.
		 * <p>
		 * The higher this number, the more tests are done to make sure they are primes (and not composites).
		 * <p>
		 * See: <a href="http://crypto.stackexchange.com/questions/3114/what-is-the-correct-value-for-certainty-in-rsa-key-pair-generation">What is the correct value for “certainty” in RSA key pair generation?</a>
		 * and
		 * <a href="http://crypto.stackexchange.com/questions/3126/does-a-high-exponent-compensate-for-a-low-degree-of-certainty?lq=1">Does a high exponent compensate for a low degree of certainty?</a>
		 */
		final int certainty = 12;

		return new RSAKeyGenerationParameters(
				publicExponent, createSecureRandom(), createPgpKeyParam.getStrength(), certainty);
	}

	private SecureRandom createSecureRandom() {
		return new SecureRandom();
	}
}
