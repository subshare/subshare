package org.subshare.core.pgp;

import static co.codewizards.cloudstore.core.io.StreamUtil.*;
import static co.codewizards.cloudstore.core.oio.OioFileFactory.*;
import static co.codewizards.cloudstore.core.util.Util.*;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.core.dto.CreateRepositoryRequestDto;
import org.subshare.core.gpg.GnuPgTest;
import org.subshare.core.pgp.gnupg.GnuPgDir;
import org.subshare.core.sign.PgpSignableSigner;
import org.subshare.core.sign.PgpSignableVerifier;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.io.ByteArrayInputStream;
import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;
import co.codewizards.cloudstore.core.io.IInputStream;
import co.codewizards.cloudstore.core.io.IOutputStream;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;

public class PgpTest {
	private static final Logger logger = LoggerFactory.getLogger(PgpTest.class);

	private static Random random = new Random();
	private static File buildJvmInstanceIdDir;
	private static Pgp pgp;

	@BeforeClass
	public static void beforePgpTest() throws Exception {
		final Uid jvmInstanceId = new Uid(); // for parallel test execution ;-)
		System.setProperty(Config.SYSTEM_PROPERTY_PREFIX + GnuPgDir.CONFIG_KEY_GNU_PG_DIR, "build/" + jvmInstanceId + "/.gnupg");
		buildJvmInstanceIdDir = createFile("build/" + jvmInstanceId);
		buildJvmInstanceIdDir.mkdir();

		initPgp();

		pgp = PgpRegistry.getInstance().getPgpOrFail();
	}

	@AfterClass
	public static void afterPgpTest() throws Exception {
		pgp = null;

		PgpRegistry.getInstance().clearCache();

		if (buildJvmInstanceIdDir != null) {
			buildJvmInstanceIdDir.deleteRecursively();
			buildJvmInstanceIdDir = null;
		}
	}

	@Test
	public void inlineSignAndVerify() throws Exception {
		byte[] testData = createTestData();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PgpEncoder encoder = pgp.createEncoder(new ByteArrayInputStream(testData), out);

		PgpKeyId signPgpKeyId = new PgpKeyId("70c642ca41cd4390");
		PgpKey signPgpKey = pgp.getPgpKey(signPgpKeyId);
		encoder.setSignPgpKey(signPgpKey);

		encoder.encode();

		byte[] inlineSignedData = out.toByteArray();
		out.reset();

		assertThat(inlineSignedData).isNotEqualTo(testData);
		assertThat(inlineSignedData.length).isGreaterThan(testData.length);

		PgpDecoder decoder = pgp.createDecoder(new ByteArrayInputStream(inlineSignedData), out);
		decoder.decode();

		byte[] decodedData = out.toByteArray();
		out.reset();

		assertThat(decodedData).isEqualTo(testData);
		assertThat(decoder.getPgpSignature()).isNotNull();

		PgpKey signatureMasterKey = pgp.getPgpKey(decoder.getPgpSignature().getPgpKeyId());
		assertThat(signatureMasterKey).isNotNull();
		signatureMasterKey = signatureMasterKey.getMasterKey();

		assertThat(signatureMasterKey.getPgpKeyId()).isEqualTo(signPgpKey.getPgpKeyId());
		assertThat(decoder.getDecryptPgpKey()).isNull();

		// break signature and try again
		modifyOneByte(inlineSignedData);
		try {
			decoder = pgp.createDecoder(new ByteArrayInputStream(inlineSignedData), out);
			decoder.decode();
			fail("Modification not detected!");
		} catch (Exception x) { // Might be every imaginable exception (not only SignatureException), because we might have corrupted meta-data needed for basic reading.
			logger.debug("Caught expected exception: " + x, x);
		}
	}

	private void modifyOneByte(final byte[] byteArray) {
		int byteIndex = random.nextInt(byteArray.length);
		byte oldValue = byteArray[byteIndex];
		while (oldValue == byteArray[byteIndex])
			byteArray[byteIndex] = (byte) random.nextInt(256);
	}

