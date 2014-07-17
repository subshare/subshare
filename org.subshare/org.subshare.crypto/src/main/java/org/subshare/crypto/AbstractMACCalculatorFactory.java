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

import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

/**
 * <p>
 * Abstract base class for implementing a {@link MACCalculatorFactory}.
 * </p><p>
 * Implementors should subclass this class instead of directly implementing the interface
 * <code>MACCalculatorFactory</code>.
 * </p>
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public abstract class AbstractMACCalculatorFactory implements MACCalculatorFactory
{
	private String algorithmName;

	@Override
	public String getAlgorithmName() {
		return algorithmName;
	}

	@Override
	public void setAlgorithmName(String algorithmName)
	{
		if (this.algorithmName != null && !this.algorithmName.equals(algorithmName))
			throw new IllegalStateException("this.algorithmName is already assigned! Cannot modify!");

		if (algorithmName == null)
			throw new IllegalArgumentException("algorithmName == null");

		this.algorithmName = algorithmName;
	}

	@Override
	public MACCalculator createMACCalculator(boolean initWithDefaults)
	{
		MACCalculator macCalculator = _createMACCalculator();

		if (initWithDefaults) {
			SecureRandom random = new SecureRandom();
			byte[] key = new byte[macCalculator.getKeySize()];
			random.nextBytes(key);
			if (macCalculator.getIVSize() > 0) {
				byte[] iv = new byte[macCalculator.getIVSize()];
				random.nextBytes(iv);
				macCalculator.init(new ParametersWithIV(new KeyParameter(key), iv));
			}
			else
				macCalculator.init(new KeyParameter(key));
		}

		macCalculator.setAlgorithmName(getAlgorithmName());

		return macCalculator;
	}

	protected abstract MACCalculator _createMACCalculator();
}
