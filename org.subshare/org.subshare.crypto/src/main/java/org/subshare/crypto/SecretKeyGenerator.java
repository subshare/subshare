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

import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * Generator for secret keys to be used in <a target="_blank" href="http://en.wikipedia.org/wiki/Symmetric_encryption">symmetric encryption</a>.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public interface SecretKeyGenerator
{
	/**
	 * Initialise this <code>SecretKeyGenerator</code>. Implementations should
	 * be able to initialise with defaults, if no parameters are given (i.e. <code>params</code> being <code>null</code>).
	 * Usually, defaults mean to generate 256 bit keys.
	 * @param params the parameters or <code>null</code>, if defaults should be used.
	 */
	void init(KeyGenerationParameters params);

	/**
	 * Generate random a secret key. Throws an {@link IllegalStateException}, if
	 * {@link #init(KeyGenerationParameters)} was not yet called.
	 * @return the newly created secret key; never <code>null</code>.
	 */
	KeyParameter generateKey();
}
