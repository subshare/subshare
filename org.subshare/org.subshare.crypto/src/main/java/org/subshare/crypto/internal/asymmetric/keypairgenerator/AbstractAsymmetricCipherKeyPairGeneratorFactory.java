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

import org.subshare.crypto.AsymmetricCipherKeyPairGeneratorFactory;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public abstract class AbstractAsymmetricCipherKeyPairGeneratorFactory
implements AsymmetricCipherKeyPairGeneratorFactory
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
}
