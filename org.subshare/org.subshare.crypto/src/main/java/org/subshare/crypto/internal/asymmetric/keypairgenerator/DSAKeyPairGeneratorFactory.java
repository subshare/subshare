package org.subshare.crypto.internal.asymmetric.keypairgenerator;

import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.generators.DSAKeyPairGenerator;
import org.bouncycastle.crypto.generators.DSAParametersGenerator;
import org.bouncycastle.crypto.params.DSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.DSAParameters;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class DSAKeyPairGeneratorFactory
extends AbstractAsymmetricCipherKeyPairGeneratorFactory
{
	public DSAKeyPairGeneratorFactory() {
		setAlgorithmName("DSA");
	}

	@Override
	public AsymmetricCipherKeyPairGenerator createAsymmetricCipherKeyPairGenerator(final boolean initWithDefaults) {
		final DSAKeyPairGenerator generator = new DSAKeyPairGenerator();

		if (initWithDefaults) {
			/*
			 * How certain do we want to be that the chosen primes are really primes.
			 * <p>
			 * The higher this number, the more tests are done to make sure they are primes (and not composites).
			 * <p>
			 * See: <a href="http://crypto.stackexchange.com/questions/3114/what-is-the-correct-value-for-certainty-in-rsa-key-pair-generation">What is the correct value for “certainty” in RSA key pair generation?</a>
			 * and
			 * <a href="http://crypto.stackexchange.com/questions/3126/does-a-high-exponent-compensate-for-a-low-degree-of-certainty?lq=1">Does a high exponent compensate for a low degree of certainty?</a>
			 */
			final int certainty = 12;

			final SecureRandom random = new SecureRandom();

			final DSAParametersGenerator pGen = new DSAParametersGenerator();
			pGen.init(4096, certainty, random);
			final DSAParameters dsaParameters = pGen.generateParameters();
			final DSAKeyGenerationParameters dsaKeyGenerationParameters = new DSAKeyGenerationParameters(random, dsaParameters);
			generator.init(dsaKeyGenerationParameters);
		}
		return generator;
	}
}
