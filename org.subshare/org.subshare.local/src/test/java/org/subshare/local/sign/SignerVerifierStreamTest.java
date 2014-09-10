package org.subshare.local.sign;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

import org.subshare.core.sign.SignerOutputStream;
import org.subshare.core.sign.VerifierInputStream;
import org.subshare.core.user.UserRepoKey;
import org.subshare.core.user.UserRepoKey.PublicKey;
import org.subshare.core.user.UserRepoKeyPublicKeyLookup;
import org.subshare.core.user.UserRepoKeyRing;
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

	@BeforeClass
	public static void beforeClass() {
		repositoryId = UUID.randomUUID();
		userRepoKeyRing = createUserRepoKeyRing(repositoryId);
		userRepoKey = userRepoKeyRing.getRandomUserRepoKeyOrFail(repositoryId);
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
		final byte[][] plainAndSigned = createRandomSignedData();
		final byte[] plain = plainAndSigned[0];
		final byte[] signed = plainAndSigned[1];

		verify(plain, signed);
	}

	@Test(expected = SignatureException.class)
	public void signAndVerifyRandomBroken() throws Exception {
		final byte[][] plainAndSigned = createRandomSignedData();
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

	private byte[][] createRandomSignedData() throws IOException {
		final byte[] plain;
		final byte[] signed;

		try (final ByteArrayOutputStream plainOut = new ByteArrayOutputStream();) {
			try (final ByteArrayOutputStream signedOut = new ByteArrayOutputStream();) {
				try (final SignerOutputStream signerOutputStream = new SignerOutputStream(signedOut, userRepoKey);) {
					final int writeCount = random.nextInt(100);
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
