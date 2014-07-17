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

import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;

/**
 * <p>
 * Factory creating instances of {@link AsymmetricCipherKeyPairGenerator}.
 * </p><p>
 * Implementations of this interface are used by {@link CryptoRegistry#createKeyPairGenerator(String, boolean)}
 * to provide instances of <code>AsymmetricCipherKeyPairGenerator</code>.
 * </p>
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public interface AsymmetricCipherKeyPairGeneratorFactory
{
	/**
	 * Create and optionally initialise a new instance of {@link AsymmetricCipherKeyPairGenerator}.
	 * @param initWithDefaults whether to
	 * {@link AsymmetricCipherKeyPairGenerator#init(org.bouncycastle.crypto.KeyGenerationParameters) initialise} the <code>AsymmetricCipherKeyPairGenerator</code> with default values
	 * so that it can be used immediately as-is.
	 * @return a new instance of {@link AsymmetricCipherKeyPairGenerator} (iff <code>initWithDefaults==true</code> ready-to-use;
	 * otherwise requiring {@link AsymmetricCipherKeyPairGenerator#init(org.bouncycastle.crypto.KeyGenerationParameters) initialisation}
	 * before it can be used).
	 */
	AsymmetricCipherKeyPairGenerator createAsymmetricCipherKeyPairGenerator(boolean initWithDefaults);

	/**
	 * Get the name of the encryption algorithm for which keys should be generated. For example "RSA".
	 * See <a target="_blank" href="http://cumulus4j.org/${project.version}/documentation/supported-algorithms.html">Supported algorithms</a>
	 * for a list of supported algorithms.
	 * @return the name of the encryption algorithm for which keys are to be generated.
	 */
	String getAlgorithmName();

	/**
	 * Set the name of the encryption algorithm for which keys are to be generated. This method
	 * should never be called by API consumers! It should throw an {@link IllegalStateException}, if
	 * it is called again, after an algorithm name was already set before.
	 * @param algorithmName the name of the encryption algorithm; never <code>null</code>.
	 * @see #getAlgorithmName()
	 */
	void setAlgorithmName(String algorithmName);
}
