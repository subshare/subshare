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
package org.subshare.crypto.internal.asymmetric.keypairgenerator;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class RSAKeyPairGeneratorFactory
extends AbstractAsymmetricCipherKeyPairGeneratorFactory
{
	public RSAKeyPairGeneratorFactory() {
		setAlgorithmName("RSA");
	}

	private static final BigInteger defaultPublicExponent = BigInteger.valueOf(0x10001);
	private static final int defaultTests = 12;

	private SecureRandom random;

	@Override
	public AsymmetricCipherKeyPairGenerator createAsymmetricCipherKeyPairGenerator(boolean initWithDefaults)
	{
		RSAKeyPairGenerator generator = new RSAKeyPairGenerator();

		if (initWithDefaults) {
			if (random == null)
				random = new SecureRandom();

			generator.init(new RSAKeyGenerationParameters(defaultPublicExponent, random, 4096, defaultTests));
		}

		return generator;
	}
}
