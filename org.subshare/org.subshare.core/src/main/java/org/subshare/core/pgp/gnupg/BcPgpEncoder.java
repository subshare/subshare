package org.subshare.core.pgp.gnupg;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;
import org.subshare.core.pgp.AbstractPgpEncoder;
import org.subshare.core.pgp.PgpAuthenticationCallback;
import org.subshare.core.pgp.PgpKey;

public class BcPgpEncoder extends AbstractPgpEncoder {

	private final BcWithLocalGnuPgPgp pgp;

	public BcPgpEncoder(final BcWithLocalGnuPgPgp pgp) {
		this.pgp = assertNotNull("pgp", pgp);
	}

	@Override
	public void encode() throws IOException {
		final int BUFFER_SIZE = 1024 * 32;

		final InputStream in = getInputStreamOrFail();
		OutputStream out = getOutputStreamOrFail();
		try {
			final PGPEncryptedDataGenerator edGenerator = getEncryptPgpKeys().isEmpty() ? null : createEncryptedDataGenerator();
			try {
				for (final PgpKey encryptPgpKey : getEncryptPgpKeys()) {
					final BcPgpKey bcPgpKey = pgp.getBcPgpKey(encryptPgpKey);
					edGenerator.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(bcPgpKey.getPublicKey()));
				}

				final OutputStream encryptedOut = edGenerator == null ? null : edGenerator.open(out, new byte[BUFFER_SIZE]);
				try {
					if (encryptedOut != null)
						out = encryptedOut;

					final PGPCompressedDataGenerator cdGenerator = createCompressedDataGenerator();
					try {
						final OutputStream compressedOut = cdGenerator.open(out);
						try {
							out = compressedOut;

							final PGPSignatureGenerator signatureGenerator = getSignPgpKey() == null ? null : createSignatureGenerator();

							if (signatureGenerator != null)
								signatureGenerator.generateOnePassVersion(false).encode(out);

							final PGPLiteralDataGenerator ldGenerator = new PGPLiteralDataGenerator();
							try {
								try (final OutputStream lOut = ldGenerator.open(out, PGPLiteralData.BINARY, getFileName(), new Date(), new byte[BUFFER_SIZE]);) {
									int bytesRead;
									final byte[] buf = new byte[BUFFER_SIZE];
									while ((bytesRead = in.read(buf, 0, buf.length)) >= 0) {
										if (bytesRead > 0) {
											lOut.write(buf, 0, bytesRead);

											if (signatureGenerator != null)
												signatureGenerator.update(buf, 0, bytesRead);
										}
									}
								}
							} finally {
								ldGenerator.close();
							}

							if (signatureGenerator != null)
								signatureGenerator.generate().encode(out);
						} finally {
							compressedOut.close();
						}
					} finally {
						cdGenerator.close();
					}
				} finally {
					if (encryptedOut != null)
						encryptedOut.close();
				}
			} finally {
				if (edGenerator != null)
					edGenerator.close();
			}
		} catch (final PGPException | SignatureException x) {
			throw new IOException(x);
		}
	}

	private PGPSignatureGenerator createSignatureGenerator() throws PGPException {
		final PgpKey signPgpKey = assertNotNull("signPgpKey", getSignPgpKey());
		final PGPSecretKey signSecretKey = getPgpSecretKeyOrFail(signPgpKey);

		final char[] signPassphrase = getPassphrase(signPgpKey);
		final PGPPrivateKey pgpPrivKey = signSecretKey.extractPrivateKey(
				new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(signPassphrase));

		final PGPSignatureGenerator signatureGenerator;
		signatureGenerator = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(
				signSecretKey.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1));

		signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, pgpPrivKey);
		final Iterator<?> it = signSecretKey.getPublicKey().getUserIDs();
		if (it.hasNext()) {
			final PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
			spGen.setSignerUserID(false, (String) it.next());
			signatureGenerator.setHashedSubpackets(spGen.generate());
		}
		return signatureGenerator;
	}

	private PGPEncryptedDataGenerator createEncryptedDataGenerator() {
		return new PGPEncryptedDataGenerator(
				new BcPGPDataEncryptorBuilder(getSymmetricEncryptionAlgorithm().getSymmetricKeyAlgorithmTag())
				.setWithIntegrityPacket(isWithIntegrityCheck())
				.setSecureRandom(new SecureRandom()));
	}

	private PGPCompressedDataGenerator createCompressedDataGenerator() {
		return new PGPCompressedDataGenerator(getCompressionAlgorithm().getCompressionAlgorithmTag());
	}

	private char[] getPassphrase(final PgpKey pgpKey) {
		final PGPSecretKey secretKey = getPgpSecretKeyOrFail(pgpKey);
		if (secretKey.getKeyEncryptionAlgorithm() != SymmetricKeyAlgorithmTags.NULL) {
			final PgpAuthenticationCallback callback = getPgpAuthenticationCallbackOrFail();
			return callback.getPassphrase(pgpKey);
		}
		return null;
	}

	private PGPSecretKey getPgpSecretKeyOrFail(final PgpKey pgpKey) {
		assertNotNull("pgpKey", pgpKey);
		final PGPSecretKey secretKey = pgp.getBcPgpKeyOrFail(pgpKey).getSecretKey();
		if (secretKey == null)
			throw new IllegalStateException(String.format(
					"The PGP key %s does not have a secret key attached (it is a public key only)!",
					Long.toHexString(pgpKey.getPgpKeyId())));

		return secretKey;
	}
}
