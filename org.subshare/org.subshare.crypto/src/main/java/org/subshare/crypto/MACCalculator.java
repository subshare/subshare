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

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Mac;

/**
 * <p>
 * A <code>MACCalculator</code> calculates <a target="_blank" href="http://en.wikipedia.org/wiki/Message_authentication_code">message
 * authentication codes</a>.
 * </p><p>
 * Use {@link CryptoRegistry#createMACCalculator(String, boolean)} to obtain a <code>MACCalculator</code> instance.
 * </p><p>
 * <b>Important: <code>MACCalculator</code>s are not thread-safe!</b>
 * </p>
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public interface MACCalculator
extends Mac
{
	void setAlgorithmName(String algorithmName);

	CipherParameters getParameters();

	/**
	 * Get the required size of the key (in bytes).
	 * @return the required size of the key (in bytes).
	 */
	int getKeySize();

	/**
	 * Get the required size of the IV (in bytes). If a MAC supports multiple sizes, this is the optimal (most secure) IV size.
	 * If a MAC supports no IV, this is 0.
	 * @return the required size of the IV.
	 */
	int getIVSize();

	/**
	 * Convenience method to process the complete input byte array at once.
	 * @param in the input to calculate a MAC for.
	 * @return the MAC.
	 * @throws IllegalStateException if the <code>MACCalculator</code> isn't initialised.
	 */
	byte[] doFinal(byte[] in)
	throws IllegalStateException;
}
