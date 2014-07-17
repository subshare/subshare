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
package org.subshare.crypto.internal.mac;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD2Digest;
import org.bouncycastle.crypto.digests.MD4Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.RIPEMD128Digest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA224Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.digests.TigerDigest;
import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.engines.RC2Engine;
import org.bouncycastle.crypto.macs.CBCBlockCipherMac;
import org.bouncycastle.crypto.macs.CFBBlockCipherMac;
import org.bouncycastle.crypto.macs.GOST28147Mac;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.macs.ISO9797Alg3Mac;
import org.bouncycastle.crypto.macs.OldHMac;
import org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import org.subshare.crypto.AbstractMACCalculatorFactory;
import org.subshare.crypto.MACCalculator;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public abstract class MACCalculatorFactoryImpl
extends AbstractMACCalculatorFactory
{
	public static class DES
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final BlockCipher cipher = new DESEngine();
			return new MACCalculatorImpl(new CBCBlockCipherMac(cipher), cipher.getBlockSize(), cipher.getBlockSize());
		}
	}

	public static class DES64
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final BlockCipher cipher = new DESEngine();
			return new MACCalculatorImpl(new CBCBlockCipherMac(cipher, 64), cipher.getBlockSize(), cipher.getBlockSize());
		}
	}

	public static class RC2
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final BlockCipher cipher = new RC2Engine();
			return new MACCalculatorImpl(new CBCBlockCipherMac(cipher), cipher.getBlockSize(), cipher.getBlockSize());
		}
	}

	public static class GOST28147
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			return new MACCalculatorImpl(new GOST28147Mac(), 32, 0); // IV not supported - throws exception when passing ParameterWithIV
		}
	}

	public static class DESCFB8
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final BlockCipher cipher = new DESEngine();
			return new MACCalculatorImpl(new CFBBlockCipherMac(cipher), cipher.getBlockSize(), cipher.getBlockSize());
		}
	}

	public static class RC2CFB8
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final BlockCipher cipher = new RC2Engine();
			return new MACCalculatorImpl(new CFBBlockCipherMac(cipher), cipher.getBlockSize(), cipher.getBlockSize());
		}
	}

	public static class DES9797Alg3with7816d4
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final BlockCipher cipher = new DESEngine();
			return new MACCalculatorImpl(new ISO9797Alg3Mac(cipher, new ISO7816d4Padding()), 24, cipher.getBlockSize());
		}
	}

	public static class DES9797Alg3
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final BlockCipher cipher = new DESEngine();
			return new MACCalculatorImpl(new ISO9797Alg3Mac(cipher), 24, cipher.getBlockSize());
		}
	}

	public static class MD2
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final Digest digest = new MD2Digest();
			return new MACCalculatorImpl(new HMac(digest), digest.getDigestSize(), 0); // IV not supported - throws exception when passing ParameterWithIV
		}
	}

	public static class MD4
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final Digest digest = new MD4Digest();
			return new MACCalculatorImpl(new HMac(digest), digest.getDigestSize(), 0); // IV not supported - throws exception when passing ParameterWithIV
		}
	}

	public static class MD5
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final MD5Digest digest = new MD5Digest();
			return new MACCalculatorImpl(new HMac(digest), digest.getDigestSize(), 0); // IV not supported - throws exception when passing ParameterWithIV
		}
	}

	public static class SHA1
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final SHA1Digest digest = new SHA1Digest();
			return new MACCalculatorImpl(new HMac(digest), digest.getDigestSize(), 0); // IV not supported - throws exception when passing ParameterWithIV
		}
	}

	public static class SHA224
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final SHA224Digest digest = new SHA224Digest();
			return new MACCalculatorImpl(new HMac(digest), digest.getDigestSize(), 0); // IV not supported - throws exception when passing ParameterWithIV
		}
	}

	public static class SHA256
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final SHA256Digest digest = new SHA256Digest();
			return new MACCalculatorImpl(new HMac(digest), digest.getDigestSize(), 0); // IV not supported - throws exception when passing ParameterWithIV
		}
	}

	public static class SHA384
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final SHA384Digest digest = new SHA384Digest();
			return new MACCalculatorImpl(new HMac(digest), digest.getDigestSize(), 0); // IV not supported - throws exception when passing ParameterWithIV
		}
	}

	/**
	 * @deprecated See {@link OldHMac}.
	 */
	@Deprecated
	public static class OldSHA384
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final SHA384Digest digest = new SHA384Digest();
			return new MACCalculatorImpl(new OldHMac(digest), digest.getDigestSize(), 0); // IV not supported - throws exception when passing ParameterWithIV
		}
	}

	public static class SHA512
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final SHA512Digest digest = new SHA512Digest();
			return new MACCalculatorImpl(new HMac(digest), digest.getDigestSize(), 0); // IV not supported - throws exception when passing ParameterWithIV
		}
	}

	/**
	 * @deprecated See {@link OldHMac}.
	 */
	@Deprecated
	public static class OldSHA512
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final SHA512Digest digest = new SHA512Digest();
			return new MACCalculatorImpl(new OldHMac(digest), digest.getDigestSize(), 0); // IV not supported - throws exception when passing ParameterWithIV
		}
	}

	public static class RIPEMD128
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final RIPEMD128Digest digest = new RIPEMD128Digest();
			return new MACCalculatorImpl(new HMac(digest), digest.getDigestSize(), 0); // IV not supported - throws exception when passing ParameterWithIV
		}
	}

	public static class RIPEMD160
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final RIPEMD160Digest digest = new RIPEMD160Digest();
			return new MACCalculatorImpl(new HMac(digest), digest.getDigestSize(), 0); // IV not supported - throws exception when passing ParameterWithIV
		}
	}

	public static class Tiger
	extends MACCalculatorFactoryImpl
	{
		@Override
		public MACCalculator _createMACCalculator() {
			final TigerDigest digest = new TigerDigest();
			return new MACCalculatorImpl(new HMac(digest), digest.getDigestSize(), 0); // IV not supported - throws exception when passing ParameterWithIV
		}
	}

// TODO implement the following 3 later.
//	//
//	// PKCS12 states that the same algorithm should be used
//	// for the key generation as is used in the HMAC, so that
//	// is what we do here.
//	//
//
//	public static class PBEWithRIPEMD160
//	extends MACCalculatorFactoryImpl
//	{
//		public PBEWithRIPEMD160()
//		{
//			super(new HMac(new RIPEMD160Digest()), PKCS12, RIPEMD160, 160);
//		}
//	}
//
//	public static class PBEWithSHA
//	extends MACCalculatorFactoryImpl
//	{
//		public PBEWithSHA()
//		{
//			super(new HMac(new SHA1Digest()), PKCS12, SHA1, 160);
//		}
//	}
//
//	public static class PBEWithTiger
//	extends MACCalculatorFactoryImpl
//	{
//		public PBEWithTiger()
//		{
//			super(new HMac(new TigerDigest()), PKCS12, TIGER, 192);
//		}
//	}
}
