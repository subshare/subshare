package org.subshare.core.crypto;

import static org.subshare.core.crypto.CryptoConfigUtil.*;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subshare.crypto.CryptoRegistry;
import org.subshare.crypto.SecretKeyGenerator;

/**
 * @deprecated This class sucks - it should be replaced by nicer constructions!
 */
@Deprecated
public class KeyFactory {

	private static final class Holder {
		static final KeyFactory instance = new KeyFactory();
	}

	public static KeyFactory getInstance() {
		return Holder.instance;
	}

	private KeyFactory() { }

	private static final Logger logger = LoggerFactory.getLogger(KeyFactory.class);

	public static final SecureRandom secureRandom = new SecureRandom();

	public KeyParameter createSymmetricKey() {
		final CipherTransformation symmetricCipherTransformation = getSymmetricCipherTransformation();
		final String engine = CryptoRegistry.splitTransformation(symmetricCipherTransformation.getTransformation())[0];

		final SecretKeyGenerator secretKeyGenerator;
		try {
			secretKeyGenerator = CryptoRegistry.getInstance().createSecretKeyGenerator(engine, true);
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		final KeyParameter key = secretKeyGenerator.generateKey();
		return key;
	}

	public AsymmetricCipherKeyPair createAsymmetricKeyPair(KeyGenerationParameters keyGenerationParameters) {
		final long startTimestamp = System.currentTimeMillis();
		final CipherTransformation asymmetricCipherTransformation = getAsymmetricCipherTransformation();
		final String engine = CryptoRegistry.splitTransformation(asymmetricCipherTransformation.getTransformation())[0];

		if (Boolean.parseBoolean(System.getProperty("testEnvironment")))
			keyGenerationParameters = new RSAKeyGenerationParameters(BigInteger.valueOf(0x10001), secureRandom, 1024, 12);

		final AsymmetricCipherKeyPairGenerator keyPairGenerator;
		try {
			keyPairGenerator = CryptoRegistry.getInstance().createKeyPairGenerator(engine, keyGenerationParameters == null);
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		if (keyGenerationParameters != null)
			keyPairGenerator.init(keyGenerationParameters);

		final AsymmetricCipherKeyPair keyPair = keyPairGenerator.generateKeyPair();

		logger.info("Creating asymmetric key pair with {} took {} ms.",
				keyGenerationParameters == null ? "defaults" : toString(keyGenerationParameters),
				System.currentTimeMillis() - startTimestamp);
		return keyPair;
	}

	private static final String toString(final KeyGenerationParameters keyGenerationParameters) {
		if (keyGenerationParameters instanceof RSAKeyGenerationParameters) {
			final RSAKeyGenerationParameters kgp = (RSAKeyGenerationParameters) keyGenerationParameters;
			return String.format("publicExponent='%s' strength='%s' certainty='%s'",
					kgp.getPublicExponent(),
					kgp.getStrength(),
					kgp.getCertainty());
		}
		return String.valueOf(keyGenerationParameters);
	}

	public AsymmetricCipherKeyPair createAsymmetricKeyPair() {
		return createAsymmetricKeyPair(null);
	}
}
