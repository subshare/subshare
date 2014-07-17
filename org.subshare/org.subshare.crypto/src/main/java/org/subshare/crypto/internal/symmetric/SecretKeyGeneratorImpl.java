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
package org.subshare.crypto.internal.symmetric;

import java.security.SecureRandom;

import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.subshare.crypto.SecretKeyGenerator;

/**
 * Default implementation of {@link SecretKeyGenerator}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class SecretKeyGeneratorImpl implements SecretKeyGenerator
{
	private int strength;
	private int strengthInBytes;
	private SecureRandom random;

	@Override
	public void init(final KeyGenerationParameters params)
	{
		strength = 0;
		random = null;

		if (params != null) {
			strength = params.getStrength();
			random = params.getRandom();
		}

		if (strength < 1)
			strength = 256;

		if (random == null)
			random = new SecureRandom();

		strengthInBytes = (strength + 7) / 8;
	}

	@Override
	public KeyParameter generateKey()
	{
		if (random == null)
			throw new IllegalStateException("init(...) was not yet called!");

		final byte[] key = new byte[strengthInBytes];
		random.nextBytes(key);
		return new KeyParameter(key);
	}

}
