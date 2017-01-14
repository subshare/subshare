package org.subshare.core.locker.transport.local;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import org.subshare.core.locker.LockerContent;
import org.subshare.core.locker.LockerEncryptedDataFile;
import org.subshare.core.locker.transport.AbstractLockerTransport;
import org.subshare.core.pgp.PgpDecoder;
import org.subshare.core.pgp.PgpEncoder;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpSignature;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;
import co.codewizards.cloudstore.core.io.LockFile;
import co.codewizards.cloudstore.core.io.LockFileFactory;
import co.codewizards.cloudstore.core.oio.File;

public class LocalLockerTransport extends AbstractLockerTransport {

	private File localLockerTransportPropertiesFile;
	private Properties localLockerTransportProperties;

	private final Set<Uid> mergedVersions = new LinkedHashSet<>();

	public LocalLockerTransport() {
	}

	@Override
	public List<Uid> getVersions() {
		return Collections.singletonList(getVersion());
	}

	@Override
	public void setLockerContent(LockerContent lockerContent) {
		super.setLockerContent(lockerContent);
		mergedVersions.clear(); // versions are per file (= lockerContent) *and* per OpenPGP-key.
	}

	@Override
	public void setPgpKey(PgpKey pgpKey) {
		super.setPgpKey(pgpKey);
		mergedVersions.clear(); // prevent the different PgpKeys of a user from becoming associated!
	}

	@Override
	public void addMergedVersions(final List<Uid> serverVersions) {
		assertNotNull(serverVersions, "serverVersions");
		mergedVersions.addAll(serverVersions);
	}

	protected Uid getVersion() {
		// We maintain one version per OpenPGP key in order to avoid the different OpenPGP keys from
		// being somehow linked (other than by access time correlation - which we might solve later, too).
		// Therefore, we generate a new version-per-pgp-key whenever the underlying version changes.

		final LockerContent lockerContent = getLockerContentOrFail();
		final Uid lockerContentLocalVersion = lockerContent.getLocalVersion();
		if (lockerContentLocalVersion == null)
			throw new IllegalStateException(String.format("Implementation error: %s.getLocalVersion() returned null! ", lockerContent.getClass().getName()));

		String s = getLocalLockerTransportProperties().getProperty(getLocalVersionPropertyKey());
		Uid persistentLocalVersion = isEmpty(s) ? null : new Uid(s);

		s = getLocalLockerTransportProperties().getProperty(getServerVersionPropertyKey());
		Uid persistentServerVersion = isEmpty(s) ? null : new Uid(s);

		if (!lockerContentLocalVersion.equals(persistentLocalVersion) || persistentServerVersion == null) {
			persistentLocalVersion = lockerContentLocalVersion;
			persistentServerVersion = new Uid();
			getLocalLockerTransportProperties().setProperty(getLocalVersionPropertyKey(), persistentLocalVersion.toString());
			getLocalLockerTransportProperties().setProperty(getServerVersionPropertyKey(), persistentServerVersion.toString());
			writeLocalLockerTransportProperties();
		}
		return persistentServerVersion;
	}

	public String getLocalVersionPropertyKey() {
		final PgpKeyId pgpKeyId = getPgpKeyOrFail().getPgpKeyId();
		return String.format("pgpKey[%s].lockerContent[%s].localVersion", pgpKeyId, getLockerContentOrFail().getName());
	}

	public String getServerVersionPropertyKey() {
		final PgpKeyId pgpKeyId = getPgpKeyOrFail().getPgpKeyId();
		return String.format("pgpKey[%s].lockerContent[%s].serverVersion", pgpKeyId, getLockerContentOrFail().getName());
	}

