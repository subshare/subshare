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

	/**
	 * This value should be a Fermat number. 0x10001 (F4) is current recommended value. 3 (F1) is known to be safe also.
	 * 3, 5, 17, 257, 65537, 4294967297, 18446744073709551617,
	 * <p>
	 * Practically speaking, Windows does not tolerate public exponents which do not fit in a 32-bit unsigned integer.
	 * Using e=3 or e=65537 works "everywhere".
	 * <p>
	 * See: <a href="http://stackoverflow.com/questions/11279595/rsa-public-exponent-defaults-to-65537-what-should-this-value-be-what-are-the">stackoverflow: RSA Public exponent defaults to 65537. ... What are the impacts of my choices?</a>
	 */
	private static final BigInteger defaultPublicExponent = BigInteger.valueOf(0x10001);

	/**
	 * How certain do we want to be that the chosen primes are really primes.
	 * <p>
	 * The higher this number, the more tests are done to make sure they are primes (and not composites).
	 * <p>
	 * See: <a href="http://crypto.stackexchange.com/questions/3114/what-is-the-correct-value-for-certainty-in-rsa-key-pair-generation">What is the correct value for “certainty” in RSA key pair generation?</a>
	 * and
	 * <a href="http://crypto.stackexchange.com/questions/3126/does-a-high-exponent-compensate-for-a-low-degree-of-certainty?lq=1">Does a high exponent compensate for a low degree of certainty?</a>
	 */
	private static final int defaultCertainty = 12;

	private SecureRandom random;

	@Override
	public AsymmetricCipherKeyPairGenerator createAsymmetricCipherKeyPairGenerator(final boolean initWithDefaults)
	{
		final RSAKeyPairGenerator generator = new RSAKeyPairGenerator();

		if (initWithDefaults) {
			if (random == null)
				random = new SecureRandom();

			generator.init(new RSAKeyGenerationParameters(defaultPublicExponent, random, 4096, defaultCertainty));
		}

		return generator;
	}
}