	/**
	 * PGP does not support encrypt-then-sign! When combining signing + encrypting, then PGP always signs the original,
	 * plain data and puts the signature inside the encrypted data. To verify a signature, the data must thus first
	 * be decrypted.
	 */
	@Test
	public void inlineSignThenEncryptAndDecryptThenVerify() throws Exception {
		byte[] testData = createTestData();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PgpEncoder encoder = pgp.createEncoder(new ByteArrayInputStream(testData), out);

		PgpKeyId signPgpKeyId = new PgpKeyId("70c642ca41cd4390");
		PgpKey signPgpKey = pgp.getPgpKey(signPgpKeyId);
		encoder.setSignPgpKey(signPgpKey);

		PgpKeyId encryptPgpKeyId = new PgpKeyId("d7a92a24aa97ddbd");
		PgpKey encryptPgpKey = pgp.getPgpKey(encryptPgpKeyId);
		encoder.getEncryptPgpKeys().add(encryptPgpKey);

		encoder.encode();

		byte[] inlineSignedEncryptedData = out.toByteArray();
		out.reset();

		assertThat(inlineSignedEncryptedData).isNotEqualTo(testData);
		assertThat(inlineSignedEncryptedData.length).isGreaterThan(testData.length);

		PgpDecoder decoder = pgp.createDecoder(new ByteArrayInputStream(inlineSignedEncryptedData), out);
		decoder.decode();

		byte[] decodedData = out.toByteArray();
		out.reset();

		assertThat(decodedData).isEqualTo(testData);
		assertThat(decoder.getPgpSignature()).isNotNull();
		assertThat(decoder.getPgpSignature().getPgpKeyId()).isEqualTo(signPgpKey.getPgpKeyForSignatureOrFail().getPgpKeyId());
		assertThat(decoder.getDecryptPgpKey()).isNotNull();

		PgpKey decryptMasterKey = decoder.getDecryptPgpKey();
		decryptMasterKey = decryptMasterKey.getMasterKey();

		assertThat(decryptMasterKey).isSameAs(encryptPgpKey);

		// break signature and try again
		modifyOneByte(inlineSignedEncryptedData);
		try {
			decoder = pgp.createDecoder(new ByteArrayInputStream(inlineSignedEncryptedData), out);
			decoder.decode();
			fail("Modification not detected!");
		} catch (Exception x) { // Might be every imaginable exception (not only SignatureException), because we might have corrupted meta-data needed for basic reading.
			logger.debug("Caught expected exception: " + x, x);
		}
	}

	@Test
	public void detachedSignAndVerify() throws Exception {
		byte[] testData = createTestData();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream signOut = new ByteArrayOutputStream();
		PgpEncoder encoder = pgp.createEncoder(new ByteArrayInputStream(testData), out);

		PgpKeyId signPgpKeyId = new PgpKeyId("70c642ca41cd4390");
		PgpKey signPgpKey = pgp.getPgpKey(signPgpKeyId);
		encoder.setSignPgpKey(signPgpKey);
		encoder.setSignOutputStream(signOut);

		encoder.encode();

		byte[] nonSignedOutputData = out.toByteArray();
		out.reset();

		byte[] detachedSignature = signOut.toByteArray();
		signOut.reset();

		assertThat(nonSignedOutputData.length).isEqualTo(testData.length);
		assertThat(nonSignedOutputData).isEqualTo(testData);

		PgpDecoder decoder = pgp.createDecoder(new ByteArrayInputStream(nonSignedOutputData), out);
		decoder.setSignInputStream(new ByteArrayInputStream(detachedSignature));
		decoder.decode();

		byte[] decodedData = out.toByteArray();
		out.reset();

		assertThat(decodedData).isEqualTo(testData);
		assertThat(decoder.getPgpSignature()).isNotNull();

		PgpKey signatureMasterKey = pgp.getPgpKey(decoder.getPgpSignature().getPgpKeyId());
		assertThat(signatureMasterKey).isNotNull();
		signatureMasterKey = signatureMasterKey.getMasterKey();

		assertThat(signatureMasterKey.getPgpKeyId()).isEqualTo(signPgpKey.getPgpKeyId());
		assertThat(decoder.getDecryptPgpKey()).isNull();

		// break signature by modifying signed data and try again
		modifyOneByte(nonSignedOutputData);
		try {
			decoder = pgp.createDecoder(new ByteArrayInputStream(nonSignedOutputData), out);
			decoder.setSignInputStream(new ByteArrayInputStream(detachedSignature));
			decoder.decode();
			fail("Modification not detected!");
		} catch (SignatureException x) { // must always be a SignatureException, because the plain input was modified - not any PGP-specific data
			logger.debug("Caught expected exception: " + x, x);
		}

		// break signature by modifying signature data and try again
		modifyOneByte(detachedSignature);
		try {
			decoder = pgp.createDecoder(new ByteArrayInputStream(testData), out);
			decoder.setSignInputStream(new ByteArrayInputStream(detachedSignature));
			decoder.decode();
			fail("Modification not detected!");
		} catch (Exception x) { // Might be every imaginable exception (not only SignatureException), because we might have corrupted meta-data needed for basic reading.
			logger.debug("Caught expected exception: " + x, x);
		}
	}

//	@Test
//	public void experiments() throws Exception {
//		try (InputStream in = new FileInputStream("/tmp/experiment-key.asc");) {
//			pgp.importKeys(in);
//		}
//		PgpKey pgpKey = pgp.getPgpKey(new PgpKeyId("82C52BC94AB0FBC1"));
//		assertThat(pgpKey).isNotNull();
//
//		pgp.getUserIdSignatures(pgpKey);
//	}

