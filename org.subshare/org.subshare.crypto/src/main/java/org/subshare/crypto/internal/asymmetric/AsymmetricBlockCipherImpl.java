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
package org.subshare.crypto.internal.asymmetric;

import org.bouncycastle.crypto.BufferedAsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;
import org.subshare.crypto.AbstractCipher;
import org.subshare.crypto.CipherOperationMode;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class AsymmetricBlockCipherImpl
extends AbstractCipher
{
	private final BufferedAsymmetricBlockCipher delegate;

	public AsymmetricBlockCipherImpl(final String transformation, final BufferedAsymmetricBlockCipher delegate) {
		super(transformation);
		this.delegate = delegate;
	}

	@Override
	public void _init(final CipherOperationMode mode, final CipherParameters parameters) {
		delegate.init(CipherOperationMode.ENCRYPT == mode, parameters);
	}

	@Override
	public int getInputBlockSize() {
		return delegate.getInputBlockSize();
	}

	@Override
	public int getOutputBlockSize() {
		return delegate.getOutputBlockSize();
	}

	@Override
	public void reset() {
		// does not exist in delegate => not necessary?!
	}

	@Override
	public int getUpdateOutputSize(final int length) {
		return getOutputSize(length); // this is not correct and very pessimistic, but for now, it is at least sth. that shouldn't produce any errors (the result should be >= the real value).
	}

	@Override
	public int getOutputSize(final int length) {
		return getOutputBlockSize(); // Copied this from org.bouncycastle.jce.provider.JCERSACipher.
	}

	@Override
	public int update(final byte in, final byte[] out, final int outOff)
	throws DataLengthException, IllegalStateException, CryptoException
	{
		delegate.processByte(in);
		return 0;
	}

	@Override
	public int update(final byte[] in, final int inOff, final int inLen, final byte[] out, final int outOff)
	throws DataLengthException, IllegalStateException, CryptoException
	{
		delegate.processBytes(in, inOff, inLen);
		return 0;
	}

	@Override
	public int doFinal(final byte[] out, final int outOff)
	throws DataLengthException, IllegalStateException, CryptoException
	{
		final byte[] encrypted = delegate.doFinal();
		System.arraycopy(encrypted, 0, out, outOff, encrypted.length);
		return encrypted.length;
	}

	@Override
	public int getIVSize() {
		return 0;
	}

//	@Override
//	public AsymmetricCipherKeyPairGenerator createKeyPairGenerator(boolean initWithDefaults)
//	{
//		String algorithmName = CryptoRegistry.splitTransformation(getTransformation())[0];
//		try {
//			return CryptoRegistry.getInstance().createKeyPairGenerator(algorithmName, initWithDefaults);
//		} catch (NoSuchAlgorithmException e) {
//			throw new RuntimeException(e); // We should be able to provide an Asymmetric...KeyPairGenerator for every Cipher => RuntimeException
//		}
//	}
//
//	@Override
//	public SecretKeyGenerator createSecretKeyGenerator(boolean initWithDefaults)
//	throws UnsupportedOperationException
//	{
//		throw new UnsupportedOperationException("This is an ASYMMETRIC cipher! Cannot get an appropriate secret key generator.");
//	}
}
