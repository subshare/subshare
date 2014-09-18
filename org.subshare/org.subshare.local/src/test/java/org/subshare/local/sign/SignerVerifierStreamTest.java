package org.subshare.local.sign;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.subshare.core.sign.SignerOutputStream;
import org.subshare.core.sign.SignerTransformation;
import org.subshare.core.sign.VerifierInputStream;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.core.user.UserRepoKeyPublicKeyLookup;
import org.subshare.core.user.UserRepoKeyRing;
import org.subshare.crypto.CryptoRegistry;
import org.subshare.local.AbstractTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import co.codewizards.cloudstore.core.auth.SignatureException;
import co.codewizards.cloudstore.core.dto.Uid;

public class SignerVerifierStreamTest extends AbstractTest {

	private static final Random random = new Random();

	private static UUID repositoryId;
	private static UserRepoKeyRing userRepoKeyRing;
	private static UserRepoKey userRepoKey;
	private static UserRepoKeyPublicKeyLookup userRepoKeyPublicKeyLookup;
	private static final Date signatureCreated = new Date(0); // TODO current date!

	@BeforeClass
	public static void beforeClass() {
		repositoryId = UUID.randomUUID();
		userRepoKeyRing = createUserRepoKeyRing(repositoryId);
		userRepoKey = userRepoKeyRing.getUserRepoKeys(repositoryId).get(0);
		userRepoKeyPublicKeyLookup = new UserRepoKeyPublicKeyLookup() {
			@Override
			public PublicKey getUserRepoKeyPublicKey(final Uid userRepoKeyId) {
				if (userRepoKeyId.equals(userRepoKey.getUserRepoKeyId()))
					return userRepoKey.getPublicKey();

				return null;
			}
		};
	}

	@AfterClass
	public static void afterClass() {
		repositoryId = null;
		userRepoKeyRing = null;
		userRepoKey = null;
		userRepoKeyPublicKeyLookup = null;
	}

	@Test
	public void signAndVerifySimpleBlockRead() throws Exception {
		final byte[] plain = new byte[1024];
		for (int i = 0; i < plain.length; ++i)
			plain[i] = (byte) (i % 0xff);

		final byte[] signed;

		try (final ByteArrayOutputStream signedOut = new ByteArrayOutputStream();) {
			try (final SignerOutputStream signerOutputStream = new SignerOutputStream(signedOut, userRepoKey);) {
				signerOutputStream.write(plain);
			}
			signed = signedOut.toByteArray();
		}

		final byte[] plainVerified;

		try (ByteArrayOutputStream plainVerifiedOut = new ByteArrayOutputStream();) {
			try (VerifierInputStream in = new VerifierInputStream(new ByteArrayInputStream(signed), userRepoKeyPublicKeyLookup);) {
				while (true) {
					final byte[] buf = new byte[500];
					final int read = in.read(buf);
					if (read < 0)
						break;

					plainVerifiedOut.write(buf, 0, read);
				}
			}
			plainVerified = plainVerifiedOut.toByteArray();
		}

		assertThat(plainVerified).isEqualTo(plain);
	}

	@Test
	public void signAndVerifyRandom() throws Exception {
		final byte[][] plainAndSigned = createRandomSignedData(100);
		final byte[] plain = plainAndSigned[0];
		final byte[] signed = plainAndSigned[1];

		verify(plain, signed);
	}

	@Test
	public void sha1Digest() throws Exception {
		for (int l = 0; l < 10; ++l) {
			System.out.println("sha1Digest run #" + (l + 1));
			final SHA1Digest digest1 = new SHA1Digest();
			final SHA1Digest digest2 = new SHA1Digest();

			for (int i = 0; i < 100; ++i) {
				final byte[] buf = new byte[1 + random.nextInt(1024 * 1024)];
				random.nextBytes(buf);
				digest1.update(buf, 0, buf.length);
				for (int idx = 0; idx < buf.length; ++idx)
					digest2.update(buf[idx]);
			}
			final byte[] hash1 = new byte[digest1.getDigestSize()];
			digest1.doFinal(hash1, 0);

			final byte[] hash2 = new byte[digest2.getDigestSize()];
			digest2.doFinal(hash2, 0);

			assertThat(hash1).isEqualTo(hash2);
			System.out.println();
		}
	}

