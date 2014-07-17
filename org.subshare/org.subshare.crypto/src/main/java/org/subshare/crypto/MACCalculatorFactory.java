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

/**
 * <p>
 * Factory creating instances of {@link MACCalculator}.
 * </p><p>
 * Implementations of this interface are used by {@link CryptoRegistry#createMACCalculator(String, boolean)}
 * to provide instances of <code>MACCalculator</code>.
 * </p><p>
 * Note: Implementors should subclass {@link AbstractMACCalculatorFactory} instead of directly implementing this
 * interface.
 * </p>
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public interface MACCalculatorFactory
{
	/**
	 * <p>
	 * Create a new instance of {@link MACCalculator} and optionally
	 * {@link MACCalculator#init(org.bouncycastle.crypto.CipherParameters) initialise} it.
	 * </p>
	 *
	 * @param initWithDefaults whether to
	 * {@link MACCalculator#init(org.bouncycastle.crypto.CipherParameters) initialise} the <code>MACCalculator</code> with default values
	 * so that it can be used immediately as-is.
	 * @return a new instance of {@link MACCalculator} (iff <code>initWithDefaults==true</code> ready-to-use;
	 * otherwise requiring {@link MACCalculator#init(org.bouncycastle.crypto.CipherParameters) initialisation}
	 * before it can be used).
	 */
	MACCalculator createMACCalculator(boolean initWithDefaults);

	/**
	 * Get the name of the MAC algorithm implemented by the {@link MACCalculator} created by this factory.
	 * See <a target="_blank" href="http://cumulus4j.org/${project.version}/documentation/supported-algorithms.html">Supported algorithms</a>
	 * for a list of supported algorithms.
	 * @return the name of the MAC algorithm.
	 */
	String getAlgorithmName();

	/**
	 * Set the name of the MAC algorithm. This method is called once and should throw an {@link IllegalStateException}
	 * if it is called again.
	 * @param algorithmName the name of the MAC algorithm; never <code>null</code>.
	 * @see #getAlgorithmName()
	 */
	void setAlgorithmName(String algorithmName);
}
