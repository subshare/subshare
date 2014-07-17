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
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.DataLengthException;

/**
 * Abstract base class for {@link Cipher} implementations. Implementors should not implement
 * the <code>Cipher</code> interface directly, but sub-class this class instead.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public abstract class AbstractCipher implements Cipher
{
	private final String transformation;

	protected AbstractCipher(final String transformation)
	{
		if (transformation == null)
			throw new IllegalArgumentException("transformation == null");

		this.transformation = transformation;
	}

	@Override
	public final String getTransformation() {
		return transformation;
	}

	@Override
	public byte[] doFinal(final byte[] in)
	throws DataLengthException, IllegalStateException, CryptoException
	{
		final byte[] out = new byte[getOutputSize(in.length)];
		int outOff = update(in, 0, in.length, out, 0);
		outOff += doFinal(out, outOff);

		if (outOff == out.length)
			return out;

		final byte[] truncOut = new byte[outOff];
		System.arraycopy(out, 0, truncOut, 0, truncOut.length);
		return truncOut;
	}

	private CipherOperationMode mode;

	private CipherParameters parameters;

	@Override
	public final void init(final CipherOperationMode mode, final CipherParameters parameters)
	throws IllegalArgumentException
	{
		if (mode == null)
			throw new IllegalArgumentException("mode == null");

		if (parameters == null)
			throw new IllegalArgumentException("parameters == null");

		this.mode = mode;
		this.parameters = parameters;

		_init(mode, parameters);
	}

	@Override
	public CipherOperationMode getMode() {
		return mode;
	}

	@Override
	public CipherParameters getParameters() {
		return parameters;
	}

	protected abstract void _init(CipherOperationMode mode, CipherParameters parameters)
	throws IllegalArgumentException;
}