	@Test
	public void signRsaWithSha1() throws Exception {
		for (int l = 0; l < 5; ++l) {
			System.out.println("signRsaWithSha1 run #" + (l + 1));
			final Signer signer1 = CryptoRegistry.getInstance().createSigner(SignerTransformation.RSA_SHA1.getTransformation());
			final Signer signer2 = CryptoRegistry.getInstance().createSigner(SignerTransformation.RSA_SHA1.getTransformation());
			final Signer signer3 = CryptoRegistry.getInstance().createSigner(SignerTransformation.RSA_SHA1.getTransformation());

			signer1.init(true, userRepoKey.getKeyPair().getPrivate());
			signer2.init(true, userRepoKey.getKeyPair().getPrivate());
			signer3.init(true, userRepoKey.getKeyPair().getPrivate());

			ByteArrayOutputStream allOut = new ByteArrayOutputStream();
			for (int i = 0; i < 100; ++i) {
				final byte[] buf = new byte[1 + random.nextInt(1024 * 1024)];
				random.nextBytes(buf);
				allOut.write(buf);
				signer1.update(buf, 0, buf.length);
				for (int idx = 0; idx < buf.length; ++idx)
					signer2.update(buf[idx]);
			}
			final byte[] all = allOut.toByteArray(); allOut = null;
			final byte[] signature1 = signer1.generateSignature();
			final byte[] signature2 = signer2.generateSignature();

			signer3.update(all, 0, all.length);
			final byte[] signature3 = signer3.generateSignature();

			assertThat(signature1).isEqualTo(signature2);
			assertThat(signature3).isEqualTo(signature2);
			System.out.println();
		}
	}

	@Test
	public void signAndVerifyRandomManySmall() throws Exception {
		for (int i = 0; i < 100; ++i) {
			System.out.println("signAndVerifyRandomManySmall run #" + (i + 1));
			final byte[][] plainAndSigned = createRandomSignedData(1);
			final byte[] plain = plainAndSigned[0];
			final byte[] signed = plainAndSigned[1];

//			verifySimple(signed);
//
//			final Signer signer = CryptoRegistry.getInstance().createSigner(SignerTransformation.RSA_SHA1.getTransformation());
//			signer.init(true, userRepoKey.getKeyPair().getPrivate());
//			final byte[] signatureCreatedBytes = longToBytes(signatureCreated.getTime());
//			signer.update(signatureCreatedBytes, 0, signatureCreatedBytes.length);
//			signer.update(plain, 0, plain.length);
//			final byte[] signature = signer.generateSignature();
//			final byte[] extractedSignature = extractSignature(signed);
//			assertThat(signature).isEqualTo(extractedSignature);

			verify(plain, signed);
		}
	}

//	private void writeTempFile(final String fileNameSuffix, final byte[] data) throws IOException {
//		final File file = File.createTempFile(Long.toString(System.currentTimeMillis(), 36) + "_", "." + fileNameSuffix);
//		try (FileOutputStream out = new FileOutputStream(file);) {
//			out.write(data, 0, data.length);
//		}
//	}

//	private byte[] extractSignature(final byte[] signed) {
//		final byte[] signatureBytesLength = new byte[4];
//		System.arraycopy(signed, signed.length - signatureBytesLength.length, signatureBytesLength, 0, signatureBytesLength.length);
//		reverse(signatureBytesLength);
//
//		final byte[] signatureBytes = new byte[bytesToInt(signatureBytesLength)];
//		System.arraycopy(signed, signed.length - signatureBytes.length - signatureBytesLength.length, signatureBytes, 0, signatureBytes.length);
//		return signatureBytes;
//	}

//	private static final int signatureCreatedOffset = 19;
//
//	private boolean verifySimple(final byte[] signed) throws Exception {
//		final Signer signer = CryptoRegistry.getInstance().createSigner(SignerTransformation.RSA_SHA1.getTransformation());
//		signer.init(false, userRepoKey.getKeyPair().getPublic());
//
//		final byte[] signatureCreatedBytes = new byte[8];
//		System.arraycopy(signed, signatureCreatedOffset, signatureCreatedBytes, 0, signatureCreatedBytes.length);
//		reverse(signatureCreatedBytes);
//		signer.update(signatureCreatedBytes, 0, signatureCreatedBytes.length);
//
//		final byte[] signatureBytesLength = new byte[4];
//		System.arraycopy(signed, signed.length - signatureBytesLength.length, signatureBytesLength, 0, signatureBytesLength.length);
//		reverse(signatureBytesLength);
//
//		final byte[] signatureBytes = new byte[bytesToInt(signatureBytesLength)];
//		System.arraycopy(signed, signed.length - signatureBytes.length - signatureBytesLength.length, signatureBytes, 0, signatureBytes.length);
//
//		final int payloadOffset = signatureCreatedOffset + signatureCreatedBytes.length;
//		final int payloadLength = signed.length - payloadOffset - signatureBytes.length - signatureBytesLength.length;
//		signer.update(signed, payloadOffset, payloadLength);
//
//		if (signer.verifySignature(signatureBytes)) {
//			System.out.println("SIGNATURE VALID!!!");
//			return true;
//		}
//		else {
//			System.out.println("SIGNATURE *NOT* VALID!!!");
//			return false;
//		}
//	}

//	private void reverse(final byte[] bytes) {
//		for (int i = 0; i < (bytes.length / 2); ++i) {
//			final byte b = bytes[i];
//			bytes[i] = bytes[bytes.length - 1 - i];
//			bytes[bytes.length - 1 - i] = b;
//		}
//	}

