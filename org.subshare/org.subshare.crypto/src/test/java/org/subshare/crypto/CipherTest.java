/*
 * Cumulus4j - Securing your data in the cloud - http://cumulus4j.org
 * Copyright (C) 2011 NightLabs Consulting GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.subshare.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class CipherTest
{
	private static final Logger logger = LoggerFactory.getLogger(CipherTest.class);

	private static final BouncyCastleProvider bouncyCastleProvider = new BouncyCastleProvider();

//	private static final boolean USE_BOUNCY_CASTLE_JCE_PROVIDER = true;
//	static {
//		if (USE_BOUNCY_CASTLE_JCE_PROVIDER) {
//			Security.insertProviderAt(bouncyCastleProvider, 2);
//
//			KeyGenerator kg;
//			try {
//				kg = KeyGenerator.getInstance("AES");
//			} catch (NoSuchAlgorithmException e) {
//				logger.warn("KeyGenerator.getInstance(\"AES\") failed: " + e, e);
//				kg = null;
//			}
//
//			if (kg == null || kg.getProvider() != bouncyCastleProvider)
//				logger.warn("BouncyCastleProvider was NOT registered!!!");
//		}
//	}

	private static final String[] SYMMETRIC_TRANSFORMATIONS = {
		"AES/CBC/NoPadding",
		"AES/CBC/ISO10126Padding",
		"AES/CBC/ISO10126-2",
		"AES/CBC/ISO7816-4",
		"AES/CBC/TBC",
		"AES/CBC/X9.23",
		"AES/CBC/ZeroByte",
		"AES/CBC/PKCS5",
		"AES/CBC/PKCS5Padding",
		"AES/CBC/PKCS7Padding",

		"AES/CFB/NoPadding",
		"AES/CFB/ISO10126Padding",
		"AES/CFB/ISO10126-2",
		"AES/CFB/ISO7816-4",
		"AES/CFB/TBC",
		"AES/CFB/X9.23",
		"AES/CFB/ZeroByte",
		"AES/CFB/PKCS5",
		"AES/CFB/PKCS5Padding",
		"AES/CFB/PKCS7Padding",

		"AES/CFB8/NoPadding",
		"AES/CFB8/ISO10126Padding",
		"AES/CFB8/ISO10126-2",
		"AES/CFB8/ISO7816-4",
		"AES/CFB8/TBC",
		"AES/CFB8/X9.23",
		"AES/CFB8/ZeroByte",
		"AES/CFB8/PKCS5",
		"AES/CFB8/PKCS5Padding",
		"AES/CFB8/PKCS7Padding",

		"AES/CFB16/NoPadding",
		"AES/CFB16/ISO10126Padding",
		"AES/CFB16/ISO10126-2",
		"AES/CFB16/ISO7816-4",
		"AES/CFB16/TBC",
		"AES/CFB16/X9.23",
		"AES/CFB16/ZeroByte",
		"AES/CFB16/PKCS5",
		"AES/CFB16/PKCS5Padding",
		"AES/CFB16/PKCS7Padding",

		"AES/CFB64/NoPadding",
		"AES/CFB64/ISO10126Padding",
		"AES/CFB64/ISO10126-2",
		"AES/CFB64/ISO7816-4",
		"AES/CFB64/TBC",
		"AES/CFB64/X9.23",
		"AES/CFB64/ZeroByte",
		"AES/CFB64/PKCS5",
		"AES/CFB64/PKCS5Padding",
		"AES/CFB64/PKCS7Padding",

		"AES/OFB/NoPadding",
		"AES/OFB/ISO10126Padding",
		"AES/OFB/ISO10126-2",
		"AES/OFB/ISO7816-4",
		"AES/OFB/TBC",
		"AES/OFB/X9.23",
		"AES/OFB/ZeroByte",
		"AES/OFB/PKCS5",
		"AES/OFB/PKCS5Padding",
		"AES/OFB/PKCS7Padding"
	};

	private static final String[] ASYMMETRIC_TRANSFORMATIONS = {
		"RSA",
		"RSA//",
		"RSA//NoPadding",    // same as: RSA/ECB/NoPadding
		"RSA/None/NoPadding", // same as: RSA//NoPadding
		"RSA/ECB/NoPadding", // same as: RSA//NoPadding
		"RSA/CBC/NoPadding", // does not exist!
		"RSA//ISO9796-1",
		"RSA//OAEP",
		"RSA//PKCS1",
		"RSA//PKCS1Padding",
		"RSA//OAEPWITHSHA1ANDMGF1PADDING",   // same as: RSA/ECB/OAEPWITHSHA1ANDMGF1PADDING
		"RSA/ECB/OAEPWITHSHA1ANDMGF1PADDING", // same as: RSA//OAEPWITHSHA1ANDMGF1PADDING
		"RSA/None/OAEPWITHSHA1ANDMGF1PADDING" // same as: RSA//OAEPWITHSHA1ANDMGF1PADDING
	};

	@Test
	public void testLookupCompatibilityWithJCE()
	{
//		Security.insertProviderAt(bouncyCastleProvider, 2);
//		try {
		final List<String> transformations = new ArrayList<String>();
//		transformations.addAll(Arrays.asList(SYMMETRIC_TRANSFORMATIONS));
		transformations.addAll(Arrays.asList(ASYMMETRIC_TRANSFORMATIONS));

		for (final String transformation : transformations) {
			Throwable jceError = null;
			Throwable cryptoRegistryError = null;

			try {
				Cipher.getInstance(transformation);
			} catch (final Throwable t) {
				jceError = t;
			}

			try {
				CryptoRegistry.getInstance().createCipher(transformation);
			} catch (final Throwable t) {
				cryptoRegistryError = t;
			}

			if (jceError == null) {
				if (cryptoRegistryError != null) {
					final String errorMessage = "JCE successfully provided a Cipher for transformation=\"" + transformation + "\", but our CryptoRegistry failed: " + cryptoRegistryError;
					logger.error(errorMessage, cryptoRegistryError);
					Assert.fail(errorMessage);
				}
			}
			else {
				if (cryptoRegistryError == null)
					logger.warn("JCE fails to provide a Cipher for transformation=\"" + transformation + "\", but our CryptoRegistry succeeded!");
				else if (jceError.getClass() != cryptoRegistryError.getClass())
					Assert.fail("JCE fails to provide a Cipher for transformation=\"" + transformation + "\" with a " + jceError.getClass().getName() + ", but our CryptoRegistry failed with another exception: " + cryptoRegistryError.getClass());
				else
					logger.warn("Both JCE and CryptoRegistry failed to provide a cipher for transformation=\"" + transformation + "\" with the same type of exception: " + cryptoRegistryError.getClass().getName());
			}
		}
//		} finally {
//			Security.removeProvider(bouncyCastleProvider.getName());
//		}
	}

	@Test
	public void testLookupAllSupportedCiphers()
	throws Exception
	{
		final long start = System.currentTimeMillis();
		final Set<String> transformations = CryptoRegistry.getInstance().getSupportedCipherTransformations(null);
		for (final String transformation : transformations) {
			logger.debug("testLookupAllSupportedCiphers: Creating cipher for transformation \"{}\".", transformation);
			CryptoRegistry.getInstance().createCipher(transformation);
		}
		logger.info(
				"testLookupAllSupportedCiphers: Successfully created {} ciphers in {} msec.",
				transformations.size(), System.currentTimeMillis() - start
		);
	}

	private final SecureRandom random = new SecureRandom();

	private static String getEngineName(final String transformation)
	{
		return CryptoRegistry.splitTransformation(transformation)[0];
	}

	private static String getPaddingName(final String transformation)
	{
		return CryptoRegistry.splitTransformation(transformation)[2];
	}

//	@Test
//	public void testSymmetricEncryptionCompatibilityWithJCE_SunProvider()
//	throws Exception
//	{
//		for (String transformation : SYMMETRIC_TRANSFORMATIONS)
//		{
//			try {
//				String paddingName = getPaddingName(transformation);
//				if ("".equals(paddingName) || "NOPADDING".equals(paddingName.toUpperCase(Locale.ENGLISH)))
//					continue;
//
//				Cipher jceCipher;
//				try {
//					jceCipher = Cipher.getInstance(transformation);
//				} catch (Throwable t) {
//					continue;
//				}
//
//				org.cumulus4j.crypto.Cipher c4jCipher = CryptoRegistry.getInstance().createCipher(transformation);
//				byte[] original = new byte[1024 + random.nextInt(10240)];
//				random.nextBytes(original);
//
//				byte[] iv = new byte[c4jCipher.getIVSize()];
//				random.nextBytes(iv);
//
//				// we generate a random 128 bit key
//				byte[] key = new byte[128 / 8];
//				random.nextBytes(key);
//
//				c4jCipher.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(new KeyParameter(key), iv));
//				jceCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, getEngineName(transformation)), new IvParameterSpec(iv));
//
//				byte[] encrypted = c4jCipher.doFinal(original);
//				byte[] decrypted = jceCipher.doFinal(encrypted);
//				Assert.assertTrue(
//						"Decrypted does not match original for transformation \"" + transformation + "\"!",
//						Arrays.equals(original, decrypted)
//				);
//			} catch (Exception x) {
//				throw new Exception("Processing transformation \"" + transformation + "\" failed: " + x, x);
//			}
//		}
//	}

	@Test
	public void testSymmetricEncryptionCompatibilityWithJCE_BouncyCastleProvider()
	throws Exception
	{
		Security.insertProviderAt(bouncyCastleProvider, 2);
		try {
			KeyGenerator kg;
			try {
				kg = KeyGenerator.getInstance("AES");
			} catch (final NoSuchAlgorithmException e) {
				logger.warn("KeyGenerator.getInstance(\"AES\") failed: " + e, e);
				kg = null;
			}

			if (kg == null || kg.getProvider() != bouncyCastleProvider)
				Assert.fail("Registering BouncyCastleProvider failed!");

			for (final String transformation : SYMMETRIC_TRANSFORMATIONS)
			{
				try {
					final String paddingName = getPaddingName(transformation);
					if ("".equals(paddingName) || "NOPADDING".equals(paddingName.toUpperCase(Locale.ENGLISH)))
						continue;

					Cipher jceCipher;
					try {
						jceCipher = Cipher.getInstance(transformation);
					} catch (final Throwable t) {
						continue;
					}

					final org.subshare.crypto.Cipher c4jCipher = CryptoRegistry.getInstance().createCipher(transformation);
					final byte[] original = new byte[1024 + random.nextInt(10240)];
					random.nextBytes(original);

					final byte[] iv = new byte[c4jCipher.getIVSize()];
					random.nextBytes(iv);

					// we generate a random 128 bit key
					final byte[] key = new byte[128 / 8];
					random.nextBytes(key);

					c4jCipher.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(new KeyParameter(key), iv));
					jceCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, getEngineName(transformation)), new IvParameterSpec(iv));

					final byte[] encrypted = c4jCipher.doFinal(original);
					final byte[] decrypted = jceCipher.doFinal(encrypted);
					Assert.assertTrue(
							"Decrypted does not match original for transformation \"" + transformation + "\"!",
							Arrays.equals(original, decrypted)
					);
				} catch (final Exception x) {
					throw new Exception("Processing transformation \"" + transformation + "\" failed: " + x, x);
				}
			}
		} finally {
			Security.removeProvider(bouncyCastleProvider.getName());
		}
	}

	private final Map<Integer, byte[]> blockSize2Plaintext = new HashMap<Integer, byte[]>();

	private byte[] getPlaintext(final int blockSize)
	{
		byte[] plaintext = blockSize2Plaintext.get(blockSize);
		if (plaintext == null) {

			if (blockSize < 0)
				plaintext = new byte[10240 + random.nextInt(10241)];
			else
				plaintext = new byte[blockSize];

			random.nextBytes(plaintext);
			blockSize2Plaintext.put(blockSize, plaintext);
		}
		return plaintext;
	}

	@Test
	public void testSymmetricEncryptionWithoutIv()
	throws Exception
	{
		final Set<String> transformations = new TreeSet<String>();
//		transformations.addAll(CryptoRegistry.getInstance().getSupportedCipherTransformations(CipherEngineType.symmetricBlock));
		transformations.add("AES/CBC/PKCS5Padding");
		transformations.add("AES/CFB/NoPadding");
		transformations.add("Twofish/CBC/PKCS5Padding");
		transformations.add("Twofish/CFB/NoPadding");

		final byte[] key = new byte[128 / 8];
		random.nextBytes(key);
		final KeyParameter keyParameter = new KeyParameter(key);


		for (int testMode = 0; testMode < 4; ++testMode) {
			for (final String transformation : transformations) {
				logger.info("transformation={}", transformation);
				try {
					final org.subshare.crypto.Cipher encrypter = CryptoRegistry.getInstance().createCipher(transformation);
					final org.subshare.crypto.Cipher decrypter = CryptoRegistry.getInstance().createCipher(transformation);

					final byte[] plaintext = getPlaintext(encrypter.getInputBlockSize());
					if (plaintext.length < 1)
						throw new IllegalStateException("plaintext.length < 1");

					final byte[] nullIV = new byte[encrypter.getIVSize()];

					switch (testMode) {
						case 0:
							encrypter.init(CipherOperationMode.ENCRYPT, keyParameter);
							decrypter.init(CipherOperationMode.DECRYPT, keyParameter);
							break;
						case 1:
							encrypter.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(keyParameter, nullIV));
							decrypter.init(CipherOperationMode.DECRYPT, keyParameter);
							break;
						case 2:
							encrypter.init(CipherOperationMode.ENCRYPT, keyParameter);
							decrypter.init(CipherOperationMode.DECRYPT, new ParametersWithIV(keyParameter, nullIV));
							break;
						case 3:
							encrypter.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(keyParameter, nullIV));
							decrypter.init(CipherOperationMode.DECRYPT, new ParametersWithIV(keyParameter, nullIV));
							break;
						default:
							throw new IllegalStateException("Unknown testMode: " + testMode);
					}

					final byte[] plain = new byte[random.nextInt(1024 * 1024)];
					random.nextBytes(plain);

					final byte[] encrypted = encrypter.doFinal(plain);
					final byte[] decrypted = decrypter.doFinal(encrypted);

					assertThat(decrypted).isEqualTo(plain);
				} catch (final Exception x) {
					throw new RuntimeException("Test failed for transformation \"" + transformation + "\": " + x, x);
				}
			}
		}
	}

	@Test
	public void testNullAsKeyParameter()
	throws Exception
	{
		final Set<String> transformations = new TreeSet<String>();
//		transformations.add("AES.FAST/CTS/");
		transformations.addAll(CryptoRegistry.getInstance().getSupportedCipherTransformations(CipherEngineType.symmetricBlock));
		transformations.addAll(CryptoRegistry.getInstance().getSupportedCipherTransformations(CipherEngineType.symmetricStream));

		final byte[] key = new byte[128 / 8];
		random.nextBytes(key);
		final KeyParameter keyParameter = new KeyParameter(key);

		final Map<String, Throwable> transformation2throwable = new TreeMap<String, Throwable>();

		for (final String transformation : transformations) {
			logger.info("transformation={}", transformation);
			try {
				final org.subshare.crypto.Cipher encrypter = CryptoRegistry.getInstance().createCipher(transformation);
				final org.subshare.crypto.Cipher decrypter = CryptoRegistry.getInstance().createCipher(transformation);

				final byte[] plaintext = getPlaintext(encrypter.getInputBlockSize());
				if (plaintext.length < 1)
					throw new IllegalStateException("plaintext.length < 1");

				if (encrypter.getIVSize() > 0) {
					final byte[] iv = new byte[encrypter.getIVSize()];
					random.nextBytes(iv);
					encrypter.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(keyParameter, iv));
					decrypter.init(CipherOperationMode.DECRYPT, new ParametersWithIV(keyParameter, iv));
				}
				else {
					encrypter.init(CipherOperationMode.ENCRYPT, keyParameter);
					decrypter.init(CipherOperationMode.DECRYPT, keyParameter);
				}

				if (encrypter.getIVSize() <= 0)
					logger.info("testNullAsKeyParameter: Transformation \"{}\" does not support IV => Skipping.", transformation);
				else {
					final byte[] iv = new byte[encrypter.getIVSize()];
					random.nextBytes(iv);

					try {
						encrypter.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(null, iv));
						decrypter.init(CipherOperationMode.DECRYPT, new ParametersWithIV(null, iv));
					} catch (final Exception x) {
						transformation2throwable.put(transformation, x);
					}
				}
			} catch (final Exception x) {
//				throw new RuntimeException("Test failed for transformation \"" + transformation + "\": " + x, x);
				logger.error("transformation \"" + transformation + "\": " + x);
			}
		}

		if (!transformation2throwable.isEmpty()) {
			logger.error(">>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			for (final Map.Entry<String, Throwable> me : transformation2throwable.entrySet()) {
				final String transformation = me.getKey();
				logger.error("transformation \"" + transformation + "\": " + me.getValue());
			}
			logger.error("<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		}
	}

//	@Test
//	public void testAllCiphersEncryptionAndDecryption()
//	throws Exception
//	{
//		final boolean passNullAsKeyParameter = false;
//
//		Set<String> transformations = new TreeSet<String>();
////		transformations.add("AES.FAST/CFB136/");
//		transformations.add("AES.FAST/CTS/");
//		transformations.add("AES.FAST/CBC-CTS/");
////		transformations.addAll(CryptoRegistry.getInstance().getSupportedCipherTransformations(CipherEngineType.symmetricBlock));
////		transformations.addAll(CryptoRegistry.getInstance().getSupportedCipherTransformations(CipherEngineType.symmetricStream));
//
//		byte[] key = new byte[128 / 8];
//		random.nextBytes(key);
//		KeyParameter keyParameter = new KeyParameter(key);
//
//		for (String transformation : transformations) {
//			logger.info("transformation={}", transformation);
//			try {
//				org.cumulus4j.crypto.Cipher encrypter = CryptoRegistry.getInstance().createCipher(transformation);
//				org.cumulus4j.crypto.Cipher decrypter = CryptoRegistry.getInstance().createCipher(transformation);
//
//				byte[] plaintext = getPlaintext(-1);
////				byte[] plaintext = getPlaintext(encrypter.getInputBlockSize());
//				if (plaintext.length < 1)
//					throw new IllegalStateException("plaintext.length < 1");
//
//				if (encrypter.getIVSize() > 0) {
//					byte[] iv = new byte[encrypter.getIVSize()];
//					random.nextBytes(iv);
//					encrypter.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(keyParameter, iv));
//					decrypter.init(CipherOperationMode.DECRYPT, new ParametersWithIV(keyParameter, iv));
//				}
//				else {
//					encrypter.init(CipherOperationMode.ENCRYPT, keyParameter);
//					decrypter.init(CipherOperationMode.DECRYPT, keyParameter);
//				}
//
//				byte[] ciphertext1 = encrypter.doFinal(plaintext);
//				byte[] decrypted1 = decrypter.doFinal(ciphertext1);
//
//				Assert.assertArrayEquals(plaintext, decrypted1);
//
//				byte[] ciphertext1a = encrypter.doFinal(plaintext);
//				if (!Arrays.equals(ciphertext1, ciphertext1a))
//					logger.info("testNullAsKeyParameter: Transformation \"{}\" caused a 2nd encryption (with same key + IV) to produce a different ciphertext.", transformation);
//
//				byte[] decrypted1a = decrypter.doFinal(ciphertext1a);
//
//				Assert.assertArrayEquals(plaintext, decrypted1a);
//
//				if (encrypter.getIVSize() <= 0)
//					logger.info("testNullAsKeyParameter: Transformation \"{}\" does not support IV => Skipping.", transformation);
//				else {
//					byte[] iv = new byte[encrypter.getIVSize()];
//					random.nextBytes(iv);
//
//					if (passNullAsKeyParameter)
//						encrypter.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(null, iv));
//					else
//						encrypter.init(CipherOperationMode.ENCRYPT, new ParametersWithIV(keyParameter, iv));
//
//					byte[] ciphertext2 = encrypter.doFinal(plaintext);
//
//					Assert.assertFalse(
//							"Transformation \"" + transformation + "\": Even though the IVs are different, the ciphertexts are the same!",
//							Arrays.equals(ciphertext1, ciphertext2)
//					);
//
//					try {
//						byte[] decrypted2 = decrypter.doFinal(ciphertext1);
//
//						if (Arrays.equals(plaintext, decrypted2))
//							logger.info("testNullAsKeyParameter: Decrypting with transformation \"{}\" and wrong IV worked without exception and decrypted correctly.", transformation);
//						else
//							logger.info("testNullAsKeyParameter: Decrypting with transformation \"{}\" and wrong IV worked without exception but decrypted incorrectly.", transformation);
//
////						Assert.assertFalse(
////								"Transformation \"" + transformation + "\": Even though the IVs are different, the ciphertext was successfully decrypted!",
////								Arrays.equals(plaintext, decrypted2)
////						);
//					} catch (CryptoException x) {
//						logger.info("testNullAsKeyParameter: Decrypting with transformation \"{}\" and wrong IV caused a CryptoException.", transformation);
//					}
//
//					if (passNullAsKeyParameter)
//						decrypter.init(CipherOperationMode.DECRYPT, new ParametersWithIV(null, iv));
//					else
//						decrypter.init(CipherOperationMode.DECRYPT, new ParametersWithIV(keyParameter, iv));
//
//				}
//
//			} catch (Exception x) {
//				throw new RuntimeException("Test failed for transformation \"" + transformation + "\": " + x, x);
//			}
//		}
//	}
}
