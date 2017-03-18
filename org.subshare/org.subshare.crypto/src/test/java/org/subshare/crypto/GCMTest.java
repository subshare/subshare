package org.subshare.crypto;

import static org.assertj.core.api.Assertions.*;

import java.security.SecureRandom;

import org.bouncycastle.crypto.engines.TwofishEngine;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.junit.Test;

public class GCMTest {

	private static final SecureRandom random = new SecureRandom();

	/*
	 * There was a problem with GCM encryption. The en/decryption failed with about 1500 objetcs.
	 * The problem was not complete covered and was solved by updating the kernel and java.
	 * Maybe this was caused by a bug in an old Java version.
	 */
	@Test
	public void testEncryptionWithCipher() throws Exception
	{
		final byte[] plain = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		final byte[] key = new byte[] {
				 1,  2,  3,  4,  5,  6,  7,  8,
				 9, 10, 11, 12, 13, 14, 15, 16,
				17, 18, 19, 20, 21, 22, 23, 24,
				25, 26, 27, 28, 29, 30, 31, 32
		};
		final byte[] iv = new byte[] {
				 1,  2,  3,  4,  5,  6,  7,  8,
				 9, 10, 11, 12, 13, 14, 15, 16
		};
		byte[] firstCiphertext = null;

		final Cipher cipher = CryptoRegistry.getInstance().createCipher("Twofish/GCM/NoPadding");
		final Cipher invCipher = CryptoRegistry.getInstance().createCipher("Twofish/GCM/NoPadding");
//		Cipher cipher = CryptoRegistry.getInstance().createCipher("Twofish/CFB/NoPadding");
		cipher.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(new KeyParameter(key), iv));
		invCipher.init(CipherOperationMode.DECRYPT, new ParametersWithIV(new KeyParameter(key), iv));

		for (int i = 0; i < 10000; ++i) {
			System.out.println("*** " + i + " ***");
			random.nextBytes(iv);
//			Thread.sleep(50);

			// Whether we re-initialise with or without key does not matter.
//			cipher.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(new KeyParameter(key), iv));
			cipher.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(null, iv));
			invCipher.init(CipherOperationMode.DECRYPT, new ParametersWithIV(null, iv));

			final byte[] ciphertext = cipher.doFinal(plain);
			if (firstCiphertext == null)
				firstCiphertext = ciphertext;
			else
				assertThat(ciphertext).isNotEqualTo(firstCiphertext);

			byte[] decrypted = invCipher.doFinal(ciphertext);
			assertThat(decrypted).isEqualTo(plain);
		}
	}

	@Test
	public void testEncryptionWithBCLowLevelAPI() throws Exception
	{
		final byte[] plain = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 };
		final byte[] key = new byte[] {
				 1,  2,  3,  4,  5,  6,  7,  8,
				 9, 10, 11, 12, 13, 14, 15, 16,
				17, 18, 19, 20, 21, 22, 23, 24,
				25, 26, 27, 28, 29, 30, 31, 32
		};
		final byte[] iv = new byte[] {
				 1,  2,  3,  4,  5,  6,  7,  8,
				 9, 10, 11, 12, 13, 14, 15, 16
		};

//		{ // first try CFB - works fine, currently (2011-09-23).
//			CipherParameters parameters = new ParametersWithIV(new KeyParameter(key), iv);
//			byte[] firstCiphertext = null;
//			BufferedBlockCipher cipher = new BufferedBlockCipher(new CFBBlockCipher(new TwofishEngine(), 128));
//
//			cipher.init(true, parameters);
//
//			for (int i = 0; i < 10000; ++i) {
//				System.out.println("*** cfb " + i + " ***");
//
//				// Whether we re-initialise with or without key does not matter.
//				cipher.init(true, parameters);
//
//				byte[] ciphertext = new byte[cipher.getOutputSize(plain.length)];
//				int encLength = cipher.processBytes(plain, 0, plain.length, ciphertext, 0);
//				cipher.doFinal(ciphertext, encLength);
//
//				if (firstCiphertext == null)
//					firstCiphertext = ciphertext;
//				else
//					Assert.assertArrayEquals(firstCiphertext, ciphertext);
//			}
//		}


		{ // now try GCM - fails on 'fhernhache', currently (2011-09-23).
			byte[] firstCiphertext = null;
//			AEADParameters parameters = new AEADParameters(new KeyParameter(key), 128, iv, plain);
			final AEADBlockCipher cipher = new GCMBlockCipher(new TwofishEngine());
			final AEADBlockCipher invCipher = new GCMBlockCipher(new TwofishEngine());

			cipher.init(true, new ParametersWithIV(new KeyParameter(key), iv));
			invCipher.init(false, new ParametersWithIV(new KeyParameter(key), iv));

			for (int i = 0; i < 10000; ++i) {
				System.out.println("*** gcm " + i + " ***");
				random.nextBytes(iv);

				// Whether we re-initialise with or without key does not matter.
				cipher.init(true, new ParametersWithIV(null, iv));
				invCipher.init(false, new ParametersWithIV(null, iv));

				final byte[] ciphertext = new byte[cipher.getOutputSize(plain.length)];
				final int encLength = cipher.processBytes(plain, 0, plain.length, ciphertext, 0);
				cipher.doFinal(ciphertext, encLength);

				if (firstCiphertext == null)
					firstCiphertext = ciphertext;
				else
					assertThat(ciphertext).isNotEqualTo(firstCiphertext);

				final byte[] decrypted = new byte[cipher.getOutputSize(ciphertext.length)];
				int decLength = invCipher.processBytes(ciphertext, 0, ciphertext.length, decrypted, 0);
				decLength += invCipher.doFinal(decrypted, decLength);
				final byte[] decryptedTruncated = new byte[decLength];
				System.arraycopy(decrypted, 0, decryptedTruncated, 0, decLength);
				assertThat(decryptedTruncated).isEqualTo(plain);
			}
		}
	}
}
