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
package org.subshare.crypto.internal.symmetric.mode;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.modes.OFBBlockCipher;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class C4jOFBBlockCipher extends OFBBlockCipher
{
	private static int determineBitBlockSize(final BlockCipher engine, final String modeName)
	{
		if (modeName.length() != 3)
    {
        final int wordSize = Integer.parseInt(modeName.substring(3));
        return wordSize;
    }
		else
			return 8 * engine.getBlockSize();
	}

	public C4jOFBBlockCipher(final BlockCipher engine, final String modeName) {
		super(engine, determineBitBlockSize(engine, modeName));
	}
}
