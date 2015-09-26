package org.subshare.test;

import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

import org.subshare.core.pgp.Pgp;
import org.subshare.core.pgp.PgpKey;
import org.subshare.core.pgp.PgpKeyId;
import org.subshare.core.pgp.PgpRegistry;
import org.subshare.core.pgp.PgpSignature;
import org.subshare.core.pgp.gnupg.BcWithLocalGnuPgPgp;
import org.subshare.core.pgp.gnupg.GnuPgDir;
import org.subshare.core.pgp.sync.PgpSync;
import org.subshare.core.server.Server;
import org.subshare.core.server.ServerImpl;
import org.junit.BeforeClass;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.junit.runner.RunWith;

import co.codewizards.cloudstore.core.config.ConfigDir;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;

@RunWith(JMockit.class)
public class PgpSyncIT extends AbstractIT {

	static File gnuPgDirFile;
	static File clientGnuPgDirFile;
	static File serverGnuPgDirFile;

	static File configDir;
	static File clientConfigDir;
	static File serverConfigDir;

	static Pgp clientPgp;
	static Pgp serverPgp;

	@BeforeClass
	public static void beforePgpSyncIT() throws Exception {
		clientGnuPgDirFile = createFile("build/" + jvmInstanceId + "/client/.gnupg");
		clientGnuPgDirFile.mkdirs();
		serverGnuPgDirFile = createFile("build/" + jvmInstanceId + "/server/.gnupg");
		serverGnuPgDirFile.mkdirs();

		clientConfigDir = createFile("build/" + jvmInstanceId + "/client/.subshare");
		clientConfigDir.mkdirs();
		serverConfigDir = createFile("build/" + jvmInstanceId + "/server/.subshare");
		serverConfigDir.mkdirs();

		new MockUp<ConfigDir>() {
			@Mock
			File getFile(Invocation invocation) {
				if (configDir != null)
					return configDir;

				return invocation.proceed();
			}
		};

		new MockUp<GnuPgDir>() {
			@Mock
			File getFile() {
				if (gnuPgDirFile != null)
					return gnuPgDirFile;

				throw new IllegalStateException("Unexpected invocation of this method!");
			}
		};

		new MockUp<PgpRegistry>() {
			@Mock
			Pgp getPgpOrFail(Invocation invocation) {
				if (isServerThread())
					return serverPgp;
				else
					return clientPgp;
			}
		};
	}

	@Override
	public void before() throws Exception {
		super.before();

		clientGnuPgDirFile.deleteRecursively();
		serverGnuPgDirFile.deleteRecursively();
		deleteGpgFiles(clientConfigDir);
		deleteGpgFiles(serverConfigDir);

		setupPgp(clientGnuPgDirFile, "marco");

		gnuPgDirFile = clientGnuPgDirFile;
		configDir = clientConfigDir;
		clientPgp = new BcWithLocalGnuPgPgp();
		clientPgp.getMasterKeys(); // force initialisation!
		clientPgp.getLocalRevision(); // force initialisation!

		gnuPgDirFile = serverGnuPgDirFile;
		configDir = serverConfigDir;
		serverPgp = new BcWithLocalGnuPgPgp();
		serverPgp.getMasterKeys(); // force initialisation!
		serverPgp.getLocalRevision(); // force initialisation!

		gnuPgDirFile = null;
		configDir = null;
	}

	private static void deleteGpgFiles(File configDir) {
		createFile(configDir, "gpg.properties").delete();
		createFile(configDir, "gpgLocalRevision").deleteRecursively();
	}

	@Override
	public void after() throws Exception {
		clientPgp = null;
		serverPgp = null;
		super.after();
	}

	public static final String PUBRING_FILE_NAME = "pubring.gpg";
	public static final String SECRING_FILE_NAME = "secring.gpg";

	protected static void setupPgp(final File gnuPgDir, String ownerName) throws Exception {
		final String gpgDir = "gpg/" + ownerName;

		gnuPgDir.getParentFile().mkdir();
		gnuPgDir.mkdir();
		copyResource(gpgDir + '/' + PUBRING_FILE_NAME, createFile(gnuPgDir, PUBRING_FILE_NAME));
		copyResource(gpgDir + '/' + SECRING_FILE_NAME, createFile(gnuPgDir, SECRING_FILE_NAME));
	}

