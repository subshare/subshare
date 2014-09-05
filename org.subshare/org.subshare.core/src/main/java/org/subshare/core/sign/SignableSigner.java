package org.subshare.core.sign;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import static org.subshare.core.crypto.CryptoConfigUtil.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Signer;
import org.subshare.core.user.UserRepoKey;
import org.subshare.crypto.CryptoRegistry;

public class SignableSigner {

	private static final int BUFFER_SIZE = 32 * 1024;
	private static final int MAX_UNSIGNED_2_BYTE_VALUE = 0xffff;

	private final UserRepoKey userRepoKey;
	private final SignerTransformation signerTransformation;
	private final Signer signer;

	public SignableSigner(final UserRepoKey userRepoKey) {
		this.userRepoKey = assertNotNull("userRepoKey", userRepoKey);
		this.signerTransformation = getSignerTransformation();

		try {
			signer = CryptoRegistry.getInstance().createSigner(signerTransformation.getTransformation());
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		signer.init(true, userRepoKey.getKeyPair().getPrivate());
	}

	public void sign(final Signable signable) {
		assertNotNull("signable", signable).setSigningUserRepoKeyId(userRepoKey.getUserRepoKeyId());

		final int signedDataVersion = signable.getSignedDataVersion();
		if (signedDataVersion > MAX_UNSIGNED_2_BYTE_VALUE)
			throw new IllegalStateException("signedDataVersion > " + MAX_UNSIGNED_2_BYTE_VALUE);

		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.write(1); // version

			final int signerTransformationNumeric = signerTransformation.ordinal();
			if (signerTransformationNumeric > MAX_UNSIGNED_2_BYTE_VALUE)
				throw new IllegalStateException("signerTransformationNumeric > " + MAX_UNSIGNED_2_BYTE_VALUE);

			out.write(signerTransformationNumeric);
			out.write(signerTransformationNumeric >>> 8);

			out.write(signedDataVersion);
			out.write(signedDataVersion >>> 8);

			signer.reset();
			final byte[] buf = new byte[BUFFER_SIZE];
			int bytesRead;
			try (InputStream signedDataInputStream = signable.getSignedData(signedDataVersion);) {
				while ((bytesRead = signedDataInputStream.read(buf)) >= 0) {
					if (bytesRead > 0)
						signer.update(buf, 0, bytesRead);
				}
			}
			final byte[] signature = signer.generateSignature();

			final int signatureLength = signature.length;
			out.write(signatureLength);
			out.write(signatureLength >>> 8);
			out.write(signatureLength >>> 16);
			out.write(signatureLength >>> 24);
			out.write(signature);

			signable.setSignatureData(out.toByteArray());
		} catch (IOException | CryptoException x) {
			throw new RuntimeException(x);
		}
	}
}
