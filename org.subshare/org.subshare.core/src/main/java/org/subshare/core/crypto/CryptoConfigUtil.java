package org.subshare.core.crypto;

import org.subshare.core.sign.SignerTransformation;

import co.codewizards.cloudstore.core.config.Config;
import co.codewizards.cloudstore.core.oio.File;

public class CryptoConfigUtil {

	public static final String CONFIG_KEY_BACKDATING_MAX_PERMISSION_VALID_TO_AGE = "backdatingMaxPermissionValidToAge";
	public static final long CONFIG_DEFAULT_VALUE_BACKDATING_MAX_PERMISSION_VALID_TO_AGE = 3 * 24 * 3600 * 1000; // 3 days

	private CryptoConfigUtil() { }

	/**
	 * Gets the configured/preferred transformation for symmetric encryption.
	 * @return the configured/preferred transformation for symmetric encryption. Never <code>null</code>.
	 */
	public static CipherTransformation getSymmetricCipherTransformation() {
		return Config.getInstance().getPropertyAsEnum(
				CipherTransformation.CONFIG_KEY_SYMMETRIC, CipherTransformation.CONFIG_DEFAULT_VALUE_SYMMETRIC);
	}

	/**
	 * Gets the configured/preferred transformation for asymmetric encryption.
	 * <p>
	 * TODO the asymmetric algorithm is dependent on the user's key type. We cannot encrypt using RSA, if the user's key is DSA, for example. We need thus a better strategy!
	 * @return the configured/preferred transformation for asymmetric encryption. Never <code>null</code>.
	 */
	public static CipherTransformation getAsymmetricCipherTransformation() {
		return Config.getInstance().getPropertyAsEnum(
				CipherTransformation.CONFIG_KEY_ASYMMETRIC, CipherTransformation.CONFIG_DEFAULT_VALUE_ASYMMETRIC);
	}

	/**
	 * Gets the configured/preferred transformation for signing.
	 * <p>
	 * TODO the signing algorithm is dependent on the user's key type. We cannot use RSA/SHA512 with a DSA key, for example. We need therefore a better strategy!
	 * @return the configured/preferred transformation for signing. Never <code>null</code>.
	 */
	public static SignerTransformation getSignerTransformation() {
		return Config.getInstance().getPropertyAsEnum(
				SignerTransformation.CONFIG_KEY, SignerTransformation.CONFIG_DEFAULT_VALUE);
	}

	public static long getBackdatingMaxPermissionValidToAge(final File file) {
		final Config config = file.isDirectory() ? Config.getInstanceForDirectory(file) : Config.getInstanceForFile(file);
		return config.getPropertyAsLong(
				CONFIG_KEY_BACKDATING_MAX_PERMISSION_VALID_TO_AGE, CONFIG_DEFAULT_VALUE_BACKDATING_MAX_PERMISSION_VALID_TO_AGE);
	}
}