	private static void copyResource(final String sourceResName, final File destinationFile) throws IOException {
		IOUtil.copyResource(AbstractIT.class, sourceResName, destinationFile);
	}

	private static boolean isServerThread() {
		final StackTraceElement[] stackTrace = new Exception().getStackTrace();
		for (final StackTraceElement stackTraceElement : stackTrace) {
			if ("org.eclipse.jetty.server.Server".equals(stackTraceElement.getClassName()))
				return true;
		}
		return false;
	}

	@Test
	public void testPgpSync() throws Exception {
		Server server = new ServerImpl();
		server.setUrl(new URL(getSecureUrl()));

		// *** BEGIN sync 1 - from fully filled client-keyring to empty server-keyring
		Map<PgpKey, Collection<PgpSignature>> clientMasterKeysBeforeSync1 = getSignatures(clientPgp, clientPgp.getMasterKeys());
		assertThat(clientMasterKeysBeforeSync1).isNotEmpty();

		assertThat(serverPgp.getMasterKeys()).isEmpty();

		try (PgpSync pgpSync = new PgpSync(server);) {
			pgpSync.sync();
		}

		Map<PgpKey, Collection<PgpSignature>> clientMasterKeysAfterSync1 = getSignatures(clientPgp, clientPgp.getMasterKeys());
		assertThat(clientMasterKeysAfterSync1).isNotEmpty();

		Map<PgpKey, Collection<PgpSignature>> serverMasterKeysAfterSync1 = getSignatures(serverPgp, serverPgp.getMasterKeys());
		assertThat(serverMasterKeysAfterSync1).isNotEmpty();


		assertPgpKeysEqual(clientMasterKeysAfterSync1, clientMasterKeysBeforeSync1);
		assertPgpKeysEqual(serverMasterKeysAfterSync1, clientMasterKeysAfterSync1);
		// *** END sync 1


		// *** BEGIN sync 2 - adding one key each on server- and client-side
		try (InputStream in = PgpSyncIT.class.getResourceAsStream("gpg/aaa_0x710E3371.asc");) {
			clientPgp.importKeys(in);
		}
		Map<PgpKey, Collection<PgpSignature>> clientMasterKeysBeforeSync2 = getSignatures(clientPgp, clientPgp.getMasterKeys());

		try (InputStream in = PgpSyncIT.class.getResourceAsStream("gpg/bbb_0x64C77207.asc");) {
			serverPgp.importKeys(in);
		}
		Map<PgpKey, Collection<PgpSignature>> serverMasterKeysBeforeSync2 = getSignatures(serverPgp, serverPgp.getMasterKeys());


		assertThat(clientMasterKeysBeforeSync2.size()).isEqualTo(clientMasterKeysAfterSync1.size() + 1);
		assertThat(serverMasterKeysBeforeSync2.size()).isEqualTo(serverMasterKeysAfterSync1.size() + 1);

		try (PgpSync pgpSync = new PgpSync(server);) {
			pgpSync.sync();
		}

		Map<PgpKey, Collection<PgpSignature>> clientMasterKeysAfterSync2 = getSignatures(clientPgp, clientPgp.getMasterKeys());
		Map<PgpKey, Collection<PgpSignature>> serverMasterKeysAfterSync2 = getSignatures(serverPgp, serverPgp.getMasterKeys());

		// We do *not* sync down keys that the client does not yet have. Hence, the client's keys should be unchanged!
		assertPgpKeysEqual(clientMasterKeysAfterSync2, clientMasterKeysBeforeSync2);

		// However, we do sync up all keys, hence the server should now have one more.
		assertThat(serverMasterKeysAfterSync2.size()).isEqualTo(serverMasterKeysBeforeSync2.size() + 1);
		// *** END sync 2


		// *** BEGIN sync 3
		try (InputStream in = PgpSyncIT.class.getResourceAsStream("gpg/0xAA97DDBD_with_aaa_sig.asc");) {
			clientPgp.importKeys(in);
		}
		Map<PgpKey, Collection<PgpSignature>> clientMasterKeysBeforeSync3 = getSignatures(clientPgp, clientPgp.getMasterKeys());
		assertThat(clientMasterKeysBeforeSync3.size()).isEqualTo(clientMasterKeysAfterSync2.size()); // no new key, only new signature!

		try (InputStream in = PgpSyncIT.class.getResourceAsStream("gpg/0xAA97DDBD_with_bbb_sig.asc");) {
			serverPgp.importKeys(in);
		}
		Map<PgpKey, Collection<PgpSignature>> serverMasterKeysBeforeSync3 = getSignatures(serverPgp, serverPgp.getMasterKeys());
		assertThat(serverMasterKeysBeforeSync3.size()).isEqualTo(serverMasterKeysAfterSync2.size()); // no new key, only new signature!

		Map<PgpKey, Collection<PgpSignature>> filteredClientMasterKeysBeforeSync3 = filterByPgpKeyIds(clientMasterKeysBeforeSync3, new PgpKeyId("d7a92a24aa97ddbd"));
		Map<PgpKey, Collection<PgpSignature>> filteredServerMasterKeysBeforeSync3 = filterByPgpKeyIds(serverMasterKeysBeforeSync3, new PgpKeyId("d7a92a24aa97ddbd"));

		try {
			assertPgpKeysEqual(filteredClientMasterKeysBeforeSync3, filteredServerMasterKeysBeforeSync3);
			fail("Signature comparison failed to detect difference!");
		} catch (ComparisonFailure f) {
			doNothing(); // we expect this ;-)
		}

		try (PgpSync pgpSync = new PgpSync(server);) {
			pgpSync.sync();
		}

		Map<PgpKey, Collection<PgpSignature>> clientMasterKeysAfterSync3 = getSignatures(clientPgp, clientPgp.getMasterKeys());
		Map<PgpKey, Collection<PgpSignature>> serverMasterKeysAfterSync3 = getSignatures(serverPgp, serverPgp.getMasterKeys());

		Map<PgpKey, Collection<PgpSignature>> filteredClientMasterKeysAfterSync3 = filterByPgpKeyIds(clientMasterKeysAfterSync3, new PgpKeyId("d7a92a24aa97ddbd"));
		Map<PgpKey, Collection<PgpSignature>> filteredServerMasterKeysAfterSync3 = filterByPgpKeyIds(serverMasterKeysAfterSync3, new PgpKeyId("d7a92a24aa97ddbd"));
		assertPgpKeysEqual(filteredClientMasterKeysAfterSync3, filteredServerMasterKeysAfterSync3);

		// *** END sync 3
	}

