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
import java.util.Arrays;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.core.util.HashUtil;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class MACTest
{
	private static final Logger logger = LoggerFactory.getLogger(MACTest.class);

	private final SecureRandom random = new SecureRandom();

	@Test
	public void testAllSupportedMACs() throws Exception
	{
		final byte[] orig = new byte[1024 + random.nextInt(10240)];
		random.nextBytes(orig);
		for (final String macAlgorithmName : CryptoRegistry.getInstance().getSupportedMACAlgorithms()) {
			logger.debug("------------------------------------------------------------------------");
			logger.debug("testAllSupportedMACs: macAlgorithmName={}", macAlgorithmName);
			final MACCalculator macCalculator1 = CryptoRegistry.getInstance().createMACCalculator(macAlgorithmName, true);
			Assert.assertNotNull("CryptoRegistry.createMACCalculator(...) returned null for macAlgorithmName=" + macAlgorithmName, macCalculator1);
			final byte[] mac1 = new byte[macCalculator1.getMacSize()];
			macCalculator1.update(orig, 0, orig.length);
			macCalculator1.doFinal(mac1, 0);

			byte[] macKey = null;
			byte[] macIV = null;
			final CipherParameters macParams = macCalculator1.getParameters();
			if (macParams instanceof ParametersWithIV) {
				final ParametersWithIV piv = (ParametersWithIV) macParams;
				macIV = piv.getIV();
				macKey = ((KeyParameter)piv.getParameters()).getKey();
			}
			else if (macParams instanceof KeyParameter) {
				macKey = ((KeyParameter)macParams).getKey();
			}
			else
				throw new IllegalStateException("macParams type unsupported type=" + (macParams == null ? null : macParams.getClass().getName()) + " macParams=" + macParams);

			logger.debug("testAllSupportedMACs: macKey={}", HashUtil.encodeHexStr(macKey));
			logger.debug("testAllSupportedMACs: macIV={}", macIV == null ? null : HashUtil.encodeHexStr(macIV));

			final MACCalculator macCalculator2 = CryptoRegistry.getInstance().createMACCalculator(macAlgorithmName, false);
			CipherParameters macCipherParameters2 = null;
			if (macIV == null)
				macCipherParameters2 = new KeyParameter(macKey);
			else
				macCipherParameters2 = new ParametersWithIV(new KeyParameter(macKey), macIV);

			final byte[] mac2 = new byte[macCalculator2.getMacSize()];
			macCalculator2.init(macCipherParameters2);
			macCalculator2.update(orig, 0, orig.length);
			macCalculator2.doFinal(mac2, 0);

			logger.debug("testAllSupportedMACs: mac1={}", HashUtil.encodeHexStr(mac1));
			logger.debug("testAllSupportedMACs: mac2={}", HashUtil.encodeHexStr(mac2));

			Assert.assertArrayEquals(mac1, mac2);



			if (macIV != null) {
				final byte[] wrongMACIV = macIV.clone();
				random.nextBytes(wrongMACIV);
				final MACCalculator macCalculator3 = CryptoRegistry.getInstance().createMACCalculator(macAlgorithmName, false);
				final CipherParameters macCipherParameters3 = new ParametersWithIV(new KeyParameter(macKey), wrongMACIV);
				final byte[] mac3 = new byte[macCalculator3.getMacSize()];
				macCalculator3.init(macCipherParameters3);
				macCalculator3.update(orig, 0, orig.length);
				macCalculator3.doFinal(mac3, 0);

				logger.debug("testAllSupportedMACs: wrongIV={}", HashUtil.encodeHexStr(wrongMACIV));
				logger.debug("testAllSupportedMACs: wrongMAC={}", HashUtil.encodeHexStr(mac3));
				Assert.assertFalse("Passed different MAC-IV, but still got the same MAC! It seems the IV is ignored!", Arrays.equals(mac1, mac3));
			}


			{
				final byte[] wrongMACKey = macKey.clone();
				random.nextBytes(wrongMACKey);
				final MACCalculator macCalculator4 = CryptoRegistry.getInstance().createMACCalculator(macAlgorithmName, false);

				CipherParameters macCipherParameters4 = null;
				if (macIV == null)
					macCipherParameters4 = new KeyParameter(wrongMACKey);
				else
					macCipherParameters4 = new ParametersWithIV(new KeyParameter(wrongMACKey), macIV);

				final byte[] mac4 = new byte[macCalculator4.getMacSize()];
				macCalculator4.init(macCipherParameters4);
				macCalculator4.update(orig, 0, orig.length);
				macCalculator4.doFinal(mac4, 0);

				logger.debug("testAllSupportedMACs: wrongKey={}", HashUtil.encodeHexStr(wrongMACKey));
				logger.debug("testAllSupportedMACs: wrongMAC={}", HashUtil.encodeHexStr(mac4));
				Assert.assertFalse("Passed different MAC-keys, but still got the same MAC! It seems the key is ignored!", Arrays.equals(mac1, mac4));
			}


			{
				final byte[] wrongData = orig.clone();

				// change one arbitrary bit in the data
				final int byteIdx = random.nextInt(wrongData.length);
				final int bitIdx = random.nextInt(8);

				int v = wrongData[byteIdx] & 0xff;
				v ^= 1 << bitIdx;
				wrongData[byteIdx] = (byte)v;

				final byte[] mac5 = new byte[macCalculator2.getMacSize()];
				macCalculator2.update(wrongData, 0, wrongData.length);
				macCalculator2.doFinal(mac5, 0);

				logger.debug("testAllSupportedMACs: MACforWrongData={}", HashUtil.encodeHexStr(mac5));
				Assert.assertFalse("Passed different data, but still got the same MAC!", Arrays.equals(mac1, mac5));


				final byte[] mac6 = new byte[macCalculator2.getMacSize()];
				macCalculator2.update(orig, 0, orig.length);
				macCalculator2.doFinal(mac6, 0);
				Assert.assertArrayEquals(mac1, mac6);
			}
		}
	}

}