	@Test
	public void exportImportPublicKey() throws Exception {
		Set<PgpKey> pgpKeys = new HashSet<>();

		PgpKey pgpKey = pgp.getPgpKey(new PgpKeyId("70c642ca41cd4390"));
		assertThat(pgpKey).isNotNull();
		pgpKeys.add(pgpKey);

		pgpKey = pgp.getPgpKey(new PgpKeyId("d7a92a24aa97ddbd"));
		assertThat(pgpKey).isNotNull();
		assertThat(pgp.getCertifications(pgpKey).size()).isEqualTo(2); // 1 self-signed + 1 signed by 70c642ca41cd4390
		pgpKeys.add(pgpKey);

		File tempFile = createTempFile("pubkeys-", ".gpg");
		try (IOutputStream out = tempFile.createOutputStream();) {
			pgp.exportPublicKeys(pgpKeys, out);
		}

		try (IInputStream in = castStream(GnuPgTest.class.getResourceAsStream("0xAA97DDBD_with_aaa_sig.asc"))) {
			pgp.importKeys(in);
		}

		// check for 1 *additional* signature
		pgpKey = pgp.getPgpKey(new PgpKeyId("d7a92a24aa97ddbd"));
		assertThat(pgpKey).isNotNull();
		assertThat(pgp.getCertifications(pgpKey).size()).isEqualTo(3);


		try (IInputStream in = castStream(GnuPgTest.class.getResourceAsStream("0xAA97DDBD_with_bbb_sig.asc"))) {
			pgp.importKeys(in);
		}

		// check for again 1 *additional* signature
		assertThat(pgp.getCertifications(pgpKey).size()).isEqualTo(4);


		try (IInputStream in = castStream(GnuPgTest.class.getResourceAsStream("0xAA97DDBD_with_aaa_sig.asc"))) {
			pgp.importKeys(in);
		}

		// check for 0 *additional* signatures
		assertThat(pgp.getCertifications(pgpKey).size()).isEqualTo(4);


		try (IInputStream in = tempFile.createInputStream();) {
			pgp.importKeys(in);
		}

		// check for 0 *additional* signatures
		assertThat(pgp.getCertifications(pgpKey).size()).isEqualTo(4);
	}

	@Test
	public void signAndVerifyViaPgpSignable() {
		CreateRepositoryRequestDto dto = new CreateRepositoryRequestDto();
		dto.setServerRepositoryId(UUID.randomUUID());

		PgpKeyId pgpKeyId = new PgpKeyId("70c642ca41cd4390");
		PgpSignableSigner signer = new PgpSignableSigner(pgp, pgp.getPgpKey(pgpKeyId));
		signer.sign(dto);

		PgpSignableVerifier verifier = new PgpSignableVerifier(pgp);
		PgpSignature pgpSignature = verifier.verify(dto);
		assertThat(pgpSignature).isNotNull();

		PgpKey signaturePgpKey = pgp.getPgpKey(pgpSignature.getPgpKeyId());
		assertThat(signaturePgpKey).isNotNull();
		signaturePgpKey	= signaturePgpKey.getMasterKey();

		assertThat(signaturePgpKey.getPgpKeyId()).isEqualTo(pgpKeyId);

		verifier.verify(dto);

		dto.setServerRepositoryId(UUID.randomUUID());

		try {
			verifier.verify(dto);
			fail("PgpSignableVerifier did not detect modification!");
		} catch (SignatureException x) {
			doNothing(); // expected
		}
	}

	private byte[] createTestData() {
		byte[] data = new byte[random.nextInt(10 * 1024 * 1024) + 1];
		random.nextBytes(data);
		return data;
	}

	private static void initPgp() throws IOException {
		PgpRegistry.getInstance().clearCache();

		final File gnuPgDir = GnuPgDir.getInstance().getFile();

		gnuPgDir.mkdir();
		IOUtil.copyResource(GnuPgTest.class, GnuPgTest.PUBRING_FILE_NAME, createFile(gnuPgDir, GnuPgTest.PUBRING_FILE_NAME));
		IOUtil.copyResource(GnuPgTest.class, GnuPgTest.SECRING_FILE_NAME, createFile(gnuPgDir, GnuPgTest.SECRING_FILE_NAME));

		PgpRegistry.getInstance().setPgpAuthenticationCallback(new PgpAuthenticationCallback() {
			@Override
			public char[] getPassphrase(final PgpKey pgpKey) {
				return "test12345".toCharArray();
			}
		});
	}
}
