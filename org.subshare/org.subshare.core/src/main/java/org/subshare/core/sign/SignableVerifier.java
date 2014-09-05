package org.subshare.core.sign;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static co.codewizards.cloudstore.core.util.IOUtil.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.crypto.Signer;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKeyPublicKeyLookup;
import org.subshare.crypto.CryptoRegistry;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.Uid;

public class SignableVerifier {

	private static final int BUFFER_SIZE = 32 * 1024;
	private final UserRepoKeyPublicKeyLookup lookup;
	private final Map<SignerTransformation, Signer> signerTransformation2Signer = new HashMap<SignerTransformation, Signer>(2);

	public SignableVerifier(final UserRepoKeyPublicKeyLookup lookup) {
		this.lookup = assertNotNull("lookup", lookup);
	}

	public void verify(final Signable signable) throws SignatureException {
		final Uid signingUserRepoKeyId = assertNotNull("signable", signable).getSigningUserRepoKeyId();
		if (signingUserRepoKeyId == null)
			throw new SignatureException("There is no signature! signingUserRepoKeyId == null");

		final byte[] signatureData = signable.getSignatureData();
		if (signatureData == null)
			throw new SignatureException("There is no signature! signatureData == null");

		if (signatureData.length < 3)
			throw new SignatureException("There is no signature! signatureData.length < 3");

		final UserRepoKey.PublicKey userRepoKeyPublicKey = lookup.getUserRepoKeyPublicKey(signingUserRepoKeyId);
		if (userRepoKeyPublicKey == null)
			throw new SignatureException(String.format("No public key found for signingUserRepoKeyId=%s!", signingUserRepoKeyId));

		try {
			final ByteArrayInputStream in = new ByteArrayInputStream(signatureData);
			final int version = in.read();
			if (version != 1)
				throw new SignatureException(String.format("signatureData has unsupported version=%s!", version));

			final int signerTransformationNumeric = readOrFail(in) + (readOrFail(in) << 8);
			if (signerTransformationNumeric > SignerTransformation.values().length) {
				throw new IOException(String.format(
						"signerTransformationNumeric > SignerTransformation.values().length :: %s > %s",
						signerTransformationNumeric, SignerTransformation.values().length));
			}
			final SignerTransformation signerTransformation = SignerTransformation.values()[signerTransformationNumeric];

			final int signedDataVersion = readOrFail(in) + (readOrFail(in) << 8);

			final int signatureLength = readOrFail(in) + (readOrFail(in) << 8) + (readOrFail(in) << 16) + (readOrFail(in) << 24);
			final byte[] signature = new byte[signatureLength];
			readOrFail(in, signature, 0, signatureLength);

			final Signer signer = getSigner(signerTransformation);
			signer.reset();
			final byte[] buf = new byte[BUFFER_SIZE];
			int bytesRead;
			try (InputStream signedDataInputStream = signable.getSignedData(signedDataVersion);) {
				while ((bytesRead = signedDataInputStream.read(buf)) >= 0) {
					if (bytesRead > 0)
						signer.update(buf, 0, bytesRead);
				}
			}
			if (!signer.verifySignature(signature))
				throw new SignatureException("Signature not valid!");
		} catch (final IOException x) {
			throw new SignatureException(x);
		}
	}

	private Signer getSigner(final SignerTransformation signerTransformation) {
		assertNotNull("signerTransformation", signerTransformation);
		Signer signer = signerTransformation2Signer.get(signerTransformation);
		if (signer == null) {
			try {
				signer = CryptoRegistry.getInstance().createSigner(signerTransformation.getTransformation());
			} catch (final NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
			signerTransformation2Signer.put(signerTransformation, signer);
		}
		return signer;
	}
}
