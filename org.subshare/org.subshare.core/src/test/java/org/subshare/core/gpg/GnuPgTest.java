package org.subshare.core.gpg;

import static co.codewizards.cloudstore.core.util.HashUtil.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;
import static org.assertj.core.api.Assertions.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGInputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.PGPKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.util.io.Streams;
import org.junit.Test;

public class GnuPgTest {
	private static final Random random = new Random();

	@Test
	public void readPubringGpg() throws Exception {
		PGPPublicKeyRingCollection pgpPublicKeyRingCollection;
		try (InputStream in = new BufferedInputStream(GnuPgTest.class.getResourceAsStream("pubring.gpg"));) {
			pgpPublicKeyRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(in));
		}
		for (final Iterator<?> it1 = pgpPublicKeyRingCollection.getKeyRings(); it1.hasNext(); ) {
			final PGPPublicKeyRing keyRing = (PGPPublicKeyRing) it1.next();
			for (final Iterator<?> it2 = keyRing.getPublicKeys(); it2.hasNext(); ) {
				final PGPPublicKey publicKey = (PGPPublicKey) it2.next();
				printPublicKey(publicKey);
			}
			System.out.println();
		}
	}

	@Test
	public void readSecringGpg() throws Exception {
		PGPSecretKeyRingCollection pgpSecretKeyRingCollection;
		try (InputStream in = new BufferedInputStream(GnuPgTest.class.getResourceAsStream("secring.gpg"));) {
			pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(in));
		}
		for (final Iterator<?> it1 = pgpSecretKeyRingCollection.getKeyRings(); it1.hasNext(); ) {
			final PGPSecretKeyRing keyRing = (PGPSecretKeyRing) it1.next();
			for (final Iterator<?> it2 = keyRing.getPublicKeys(); it2.hasNext(); ) {
				final PGPPublicKey publicKey = (PGPPublicKey) it2.next();
				printPublicKey(publicKey);
			}

			for (final Iterator<?> it3 = keyRing.getSecretKeys(); it3.hasNext(); ) {
				final PGPSecretKey secretKey = (PGPSecretKey) it3.next();
				printSecretKey(secretKey);
			}
			System.out.println();
		}
	}

	@Test
	public void encryptAndDecrypt() throws Exception {
		final PGPDataEncryptorBuilder encryptorBuilder = new BcPGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.TWOFISH);
		final PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(encryptorBuilder);
		final PGPKeyEncryptionMethodGenerator keyEncryptionMethodGenerator = new BcPublicKeyKeyEncryptionMethodGenerator(
				getPgpPublicKeyOrFail(bytesToLong(decodeHexStr("d7a92a24aa97ddbd"))));

		encryptedDataGenerator.addMethod(keyEncryptionMethodGenerator);

		final byte[] plain = new byte[1 + random.nextInt(1024 * 1024)];
		random.nextBytes(plain);

		final File encryptedFile = File.createTempFile("encrypted_", ".tmp");
		try (final OutputStream encryptedOut = new FileOutputStream(encryptedFile);) {
			try (final OutputStream plainOut = encryptedDataGenerator.open(encryptedOut, new byte[1024 * 16]);) {
				plainOut.write(plain);
			}
		}

		final byte[] decrypted;
		try (InputStream in = new FileInputStream(encryptedFile)) {
			final PGPEncryptedDataList encryptedDataList = new PGPEncryptedDataList(new BCPGInputStream(in));
			final Iterator<?> encryptedDataObjects = encryptedDataList.getEncryptedDataObjects();
			assertThat(encryptedDataObjects.hasNext()).isTrue();
			final PGPPublicKeyEncryptedData encryptedData = (PGPPublicKeyEncryptedData) encryptedDataObjects.next();
			assertThat(encryptedDataObjects.hasNext()).isFalse();

			final PublicKeyDataDecryptorFactory dataDecryptorFactory = new BcPublicKeyDataDecryptorFactory(
					getPgpPrivateKeyOrFail(encryptedData.getKeyID(), "test12345"));

			try (InputStream plainIn = encryptedData.getDataStream(dataDecryptorFactory);) {
				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				transferStreamData(plainIn, out);
				decrypted = out.toByteArray();
			}
		}

		assertThat(decrypted).isEqualTo(plain);

		encryptedFile.delete(); // delete it, if this test did not fail
	}

	@Test
	public void encryptSignAndDecryptVerify() throws Exception {

		final byte[] plain = new byte[1 + random.nextInt(1024 * 1024)];
		random.nextBytes(plain);

		// armor means whether to use base64-encoding and thus make it transferable in pure ASCII.
		final boolean armor = false;

		// this seems to be an additional integrity check - it's not the signature.
		final boolean withIntegrityCheck = true;

		final File encryptedFile = File.createTempFile("encrypted_", ".tmp");
		try (final OutputStream encryptedOut = new FileOutputStream(encryptedFile);) {
			encryptAndSign(new ByteArrayInputStream(plain), "dummy", encryptedOut,
					getPgpPublicKeyOrFail(bytesToLong(decodeHexStr("d7a92a24aa97ddbd"))),
					getPgpSecretKeyOrFail(bytesToLong(decodeHexStr("70c642ca41cd4390"))),
					armor, withIntegrityCheck,
					"test12345".toCharArray());
		}

		final byte[] decrypted;
		try (InputStream in = new FileInputStream(encryptedFile)) {
			try (InputStream pubKeyIn = new BufferedInputStream(GnuPgTest.class.getResourceAsStream("pubring.gpg"));) {
				try (InputStream secKeyIn = new BufferedInputStream(GnuPgTest.class.getResourceAsStream("secring.gpg"));) {
					final ByteArrayOutputStream decryptedOut = new ByteArrayOutputStream();
					decryptAndVerify(in, secKeyIn, "test12345".toCharArray(), decryptedOut, pubKeyIn);
					decrypted = decryptedOut.toByteArray();
				}
			}
		}

		assertThat(decrypted).isEqualTo(plain);

		encryptedFile.delete(); // delete it, if this test did not fail
	}

	private static void encryptAndSign(final InputStream in, final String fileName, OutputStream out, final PGPPublicKey encKey, final PGPSecretKey pgpSec, final boolean armor, final boolean withIntegrityCheck, final char[] pass) throws IOException, PGPException, SignatureException {
		final int BUFFER_SIZE = 1024 * 64;
	    if (armor) {
	        out = new ArmoredOutputStream(out);
	    }

	    final PGPEncryptedDataGenerator encGen =
	    		new PGPEncryptedDataGenerator(
	    				new BcPGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.TWOFISH).setWithIntegrityPacket(withIntegrityCheck).setSecureRandom(
	    						new SecureRandom()));
	    encGen.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(encKey));
	    final OutputStream encryptedOut = encGen.open(out, new byte[BUFFER_SIZE]);

	    final PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(CompressionAlgorithmTags.ZIP);
	    final OutputStream compressedData = comData.open(encryptedOut);

	    final PGPPrivateKey pgpPrivKey = pgpSec.extractPrivateKey(
	    		new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(pass));
	    final PGPSignatureGenerator sGen = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(
	    		pgpSec.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1));
	    sGen.init(PGPSignature.BINARY_DOCUMENT, pgpPrivKey);
	    final Iterator<?> it = pgpSec.getPublicKey().getUserIDs();
	    if (it.hasNext()) {
	    	final PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
	    	spGen.setSignerUserID(false, (String) it.next());
	    	sGen.setHashedSubpackets(spGen.generate());
	    }

	    sGen.generateOnePassVersion(false).encode(compressedData);

	    final PGPLiteralDataGenerator lGen = new PGPLiteralDataGenerator();
	    final OutputStream lOut = lGen.open(compressedData, PGPLiteralData.BINARY, fileName, new Date(), new byte[BUFFER_SIZE]);

	    int ch;

	    while ((ch = in.read()) >= 0) {
	    	lOut.write(ch);
	    	sGen.update((byte) ch);
	    }

	    lOut.close();
	    lGen.close();

	    sGen.generate().encode(compressedData);


	    comData.close();
	    compressedData.close();

	    encryptedOut.close();
	    encGen.close();

	    if (armor) {
	    	out.close();
	    }
	}

	public static void decryptAndVerify(InputStream in, final InputStream keyIn, final char[] passwd, final OutputStream fOut, final InputStream publicKeyIn) throws IOException, NoSuchProviderException, SignatureException, PGPException {
		in = PGPUtil.getDecoderStream(in);

		final PGPObjectFactory pgpF = new PGPObjectFactory(in);
		PGPEncryptedDataList enc;

		final Object o = pgpF.nextObject();
		//
		// the first object might be a PGP marker packet.
		//
		if (o instanceof PGPEncryptedDataList) {
			enc = (PGPEncryptedDataList) o;
		} else {
			enc = (PGPEncryptedDataList) pgpF.nextObject();
		}

		//
		// find the secret key
		//
		final Iterator<?> it = enc.getEncryptedDataObjects();
		PGPPrivateKey sKey = null;
		PGPPublicKeyEncryptedData pbe = null;

		while (sKey == null && it.hasNext()) {
			pbe = (PGPPublicKeyEncryptedData) it.next();
			sKey = findSecretKey(keyIn, pbe.getKeyID(), passwd);
		}

		if (sKey == null) {
			throw new IllegalArgumentException("secret key for message not found.");
		}

		final PublicKeyDataDecryptorFactory dataDecryptorFactory = new BcPublicKeyDataDecryptorFactory(
				getPgpPrivateKeyOrFail(pbe.getKeyID(), "test12345"));

		final InputStream clear = pbe.getDataStream(dataDecryptorFactory);

		PGPObjectFactory plainFact = new PGPObjectFactory(clear);

		Object message = null;

		PGPOnePassSignatureList onePassSignatureList = null;
		PGPSignatureList signatureList = null;
		PGPCompressedData compressedData = null;

		message = plainFact.nextObject();
		final ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();

		while (message != null) {
			if (message instanceof PGPCompressedData) {
				compressedData = (PGPCompressedData) message;
				plainFact = new PGPObjectFactory(compressedData.getDataStream());
				message = plainFact.nextObject();
			}

			if (message instanceof PGPLiteralData) {
				// have to read it and keep it somewhere.
				Streams.pipeAll(((PGPLiteralData) message).getInputStream(), actualOutput);
			} else if (message instanceof PGPOnePassSignatureList) {
				onePassSignatureList = (PGPOnePassSignatureList) message;
			} else if (message instanceof PGPSignatureList) {
				signatureList = (PGPSignatureList) message;
			} else {
				throw new PGPException("message unknown message type.");
			}
			message = plainFact.nextObject();
		}
		actualOutput.close();
		PGPPublicKey publicKey = null;
		final byte[] output = actualOutput.toByteArray();
		if (onePassSignatureList == null || signatureList == null) {
			throw new PGPException("Poor PGP. Signatures not found.");
		} else {

			for (int i = 0; i < onePassSignatureList.size(); i++) {
				final PGPOnePassSignature ops = onePassSignatureList.get(0);
				final PGPPublicKeyRingCollection pgpRing = new PGPPublicKeyRingCollection(
						PGPUtil.getDecoderStream(publicKeyIn));
				publicKey = pgpRing.getPublicKey(ops.getKeyID());
				if (publicKey != null) {
					ops.init(new BcPGPContentVerifierBuilderProvider(), publicKey);
					ops.update(output);
					final PGPSignature signature = signatureList.get(i);
					if (ops.verify(signature)) {
						final Iterator<?> userIds = publicKey.getUserIDs();
						while (userIds.hasNext()) {
							final String userId = (String) userIds.next();
						}
					} else {
						throw new SignatureException("Signature verification failed");
					}
				}
			}

		}

		if (pbe.isIntegrityProtected() && !pbe.verify()) {
			throw new PGPException("Data is integrity protected but integrity is lost.");
		} else if (publicKey == null) {
			throw new SignatureException("Signature not found");
		} else {
			fOut.write(output);
			fOut.flush();
			fOut.close();
		}
	}

	public static PGPPrivateKey findSecretKey(final InputStream keyIn, final long keyID, final char[] pass) throws IOException, PGPException {
	    final PGPSecretKeyRingCollection pgpSec = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(keyIn));
	    final PGPSecretKey pgpSecKey = pgpSec.getSecretKey(keyID);
	    if (pgpSecKey == null) return null;

	    final PBESecretKeyDecryptor decryptor = new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(pass);
	    return pgpSecKey.extractPrivateKey(decryptor);
	}

	private static PGPPublicKey getPgpPublicKeyOrFail(final long keyId) throws IOException, PGPException {
		PGPPublicKeyRingCollection pgpPublicKeyRingCollection;
		try (InputStream in = new BufferedInputStream(GnuPgTest.class.getResourceAsStream("pubring.gpg"));) {
			pgpPublicKeyRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(in));
		}
		final PGPPublicKey publicKey = pgpPublicKeyRingCollection.getPublicKey(keyId);
		if (publicKey == null)
			throw new IllegalArgumentException("No key with this keyId found: " + encodeHexStr(longToBytes(keyId)));

		return publicKey;
	}

	private static PGPPrivateKey getPgpPrivateKeyOrFail(final long keyId, final String passphrase) throws IOException, PGPException {
		final PGPSecretKey secretKey = getPgpSecretKeyOrFail(keyId);

		final PGPDigestCalculatorProvider calculatorProvider = new BcPGPDigestCalculatorProvider();
		final BcPBESecretKeyDecryptorBuilder secretKeyDecryptorBuilder = new BcPBESecretKeyDecryptorBuilder(calculatorProvider);
		final PBESecretKeyDecryptor secretKeyDecryptor = secretKeyDecryptorBuilder.build(passphrase.toCharArray());
		final PGPPrivateKey privateKey = secretKey.extractPrivateKey(secretKeyDecryptor);
		return privateKey;
	}

	private static PGPSecretKey getPgpSecretKeyOrFail(final long keyId) throws IOException, PGPException {
		PGPSecretKeyRingCollection pgpSecretKeyRingCollection;
		try (InputStream in = new BufferedInputStream(GnuPgTest.class.getResourceAsStream("secring.gpg"));) {
			pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(in));
		}
		final PGPSecretKey secretKey = pgpSecretKeyRingCollection.getSecretKey(keyId);
		if (secretKey == null)
			throw new IllegalArgumentException("No key with this keyId found: " + encodeHexStr(longToBytes(keyId)));

		return secretKey;
	}

	private static byte[] longToBytes(final long l) {
		final byte[] bytes = new byte[8];
		for (int i = 0; i < 8; ++i)
			bytes[i] = (byte) (l >>> (8 * (7 - i)));

		return bytes;
	}

	private static long bytesToLong(final byte[] bytes) {
		long l = 0;
		for (int i = 0; i < 8; ++i)
			l |= ((long) (bytes[i] & 0xff)) << (8 * (7 - i));

		return l;
	}

	@Test
	public void bytesToLongToBytes() {
		final byte[] bytes = longToBytes(Long.MAX_VALUE);
		long l = bytesToLong(bytes);
		assertThat(l).isEqualTo(Long.MAX_VALUE);

		for (int i = 0; i < 100; ++i) {
			random.nextBytes(bytes);
			l = bytesToLong(bytes);
			final byte[] bytes2 = longToBytes(l);
			assertThat(bytes2).isEqualTo(bytes);
		}
	}

	private void printPublicKey(final PGPPublicKey publicKey) {
		final byte[] fingerprint = publicKey.getFingerprint();

		System.out.println(">>> pub >>>");
		System.out.println("keyID: " + encodeHexStr(longToBytes(publicKey.getKeyID())));
		System.out.println("fingerprint: " + (fingerprint == null ? "" : formatEncodedHexStrForHuman(encodeHexStr(fingerprint))));
		System.out.println("masterKey: " + publicKey.isMasterKey());
		System.out.println("encryptionKey: " + publicKey.isEncryptionKey());

		for (final Iterator<?> it3 = publicKey.getUserIDs(); it3.hasNext(); ) {
			final String userId = (String) it3.next();
			System.out.println("userID: " + userId);
		}

		for (final Iterator<?> it4 = publicKey.getSignatures(); it4.hasNext(); ) {
			final PGPSignature signature = (PGPSignature) it4.next();
			System.out.println("signature.keyID: " + encodeHexStr(longToBytes(signature.getKeyID())));
		}
		System.out.println("<<< pub <<<");
	}

	private void printSecretKey(final PGPSecretKey secretKey) {
		System.out.println(">>> sec >>>");
		System.out.println("keyID: " + encodeHexStr(longToBytes(secretKey.getKeyID())));
		for (final Iterator<?> it1 = secretKey.getUserIDs(); it1.hasNext(); ) {
			final String userId = (String) it1.next();
			System.out.println("userID: " + userId);
		}
		System.out.println("<<< sec <<<");
	}


	private static File getUserHome()
	{
		final String userHome = System.getProperty("user.home"); //$NON-NLS-1$
		if (userHome == null)
			throw new IllegalStateException("System property user.home is not set! This should never happen!"); //$NON-NLS-1$

		return new File(userHome);
	}
}