	@Override
	public List<LockerEncryptedDataFile> getEncryptedDataFiles() {
		final LockerContent lockerContent = getLockerContentOrFail();
		final PgpKey pgpKey = getPgpKeyOrFail();

		final LockerEncryptedDataFile encryptedDataFile = new LockerEncryptedDataFile();
		encryptedDataFile.setContentName(lockerContent.getName());
		encryptedDataFile.setContentVersion(getVersion());
		encryptedDataFile.setReplacedContentVersions(mergedVersions);
		try {
			encryptedDataFile.signManifestData(pgpKey);

			final byte[] localData = lockerContent.getLocalData();
			if (localData == null)
				throw new IllegalStateException(String.format("Implementation error: %s.getLocalData() returned null! ", lockerContent.getClass().getName()));

			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final PgpEncoder encoder = getPgp().createEncoder(new ByteArrayInputStream(localData), out);
			encoder.getEncryptPgpKeys().add(pgpKey);
			encoder.setSignPgpKey(pgpKey);
			encoder.encode();

			encryptedDataFile.putDefaultData(out.toByteArray());
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
		return Collections.singletonList(encryptedDataFile);
	}

	@Override
	public void putEncryptedDataFile(final LockerEncryptedDataFile encryptedDataFile) {
		assertNotNull(encryptedDataFile, "encryptedDataFile");
		final Uid contentVersion = encryptedDataFile.getContentVersion();
		assertNotNull(contentVersion, "encryptedDataFile.contentVersion");

		final PgpSignature manifestSignature = encryptedDataFile.assertManifestSignatureValid();
		final PgpKeyId pgpKeyId = getPgpKeyOrFail().getPgpKeyId();

		PgpKey manifestSignatureKey = getPgp().getPgpKey(manifestSignature.getPgpKeyId());
		if (manifestSignatureKey == null)
			throw new IllegalStateException(String.format("PGP key (used for signing the manifest) not found: %s", manifestSignature.getPgpKeyId()));

		manifestSignatureKey = manifestSignatureKey.getMasterKey();

		if (! pgpKeyId.equals(manifestSignatureKey.getPgpKeyId()))
			throw new IllegalStateException(String.format("pgpKeyId != manifestSignatureKey.pgpKeyId :: %s != %s", pgpKeyId, manifestSignature.getPgpKeyId()));

		if (mergedVersions.contains(contentVersion))
			return; // no need to merge the same version multiple times - can theoretically happen because of multiple servers, but currently we sync only one server at a time - so purely theoretical at the moment.

		try {
			final byte[] defaultData = encryptedDataFile.getDefaultData();
			assertNotNull(defaultData, "encryptedDataFile.defaultData");

			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final PgpDecoder decoder = getPgp().createDecoder(new ByteArrayInputStream(defaultData), out);
			decoder.decode();
			final PgpSignature defaultDataSignature = decoder.getPgpSignature();
			if (defaultDataSignature == null)
				throw new SignatureException("Missing signature!");

			if (decoder.getDecryptPgpKey() == null)
				throw new IllegalStateException("WTF?! The data was not encrypted!");

			PgpKey defaultDataSignatureKey = getPgp().getPgpKey(defaultDataSignature.getPgpKeyId());
			if (defaultDataSignatureKey == null)
				throw new IllegalStateException(String.format("PGP key (used for signing the default-data) not found: %s", defaultDataSignature.getPgpKeyId()));

			defaultDataSignatureKey = defaultDataSignatureKey.getMasterKey();

			if (! pgpKeyId.equals(defaultDataSignatureKey.getPgpKeyId()))
				throw new IllegalStateException(String.format("pgpKeyId != defaultDataSignature.pgpKeyId :: %s != %s", pgpKeyId, defaultDataSignature.getPgpKeyId()));

			getLockerContentOrFail().mergeFrom(out.toByteArray());
			mergedVersions.add(contentVersion);
		} catch (IOException x) {
			throw new RuntimeException(x);
		}
	}

	private File getLocalLockerTransportPropertiesFile() {
		if (localLockerTransportPropertiesFile == null)
			localLockerTransportPropertiesFile = createFile(ConfigDir.getInstance().getFile(), "localLockerTransport.properties");

		return localLockerTransportPropertiesFile;
	}

	private Properties getLocalLockerTransportProperties() {
		if (localLockerTransportProperties == null) {
			try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getLocalLockerTransportPropertiesFile(), 30000);) {
				final Lock lock = lockFile.getLock();
				lock.lock();
				try {
					if (localLockerTransportProperties == null) {
						final Properties p = new Properties();
						try (final InputStream in = castStream(lockFile.createInputStream())) {
							p.load(in);
						}
						localLockerTransportProperties = p;
					}
				} finally {
					lock.unlock();
				}
			} catch (final IOException x) {
				throw new RuntimeException(x);
			}
		}
		return localLockerTransportProperties;
	}

	private void writeLocalLockerTransportProperties() {
		final Properties localLockerTransportProperties = getLocalLockerTransportProperties();
		synchronized (localLockerTransportProperties) {
			try (final LockFile lockFile = LockFileFactory.getInstance().acquire(getLocalLockerTransportPropertiesFile(), 30000);) {
				try (final OutputStream out = castStream(lockFile.createOutputStream())) { // acquires LockFile.lock implicitly
					localLockerTransportProperties.store(out, null);
				}
			} catch (final IOException x) {
				throw new RuntimeException(x);
			}
		}
	}
}
