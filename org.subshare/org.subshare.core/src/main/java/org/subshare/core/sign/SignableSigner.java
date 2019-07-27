package org.subshare.core.sign;

import static co.codewizards.cloudstore.core.util.IOUtil.*;
import static co.codewizards.cloudstore.core.util.StringUtil.*;
import static java.util.Objects.*;
import static org.subshare.core.crypto.CryptoConfigUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Signer;
import org.subshare.core.dto.SignatureDto;
import org.subshare.core.user.UserRepoKey;
import org.subshare.crypto.CryptoRegistry;

import co.codewizards.cloudstore.core.io.ByteArrayOutputStream;

public class SignableSigner {

	private static final int BUFFER_SIZE = 32 * 1024;
	static final int MAX_UNSIGNED_2_BYTE_VALUE = 0xffff;

	private final UserRepoKey userRepoKey;
	private final SignerTransformation signerTransformation;
	private final Signer signer;

	public SignableSigner(final UserRepoKey userRepoKey) {
		this.userRepoKey = requireNonNull(userRepoKey, "userRepoKey");
		this.signerTransformation = getSignerTransformation();

		try {
			signer = CryptoRegistry.getInstance().createSigner(signerTransformation.getTransformation());
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		signer.init(true, userRepoKey.getKeyPair().getPrivate());
	}

	public void sign(final Signable signable) {
		requireNonNull(signable, "signable");

		final Date signatureCreated = new Date();
		final SignatureDto signatureDto = new SignatureDto();
		signatureDto.setSignatureCreated(signatureCreated);
		signatureDto.setSigningUserRepoKeyId(userRepoKey.getUserRepoKeyId());

		final String signedDataType = signable.getSignedDataType();
		if (isEmpty(signedDataType))
			throw new IllegalArgumentException(String.format("Implementation error in class %s: signable.getSignedDataType() returned null! %s", signable.getClass().getName(), signable));

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

			final byte[] signedDataTypeBytes = signedDataType.getBytes(StandardCharsets.UTF_8);
			signer.update(signedDataTypeBytes, 0, signedDataTypeBytes.length);

			final byte[] signatureCreatedBytes = longToBytes(signatureCreated.getTime());
			signer.update(signatureCreatedBytes, 0, signatureCreatedBytes.length);

			final byte[] buf = new byte[BUFFER_SIZE];
			int bytesRead;
			try (InputStream signedDataInputStream = signable.getSignedData(signedDataVersion);) {
				while ((bytesRead = signedDataInputStream.read(buf)) >= 0) {
					if (bytesRead > 0)
						signer.update(buf, 0, bytesRead);
				}
			}

			final byte[] signatureBytes = signer.generateSignature();

			final int signatureBytesLength = signatureBytes.length;
			out.write(signatureBytesLength);
			out.write(signatureBytesLength >>> 8);
			out.write(signatureBytesLength >>> 16);
			out.write(signatureBytesLength >>> 24);
			out.write(signatureBytes);

			signatureDto.setSignatureData(out.toByteArray());
			signable.setSignature(signatureDto);
		} catch (IOException | CryptoException x) {
			throw new RuntimeException(x);
		}
	}
}
