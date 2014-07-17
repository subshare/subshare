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

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.CCMBlockCipher;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.modes.OFBBlockCipher;
import org.subshare.crypto.AbstractCipher;
import org.subshare.crypto.CipherOperationMode;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class AEADBlockCipherImpl extends AbstractCipher
{
	private AEADBlockCipher delegate;

	public AEADBlockCipherImpl(String transformation, AEADBlockCipher delegate) {
		super(transformation);
		this.delegate = delegate;
	}

	@Override
	public void reset() {
		delegate.reset();
	}

	@Override
	public int getInputBlockSize() {
		return delegate.getUnderlyingCipher().getBlockSize();
	}

	@Override
	public int getOutputBlockSize() {
		return delegate.getUnderlyingCipher().getBlockSize();
	}

	@Override
	public int getUpdateOutputSize(int length) {
		return delegate.getUpdateOutputSize(length);
	}

	@Override
	public int getOutputSize(int length) {
		return delegate.getOutputSize(length);
	}

	@Override
	public int update(byte in, byte[] out, int outOff)
	throws DataLengthException, IllegalStateException, CryptoException
	{
		return delegate.processByte(in, out, outOff);
	}

	@Override
	public int update(byte[] in, int inOff, int inLen, byte[] out, int outOff)
	throws DataLengthException, IllegalStateException, CryptoException
	{
		return delegate.processBytes(in, inOff, inLen, out, outOff);
	}

	@Override
	public int doFinal(byte[] out, int outOff)
	throws DataLengthException, IllegalStateException, CryptoException
	{
		return delegate.doFinal(out, outOff);
	}

	private int ivSize = -1;

	@Override
	public int getIVSize()
	{
		int ivSize = this.ivSize;
		if (ivSize < 0) {
			BlockCipher underlyingCipher = delegate.getUnderlyingCipher();

			if (underlyingCipher instanceof CFBBlockCipher)
				ivSize = ((CFBBlockCipher)underlyingCipher).getUnderlyingCipher().getBlockSize();
			else if (underlyingCipher instanceof OFBBlockCipher)
				ivSize = ((OFBBlockCipher)underlyingCipher).getUnderlyingCipher().getBlockSize();
			else
				ivSize = underlyingCipher.getBlockSize();

			if (delegate instanceof CCMBlockCipher)
				--ivSize;

			this.ivSize = ivSize;
		}

		return ivSize;
	}

	@Override
	protected void _init(CipherOperationMode mode, CipherParameters parameters)
	throws IllegalArgumentException
	{
		delegate.init(CipherOperationMode.ENCRYPT == mode, parameters);
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
//			return CryptoRegistry.sharedInstance().createSecretKeyGenerator(algorithmName, initWithDefaults);
//		} catch (NoSuchAlgorithmException e) {
//			throw new RuntimeException(e); // We should be able to provide an SecretKeyGenerator for every Cipher => RuntimeException
//		}
//	}
}