	private static Map<PgpKey, Collection<PgpSignature>> filterByPgpKeyIds(Map<PgpKey, Collection<PgpSignature>> inputMap, PgpKeyId ... pgpKeyIds) {
		Set<PgpKeyId> pgpKeyIdSet = new HashSet<>(Arrays.asList(pgpKeyIds));
		Map<PgpKey, Collection<PgpSignature>> result = new HashMap<PgpKey, Collection<PgpSignature>>();
		for (Map.Entry<PgpKey, Collection<PgpSignature>> me : inputMap.entrySet()) {
			PgpKey pgpKey = me.getKey();
			Collection<PgpSignature> signatures = me.getValue();
			if (pgpKeyIdSet.contains(pgpKey.getPgpKeyId()))
				result.put(pgpKey, signatures);
		}
		return result;
	}

	private static Map<PgpKey, Collection<PgpSignature>> getSignatures(Pgp pgp, Collection<PgpKey> pgpKeys) {
		Map<PgpKey, Collection<PgpSignature>> result = new LinkedHashMap<>(pgpKeys.size());
		for (PgpKey pgpKey : pgpKeys) {
			Collection<PgpSignature> signatures = new HashSet<PgpSignature>(pgp.getCertifications(pgpKey));
			result.put(pgpKey, signatures);
		}
		assertThat(result.size()).isEqualTo(pgpKeys.size());
		return result;
	}

