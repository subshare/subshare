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

import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class AsymmetricKeyTest
{
	private final SecureRandom secureRandom = new SecureRandom();

	@Test
	public void encodeDecodeRSA() throws Exception {
		encodeDecode("RSA/ECB/OAEPWITHSHA1ANDMGF1PADDING");
	}

	@Test
	public void encodeDecodeRSAwithOAEPwithSHA1andMGF1Padding() throws Exception {
		encodeDecode("RSA/ECB/OAEPWITHSHA1ANDMGF1PADDING");
	}

	private void encodeDecode(final String transformation) throws Exception {
		final String engine = CryptoRegistry.splitTransformation(transformation)[0];

		final AsymmetricCipherKeyPairGenerator keyPairGenerator = CryptoRegistry.getInstance().createKeyPairGenerator(engine, true);
		final AsymmetricCipherKeyPair keyPair = keyPairGenerator.generateKeyPair();

		final byte[] encodedPrivateKey = CryptoRegistry.getInstance().encodePrivateKey(keyPair.getPrivate());
		final byte[] encodedPublicKey = CryptoRegistry.getInstance().encodePublicKey(keyPair.getPublic());

		final AsymmetricKeyParameter decodedPrivateKey = CryptoRegistry.getInstance().decodePrivateKey(encodedPrivateKey);
		final AsymmetricKeyParameter decodedPublicKey = CryptoRegistry.getInstance().decodePublicKey(encodedPublicKey);

		final byte[] plainText = new byte[100 + secureRandom.nextInt(40)];
		secureRandom.nextBytes(plainText);

		final Cipher cipher = CryptoRegistry.getInstance().createCipher(transformation);

		cipher.init(CipherOperationMode.ENCRYPT, keyPair.getPublic());
		final byte[] encrypted1 = cipher.doFinal(plainText);

		cipher.init(CipherOperationMode.ENCRYPT, decodedPublicKey);
		final byte[] encrypted2 = cipher.doFinal(plainText);

		cipher.init(CipherOperationMode.DECRYPT, keyPair.getPrivate());
		final byte[] decrypted1a = cipher.doFinal(encrypted1);
		final byte[] decrypted2a = cipher.doFinal(encrypted2);

		cipher.init(CipherOperationMode.DECRYPT, decodedPrivateKey);
		final byte[] decrypted1b = cipher.doFinal(encrypted1);
		final byte[] decrypted2b = cipher.doFinal(encrypted2);

		Assert.assertArrayEquals(plainText, decrypted1a);
		Assert.assertArrayEquals(plainText, decrypted1b);
		Assert.assertArrayEquals(plainText, decrypted2a);
		Assert.assertArrayEquals(plainText, decrypted2b);
	}
}
