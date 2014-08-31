package org.subshare.core.pgp.gnupg;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.util.io.Streams;
import org.subshare.core.pgp.AbstractPgpDecoder;

import co.codewizards.cloudstore.core.auth.SignatureException;

public class BcPgpDecoder extends AbstractPgpDecoder {

	private final BcWithLocalGnuPgPgp pgp;

	public BcPgpDecoder(final BcWithLocalGnuPgPgp pgp) {
		this.pgp = assertNotNull("pgp", pgp);
	}

	@Override
	public void decode() throws SignatureException, IOException {
		setSignPgpKey(null);
		try {
			final InputStream in = PGPUtil.getDecoderStream(getInputStreamOrFail());
			final OutputStream out = getOutputStreamOrFail();

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

			char[] passphrase;
			while (sKey == null && it.hasNext()) {
				pbe = (PGPPublicKeyEncryptedData) it.next();
				final BcPgpKey bcPgpKey = pgp.getBcPgpKey(pbe.getKeyID());
				if (bcPgpKey != null) {
					PGPSecretKey secretKey = bcPgpKey.getSecretKey();
					if (secretKey != null && ! secretKey.isPrivateKeyEmpty()) {
						if (secretKey.getKeyEncryptionAlgorithm() != SymmetricKeyAlgorithmTags.NULL) {
							passphrase = getPgpAuthenticationCallbackOrFail().getPassphrase(bcPgpKey.getPgpKey());
							if (passphrase == null) // maybe user cancelled - try next
								secretKey = null;
						}
						else
							passphrase = null;

						if (secretKey != null)
							sKey = secretKey.extractPrivateKey(
									new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(passphrase));
					}
				}
			}

			if (sKey == null) {
				throw new IllegalArgumentException("secret key for message not found.");
			}

			final PublicKeyDataDecryptorFactory dataDecryptorFactory = new BcPublicKeyDataDecryptorFactory(sKey);

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
					final PGPOnePassSignature ops = onePassSignatureList.get(i);
					final BcPgpKey bcPgpKey = pgp.getBcPgpKey(ops.getKeyID());
					if (bcPgpKey != null) {
						publicKey = bcPgpKey.getPublicKey();
						ops.init(new BcPGPContentVerifierBuilderProvider(), publicKey);
						ops.update(output);
						final PGPSignature signature = signatureList.get(i);
						if (ops.verify(signature)) {
							setSignPgpKey(bcPgpKey.getPgpKey());
						} else {
							throw new SignatureException("Signature verification failed!");
						}
					}
				}

			}

			if (pbe.isIntegrityProtected() && !pbe.verify()) {
				throw new PGPException("Data is integrity protected but integrity is lost.");
			} else if (publicKey == null) {
				throw new SignatureException("Signature not found");
			} else {
				out.write(output);
			}
		} catch (final java.security.SignatureException x) {
			throw new SignatureException(x);
		} catch (final PGPException x) {
			throw new IOException(x);
		}
	}

}