	@Test(expected = SignatureException.class)
	public void signAndVerifyRandomBroken() throws Exception {
		final byte[][] plainAndSigned = createRandomSignedData(100);
		final byte[] plain = plainAndSigned[0];
		final byte[] signed = plainAndSigned[1];

		final int signedHeaderOffset = 27;

		for (int i = 0; i < plain.length; ++i)
			assertThat(signed[signedHeaderOffset + i]).isEqualTo(plain[i]);

		final int plainOffset = random.nextInt(plain.length);

		final int oldValue = plain[plainOffset] & 0xff;
		int newValue;
		while (oldValue == (newValue = random.nextInt(256)));

		signed[signedHeaderOffset + plainOffset] = plain[plainOffset] = (byte) newValue;

		verify(plain, signed);
	}

	private byte[][] createRandomSignedData(final int writeCountMax) throws IOException {
		final byte[] plain;
		final byte[] signed;

		try (final ByteArrayOutputStream plainOut = new ByteArrayOutputStream();) {
			try (final ByteArrayOutputStream signedOut = new ByteArrayOutputStream();) {
				try (final SignerOutputStream signerOutputStream = new SignerOutputStream(signedOut, userRepoKey, signatureCreated);) {
					final int writeCount = 1 + random.nextInt(writeCountMax);
					for (int w = 0; w < writeCount; ++w) {
						if (random.nextInt(100) < 20) {
							final int b = random.nextInt(256);
							plainOut.write(b);
							signerOutputStream.write(b);
						}
						else {
							final byte[] buf = new byte[1 + random.nextInt(1024 * 1024)];
							random.nextBytes(buf);
							plainOut.write(buf);
							signerOutputStream.write(buf);
						}
//						System.out.printf("Wrote block %s of %s.\n", w + 1, writeCount);
					}
				}
				signed = signedOut.toByteArray();
			}
			plain = plainOut.toByteArray();
		}
//		System.out.println("Writing completed.");
		return new byte[][] { plain, signed };
	}

	private void verify(final byte[] plain, final byte[] signed) throws IOException {
		final byte[] plainVerified;

		try (ByteArrayOutputStream plainVerifiedOut = new ByteArrayOutputStream();) {
			try (VerifierInputStream in = new VerifierInputStream(new ByteArrayInputStream(signed), userRepoKeyPublicKeyLookup);) {
				while (true) {
					if (random.nextInt(100) < 20) {
						final int read = in.read();
						if (read < 0)
							break;

						plainVerifiedOut.write(read);
					}
					else {
						final byte[] buf = new byte[1 + random.nextInt(1024 * 1024)];
						final int read = in.read(buf);
						if (read < 0)
							break;

						plainVerifiedOut.write(buf, 0, read);
					}
				}
			}
			plainVerified = plainVerifiedOut.toByteArray();
		}

		assertThat(plainVerified).isEqualTo(plain);
	}
}
