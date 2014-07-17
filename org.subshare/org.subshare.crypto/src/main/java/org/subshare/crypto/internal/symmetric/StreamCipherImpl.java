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

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.Grain128Engine;
import org.bouncycastle.crypto.engines.Grainv1Engine;
import org.bouncycastle.crypto.engines.HC128Engine;
import org.bouncycastle.crypto.engines.HC256Engine;
import org.bouncycastle.crypto.engines.ISAACEngine;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.engines.Salsa20Engine;
import org.subshare.crypto.AbstractCipher;
import org.subshare.crypto.CipherOperationMode;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class StreamCipherImpl
extends AbstractCipher
{
	private final StreamCipher delegate;

	public StreamCipherImpl(final String transformation, final StreamCipher delegate) {
		super(transformation);
		this.delegate = delegate;
	}

	@Override
	public void _init(final CipherOperationMode mode, final CipherParameters parameters)
	throws IllegalArgumentException
	{
		delegate.init(CipherOperationMode.ENCRYPT == mode, parameters);
	}

	@Override
	public void reset() {
		delegate.reset();
	}

	@Override
	public int getInputBlockSize() {
		return 1;
	}

	@Override
	public int getOutputBlockSize() {
		return 1;
	}

	@Override
	public int getUpdateOutputSize(final int length) {
		return length;
	}

	@Override
	public int getOutputSize(final int length) {
		return length;
	}

	@Override
	public int update(final byte in, final byte[] out, final int outOff)
	throws DataLengthException, IllegalStateException, CryptoException
	{
		out[outOff] = delegate.returnByte(in);
		return 1;
	}

	@Override
	public int update(final byte[] in, final int inOff, final int inLen, final byte[] out, final int outOff)
	throws DataLengthException, IllegalStateException, CryptoException
	{
		delegate.processBytes(in, inOff, inLen, out, outOff);
		return inLen;
	}

	@Override
	public int doFinal(final byte[] out, final int outOff)
	throws DataLengthException, IllegalStateException, CryptoException
	{
		return 0;
	}

	private int ivSize = -1;

	@Override
	public int getIVSize()
	{
		if (ivSize < 0) {
			if (delegate instanceof Grainv1Engine)
				ivSize = 8;
			else if (delegate instanceof Grain128Engine)
				ivSize = 12;
			else if (delegate instanceof HC128Engine)
				ivSize = 16;
			else if (delegate instanceof HC256Engine)
				ivSize = 32;
			else if (delegate instanceof ISAACEngine)
				ivSize = 0;
			else if (delegate instanceof RC4Engine)
				ivSize = 0;
			else if (delegate instanceof Salsa20Engine)
				ivSize = 8;
			else
				throw new UnsupportedOperationException("For this delegate cipher type, this operation is not yet supported!");
		}
		return ivSize;
	}

//	@Override
//	public AsymmetricCipherKeyPairGenerator createKeyPairGenerator(boolean initWithDefaults)
//	throws UnsupportedOperationException
//	{
//		throw new UnsupportedOperationException("This is a SYMMETRIC cipher! Cannot get an appropriate key pair generator!");
//	}
//
//	@Override
//	public SecretKeyGenerator createSecretKeyGenerator(boolean initWithDefaults)
//	{
//		String algorithmName = CryptoRegistry.splitTransformation(getTransformation())[0];
//		try {
//			return CryptoRegistry.getInstance().createSecretKeyGenerator(algorithmName, initWithDefaults);
//		} catch (NoSuchAlgorithmException e) {
//			throw new RuntimeException(e); // We should be able to provide an SecretKeyGenerator for every Cipher => RuntimeException
//		}
//	}
}
