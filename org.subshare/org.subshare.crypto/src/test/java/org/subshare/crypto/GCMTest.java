package org.subshare.crypto;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.TwofishEngine;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.junit.Assert;
import org.junit.Test;

public class GCMTest {

//	private static SecureRandom random = new SecureRandom();

//	private static byte[] getBytesFromResource(String resourceName) throws IOException
//	{
//		InputStream in = GCMTest.class.getResourceAsStream(resourceName);
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		IOUtil.transferStreamData(in, out);
//		in.close();
//		out.close();
//		return out.toByteArray();
//	}
//
//	@Test
//	public void testWithCipher()
//	throws Exception
//	{
//		String debugDumpFileName = "IndexEntryLong_235_20110923_125513_768";
//
//		String transformation = "Twofish/GCM/NoPadding";
////		String transformation = "Twofish/CBC/PKCS5Padding";
//		Cipher encrypter = CryptoRegistry.getInstance().createCipher(transformation);
//		Cipher decrypter = CryptoRegistry.getInstance().createCipher(transformation);
//
////		byte[] iv = new byte[encrypter.getIVSize()];
////		random.nextBytes(iv);
//		byte[] iv = getBytesFromResource(debugDumpFileName + ".iv");
//
////		byte[] key = new byte[256 / 8];
////		random.nextBytes(key);
//		byte[] key = getBytesFromResource(debugDumpFileName + ".key");
//
//		encrypter.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(new KeyParameter(key), iv));
//		decrypter.init(CipherOperationMode.DECRYPT, new ParametersWithIV(new KeyParameter(key), iv));
//
//		byte[] plain = getBytesFromResource(debugDumpFileName + ".plain");
//		byte[] encrypted = encrypter.doFinal(plain);
//
////		byte[] encryptedResource = getBytesFromResource(debugDumpFileName + ".crypt");
////		Assert.assertArrayEquals("The dumped ciphertext is different from the newly encrypted ciphertext!", encrypted, encryptedResource);
//
//		byte[] decrypted = decrypter.doFinal(encrypted);
//
//		Assert.assertArrayEquals(plain, decrypted);
//
//////		InputStream in = GCMTest.class.getResourceAsStream("IndexEntryInteger_6_20110922_182710_317");
////		InputStream in2 = GCMTest.class.getResourceAsStream("IndexEntryInteger_6_20110922_182714_379");
//////		InputStream in = GCMTest.class.getResourceAsStream("IndexEntryInteger_6_20110922_182714_458");
////		ByteArrayOutputStream out2 = new ByteArrayOutputStream();
////		IOUtil.transferStreamData(in2, out2);
////		in2.close();
////		out2.close();
////		byte[] plain2 = out2.toByteArray();
////		byte[] encrypted2 = encrypter.doFinal(plain2);
////		byte[] decrypted2 = decrypter.doFinal(encrypted2);
////
////		Assert.assertArrayEquals(plain2, decrypted2);
////
//////		InputStream in = GCMTest.class.getResourceAsStream("IndexEntryInteger_6_20110922_182710_317");
//////		InputStream in = GCMTest.class.getResourceAsStream("IndexEntryInteger_6_20110922_182714_379");
////		InputStream in3 = GCMTest.class.getResourceAsStream("IndexEntryInteger_6_20110922_182714_458");
////		ByteArrayOutputStream out3 = new ByteArrayOutputStream();
////		IOUtil.transferStreamData(in3, out3);
////		in3.close();
////		out3.close();
////		byte[] plain3 = out3.toByteArray();
////		byte[] encrypted3 = encrypter.doFinal(plain3);
////		byte[] decrypted3 = decrypter.doFinal(encrypted3);
////
////		Assert.assertArrayEquals(plain3, decrypted3);
//	}


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
//		Cipher cipher = CryptoRegistry.getInstance().createCipher("Twofish/CFB/NoPadding");
		cipher.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(new KeyParameter(key), iv));

		for (int i = 0; i < 10000; ++i) {
			System.out.println("*** " + i + " ***");
//			Thread.sleep(50);

			// Whether we re-initialise with or without key does not matter.
			cipher.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(null, iv));
//			cipher.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(new KeyParameter(key), iv));

			final byte[] ciphertext = cipher.doFinal(plain);
			if (firstCiphertext == null)
				firstCiphertext = ciphertext;
			else
				Assert.assertArrayEquals(firstCiphertext, ciphertext);
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
			final CipherParameters parameters = new ParametersWithIV(new KeyParameter(key), iv);
			byte[] firstCiphertext = null;
//			AEADParameters parameters = new AEADParameters(new KeyParameter(key), 128, iv, plain);
			final AEADBlockCipher cipher = new GCMBlockCipher(new TwofishEngine());

			cipher.init(true, parameters);

			for (int i = 0; i < 10000; ++i) {
				System.out.println("*** gcm " + i + " ***");

				// Whether we re-initialise with or without key does not matter.
				cipher.init(true, parameters);

				final byte[] ciphertext = new byte[cipher.getOutputSize(plain.length)];
				final int encLength = cipher.processBytes(plain, 0, plain.length, ciphertext, 0);
				cipher.doFinal(ciphertext, encLength);

				if (firstCiphertext == null)
					firstCiphertext = ciphertext;
				else
					Assert.assertArrayEquals(firstCiphertext, ciphertext);
			}
		}
	}
}