	private static void assertPgpKeysEqual(Map<PgpKey, Collection<PgpSignature>> pgpKeysWithSignatures1, Map<PgpKey, Collection<PgpSignature>> pgpKeysWithSignatures2) {
		assertThat(pgpKeysWithSignatures1).isNotNull();
		assertThat(pgpKeysWithSignatures2).isNotNull();

		Set<PgpKey> pgpKeys1 = pgpKeysWithSignatures1.keySet();
		Set<PgpKey> pgpKeys2 = pgpKeysWithSignatures2.keySet();

		assertPgpKeysEqual(pgpKeys1, pgpKeys2);

		for (Map.Entry<PgpKey, Collection<PgpSignature>> me : pgpKeysWithSignatures1.entrySet()) {
			PgpKey pgpKey1 = me.getKey();
			Collection<PgpSignature> signatures1 = me.getValue();
			Collection<PgpSignature> signatures2 = pgpKeysWithSignatures2.get(pgpKey1);
			assertThat(signatures1).isNotNull();
			assertThat(signatures2).isNotNull();

			assertThat(signatures1.size()).isEqualTo(signatures2.size());
			assertThat(getSignaturePgpKeyIds(signatures1)).isEqualTo(getSignaturePgpKeyIds(signatures2));
		}
	}

	private static Set<PgpKeyId> getSignaturePgpKeyIds(Collection<PgpSignature> signatures) {
		Set<PgpKeyId> result = new HashSet<>();
		for (PgpSignature signature : signatures) {
			result.add(signature.getPgpKeyId());
		}
		return result;
	}


	private static void assertPgpKeysEqual(Collection<PgpKey> pgpKeys1, Collection<PgpKey> pgpKeys2) {
	    if (pgpKeys1 == null && pgpKeys2 == null)
	        return;

	    if (pgpKeys1 == null)
	        throw new ComparisonFailure("pgpKeys1 is null, but pgpKeys2 is not!", String.valueOf(pgpKeys1), String.valueOf(pgpKeys2));

	    if (pgpKeys2 == null)
            throw new ComparisonFailure("pgpKeys2 is null, but pgpKeys1 is not!", String.valueOf(pgpKeys1), String.valueOf(pgpKeys2));

		Map<PgpKeyId, PgpKey> pgpKeyId2PgpKey1 = new HashMap<>(pgpKeys1.size());
		for (PgpKey pgpKey1 : pgpKeys1)
			pgpKeyId2PgpKey1.put(pgpKey1.getPgpKeyId(), pgpKey1);

		Map<PgpKeyId, PgpKey> pgpKeyId2PgpKey2 = new HashMap<>(pgpKeys2.size());
		for (PgpKey pgpKey2 : pgpKeys2)
			pgpKeyId2PgpKey2.put(pgpKey2.getPgpKeyId(), pgpKey2);

		for (PgpKey pgpKey1 : pgpKeys1) {
			PgpKey pgpKey2 = pgpKeyId2PgpKey2.get(pgpKey1.getPgpKeyId());
			if (pgpKey2 == null)
				throw new ComparisonFailure(String.format("The PgpKey with pgpKeyId=%s is missing in pgpKeys2!", pgpKey1.getPgpKeyId()), "", "");
		}

		for (PgpKey pgpKey2 : pgpKeys2) {
			PgpKey pgpKey1 = pgpKeyId2PgpKey1.get(pgpKey2.getPgpKeyId());
			if (pgpKey1 == null)
				throw new ComparisonFailure(String.format("The PgpKey with pgpKeyId=%s is missing in pgpKeys1!", pgpKey2.getPgpKeyId()), "", "");

			assertThat(pgpKey1.getFingerprint()).isEqualTo(pgpKey2.getFingerprint());
			assertPgpKeysEqual(pgpKey1.getSubKeys(), pgpKey2.getSubKeys());
			assertThat(pgpKey1.getUserIds()).isEqualTo(pgpKey2.getUserIds());
		}

		assertThat(pgpKeys1.size()).isEqualTo(pgpKeys2.size());
	}
}
