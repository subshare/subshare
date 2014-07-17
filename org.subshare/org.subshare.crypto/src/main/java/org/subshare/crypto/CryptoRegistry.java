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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedAsymmetricBlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.encodings.ISO9796d1Encoding;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.engines.AESLightEngine;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.engines.CAST5Engine;
import org.bouncycastle.crypto.engines.CAST6Engine;
import org.bouncycastle.crypto.engines.CamelliaEngine;
import org.bouncycastle.crypto.engines.CamelliaLightEngine;
import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.engines.ElGamalEngine;
import org.bouncycastle.crypto.engines.GOST28147Engine;
import org.bouncycastle.crypto.engines.Grain128Engine;
import org.bouncycastle.crypto.engines.Grainv1Engine;
import org.bouncycastle.crypto.engines.HC128Engine;
import org.bouncycastle.crypto.engines.HC256Engine;
import org.bouncycastle.crypto.engines.ISAACEngine;
import org.bouncycastle.crypto.engines.NaccacheSternEngine;
import org.bouncycastle.crypto.engines.NoekeonEngine;
import org.bouncycastle.crypto.engines.NullEngine;
import org.bouncycastle.crypto.engines.RC2Engine;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.engines.RC532Engine;
import org.bouncycastle.crypto.engines.RC564Engine;
import org.bouncycastle.crypto.engines.RC6Engine;
import org.bouncycastle.crypto.engines.RSABlindedEngine;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.engines.SEEDEngine;
import org.bouncycastle.crypto.engines.Salsa20Engine;
import org.bouncycastle.crypto.engines.SerpentEngine;
import org.bouncycastle.crypto.engines.SkipjackEngine;
import org.bouncycastle.crypto.engines.TEAEngine;
import org.bouncycastle.crypto.engines.TwofishEngine;
import org.bouncycastle.crypto.engines.XTEAEngine;
import org.bouncycastle.crypto.modes.AEADBlockCipher;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.CCMBlockCipher;
import org.bouncycastle.crypto.modes.CTSBlockCipher;
import org.bouncycastle.crypto.modes.EAXBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.modes.GOFBBlockCipher;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.ISO10126d2Padding;
import org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.paddings.TBCPadding;
import org.bouncycastle.crypto.paddings.X923Padding;
import org.bouncycastle.crypto.paddings.ZeroBytePadding;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.subshare.crypto.internal.asymmetric.AsymmetricBlockCipherImpl;
import org.subshare.crypto.internal.asymmetric.keypairgenerator.DHBasicKeyPairGeneratorFactory;
import org.subshare.crypto.internal.asymmetric.keypairgenerator.DSAKeyPairGeneratorFactory;
import org.subshare.crypto.internal.asymmetric.keypairgenerator.ElGamalKeyPairGeneratorFactory;
import org.subshare.crypto.internal.asymmetric.keypairgenerator.GOST3410KeyPairGeneratorFactory;
import org.subshare.crypto.internal.asymmetric.keypairgenerator.NaccacheSternKeyPairGeneratorFactory;
import org.subshare.crypto.internal.asymmetric.keypairgenerator.RSAKeyPairGeneratorFactory;
import org.subshare.crypto.internal.mac.MACCalculatorFactoryImpl;
import org.subshare.crypto.internal.symmetric.AEADBlockCipherImpl;
import org.subshare.crypto.internal.symmetric.BufferedBlockCipherImpl;
import org.subshare.crypto.internal.symmetric.SecretKeyGeneratorImpl;
import org.subshare.crypto.internal.symmetric.StreamCipherImpl;
import org.subshare.crypto.internal.symmetric.mode.C4jCBCCTSBlockCipher;
import org.subshare.crypto.internal.symmetric.mode.C4jCFBBlockCipher;
import org.subshare.crypto.internal.symmetric.mode.C4jOFBBlockCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Entry to the unified crypto API.
 * </p>
 * <p>
 * This registry can be used for various cryptography-related tasks. For example to {@link #createCipher(String) create a cipher}
 * or to {@link #createKeyPairGenerator(String, boolean) create a key-pair-generator}.
 * </p>
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public final class CryptoRegistry
{
	private static final Logger logger = LoggerFactory.getLogger(CryptoRegistry.class);
	private static final CryptoRegistry instance = new CryptoRegistry();

	/**
	 * Get the shared instance of this registry.
	 * @return the shared instance.
	 */
	public static CryptoRegistry getInstance()
	{
		return instance;
	}

	//////////////////// BEGIN cipher engines ////////////////////
	private final Map<String, Class<? extends AsymmetricBlockCipher>> algorithmName2asymmetricBlockCipherEngineClass = new HashMap<String, Class<? extends AsymmetricBlockCipher>>();
	private final Map<String, Class<? extends BlockCipher>> algorithmName2blockCipherEngineClass = new HashMap<String, Class<? extends BlockCipher>>();
	private final Map<String, Class<? extends StreamCipher>> algorithmName2streamCipherEngineClass = new HashMap<String, Class<? extends StreamCipher>>();

	private void registerBlockCipherEngineClass(final Class<? extends BlockCipher> engineClass)
	{
		final BlockCipher engine = newInstance(engineClass);
		final String algorithmName = engine.getAlgorithmName();
		logger.trace("registerSymmetricEngineClass: algorithmName=\"{}\" engineClass=\"{}\"", algorithmName, engineClass.getName());
		algorithmName2blockCipherEngineClass.put(algorithmName.toUpperCase(Locale.ENGLISH), engineClass);
	}

	private void registerBlockCipherEngineClass(final String algorithmName, final Class<? extends BlockCipher> engineClass)
	{
		newInstance(engineClass); // for testing, if the default constructor can be used, only - instance is not used
		logger.trace("registerSymmetricEngineClass: algorithmName=\"{}\" engineClass=\"{}\"", algorithmName, engineClass.getName());
		algorithmName2blockCipherEngineClass.put(algorithmName.toUpperCase(Locale.ENGLISH), engineClass);
	}

	private void registerAsymmetricBlockCipherEngineClass(final String algorithmName, final Class<? extends AsymmetricBlockCipher> engineClass)
	{
		newInstance(engineClass); // for testing to be sure there is a default constructor and we can call it.
		logger.trace("registerAsymmetricEngineClass: algorithmName=\"{}\" engineClass=\"{}\"", algorithmName, engineClass.getName());
		algorithmName2asymmetricBlockCipherEngineClass.put(algorithmName.toUpperCase(Locale.ENGLISH), engineClass);
	}

	private void registerStreamCipherEngineClass(final Class<? extends StreamCipher> engineClass)
	{
		final StreamCipher engine = newInstance(engineClass);
		final String algorithmName = engine.getAlgorithmName();
		_registerStreamCipherEngineClass(algorithmName, engineClass);
	}

	private void registerStreamCipherEngineClass(final String algorithmName, final Class<? extends StreamCipher> engineClass)
	{
		newInstance(engineClass); // for testing to be sure there is a default constructor and we can call it.
		_registerStreamCipherEngineClass(algorithmName, engineClass);
	}

	private void _registerStreamCipherEngineClass(final String algorithmName, final Class<? extends StreamCipher> engineClass)
	{
		if (algorithmName == null)
			throw new IllegalArgumentException("algorithmName == null");

		if (engineClass == null)
			throw new IllegalArgumentException("engineClass == null");

		logger.trace("registerSymmetricEngineClass: algorithmName=\"{}\" engineClass=\"{}\"", algorithmName, engineClass.getName());
		algorithmName2streamCipherEngineClass.put(algorithmName.toUpperCase(Locale.ENGLISH), engineClass);
	}
	//////////////////// END cipher engines ////////////////////


	//////////////////// BEGIN block cipher modes ////////////////////
	private final Map<String, Class<? extends BlockCipher>> modeName2blockCipherModeClass = new HashMap<String, Class<? extends BlockCipher>>();
	private final Map<String, Class<? extends BufferedBlockCipher>> modeName2bufferedBlockCipherModeClass = new HashMap<String, Class<? extends BufferedBlockCipher>>();
	private final Map<String, Class<? extends AEADBlockCipher>> modeName2aeadBlockCipherModeClass = new HashMap<String, Class<? extends AEADBlockCipher>>();

	private void registerBlockCipherMode(final String modeName, final Class<? extends BlockCipher> modeClass)
	{
		logger.trace("registerBlockCipherMode: modeName=\"{}\" modeClass=\"{}\"", modeName, modeClass.getName());
		modeName2blockCipherModeClass.put(modeName.toUpperCase(Locale.ENGLISH), modeClass);
	}
	private void registerBufferedBlockCipherMode(final String modeName, final Class<? extends BufferedBlockCipher> modeClass)
	{
		logger.trace("registerBufferedBlockCipherMode: modeName=\"{}\" modeClass=\"{}\"", modeName, modeClass.getName());
		modeName2bufferedBlockCipherModeClass.put(modeName.toUpperCase(Locale.ENGLISH), modeClass);
	}
	private void registerAEADBlockCipherMode(final String modeName, final Class<? extends AEADBlockCipher> modeClass)
	{
		logger.trace("registerAEADBlockCipherMode: modeName=\"{}\" modeClass=\"{}\"", modeName, modeClass.getName());
		modeName2aeadBlockCipherModeClass.put(modeName.toUpperCase(Locale.ENGLISH), modeClass);
	}
	//////////////////// END block cipher modes ////////////////////


	//////////////////// BEGIN block cipher paddings ////////////////////
	private final Map<String, Class<? extends BlockCipherPadding>> paddingName2blockCipherPaddingClass = new HashMap<String, Class<? extends BlockCipherPadding>>();
	private void registerBlockCipherPadding(final Class<? extends BlockCipherPadding> paddingClass)
	{
		final BlockCipherPadding padding = newInstance(paddingClass);
		final String paddingName = padding.getPaddingName();
		logger.debug("registerBlockCipherPadding: paddingName=\"{}\" paddingClass=\"{}\"", paddingName, paddingClass.getName());
		paddingName2blockCipherPaddingClass.put(paddingName.toUpperCase(Locale.ENGLISH), paddingClass);
		paddingName2blockCipherPaddingClass.put((paddingName + "Padding").toUpperCase(Locale.ENGLISH), paddingClass);
	}
	private void registerBlockCipherPadding(final String paddingName, final Class<? extends BlockCipherPadding> paddingClass)
	{
		newInstance(paddingClass); // for testing to be sure there is a default constructor and we can call it.
		logger.trace("registerBlockCipherPadding: paddingName=\"{}\" paddingClass=\"{}\"", paddingName, paddingClass.getName());
		paddingName2blockCipherPaddingClass.put(paddingName.toUpperCase(Locale.ENGLISH), paddingClass);
		paddingName2blockCipherPaddingClass.put((paddingName + "Padding").toUpperCase(Locale.ENGLISH), paddingClass);
	}
	//////////////////// END block cipher paddings ////////////////////


	//////////////////// BEGIN asymmetric paddings ////////////////////
	private final Map<String, Class<? extends AsymmetricBlockCipher>> paddingName2asymmetricBlockCipherPaddingClass = new HashMap<String, Class<? extends AsymmetricBlockCipher>>();
	private void registerAsymmetricBlockCipherPadding(final String paddingName, final Class<? extends AsymmetricBlockCipher> paddingClass)
	{
		logger.trace("registerAsymmetricBlockCipherPadding: paddingName=\"{}\" paddingClass=\"{}\"", paddingName, paddingClass.getName());
		paddingName2asymmetricBlockCipherPaddingClass.put(paddingName.toUpperCase(Locale.ENGLISH), paddingClass);
		paddingName2asymmetricBlockCipherPaddingClass.put((paddingName + "Padding").toUpperCase(Locale.ENGLISH), paddingClass);
	}
	//////////////////// END asymmetric paddings ////////////////////


	//////////////////// BEGIN asymmetric key generators ////////////////////
	private final Map<String, AsymmetricCipherKeyPairGeneratorFactory> algorithmName2asymmetricCipherKeyPairGeneratorFactory = new HashMap<String, AsymmetricCipherKeyPairGeneratorFactory>();
	private void registerAsymmetricCipherKeyPairGeneratorFactory(final AsymmetricCipherKeyPairGeneratorFactory factory)
	{
		if (factory == null)
			throw new IllegalArgumentException("factory == null");

		if (factory.getAlgorithmName() == null)
			throw new IllegalArgumentException("factory.getAlgorithmName() == null");

		logger.trace("registerAsymmetricCipherKeyPairGeneratorFactory: algorithmName=\"{}\" factoryClass=\"{}\"", factory.getAlgorithmName(), factory.getClass().getName());
		algorithmName2asymmetricCipherKeyPairGeneratorFactory.put(factory.getAlgorithmName(), factory);
	}
	//////////////////// END asymmetric key generators ////////////////////


	private CryptoRegistry() {
		// *** BEGIN AsymmetricBlockCipher engines ***
		registerAsymmetricBlockCipherEngineClass("ElGamal", ElGamalEngine.class);
		registerAsymmetricBlockCipherEngineClass("NaccacheStern", NaccacheSternEngine.class);

		// According to the JCERSACipher class, the RSABlindedEngine is used for RSA in the JCE, thus commenting out the other two.
		registerAsymmetricBlockCipherEngineClass("RSA", RSABlindedEngine.class);
//		registerAsymmetricBlockCipherEngineClass("RSA", RSABlindingEngine.class);
//		registerAsymmetricBlockCipherEngineClass("RSA", RSAEngine.class);
		// *** END AsymmetricBlockCipher engines ***

		// *** BEGIN BlockCipher engines ***
		registerBlockCipherEngineClass(AESEngine.class);
		// We register the other two AES implementations under alternative names.
		registerBlockCipherEngineClass("AES.fast", AESFastEngine.class);
		registerBlockCipherEngineClass("AES.light", AESLightEngine.class);

		registerBlockCipherEngineClass(BlowfishEngine.class);
		registerBlockCipherEngineClass(CamelliaEngine.class);
		// Registering the alternative implementation under an alternative name.
		registerBlockCipherEngineClass("Camellia.light", CamelliaLightEngine.class);

		registerBlockCipherEngineClass(CAST5Engine.class);
		registerBlockCipherEngineClass(CAST6Engine.class);
		registerBlockCipherEngineClass(DESedeEngine.class);
		registerBlockCipherEngineClass(DESEngine.class);
		registerBlockCipherEngineClass(GOST28147Engine.class);
		// IDEA is only in the "ext" BouncyCastle lib - not in the normal one. I think it's not needed, anyway. ...at least for now...
//		registerBlockCipherEngineClass(IDEAEngine.class);
		registerBlockCipherEngineClass(NoekeonEngine.class);
		registerBlockCipherEngineClass(NullEngine.class);
		registerBlockCipherEngineClass(RC2Engine.class);
		registerBlockCipherEngineClass(RC532Engine.class);
		registerBlockCipherEngineClass(RC564Engine.class);
		registerBlockCipherEngineClass(RC6Engine.class);
		registerBlockCipherEngineClass(RijndaelEngine.class);
		registerBlockCipherEngineClass(SEEDEngine.class);
		registerBlockCipherEngineClass(SerpentEngine.class);
		registerBlockCipherEngineClass(SkipjackEngine.class);
		registerBlockCipherEngineClass(TEAEngine.class);
		registerBlockCipherEngineClass(TwofishEngine.class);
//		registerSymmetricEngineClass(VMPCEngine.class);
//		registerSymmetricEngineClass(VMPCKSA3Engine.class);
		registerBlockCipherEngineClass(XTEAEngine.class);
		// *** END BlockCipher engines ***


		// *** BEGIN StreamCipher engines ***
		registerStreamCipherEngineClass(Grain128Engine.class);
		registerStreamCipherEngineClass("GRAIN-V1", Grainv1Engine.class);
		registerStreamCipherEngineClass(HC128Engine.class);
		registerStreamCipherEngineClass(HC256Engine.class);
		registerStreamCipherEngineClass(ISAACEngine.class);
		registerStreamCipherEngineClass(RC4Engine.class);
		registerStreamCipherEngineClass(Salsa20Engine.class);
		// *** END StreamCipher engines ***


// *** Wrap engines ***
//		register___(AESWrapEngine.class);
//		register___(CamelliaWrapEngine.class);
//		register___(DESedeWrapEngine.class);
//		register___(RC2WrapEngine.class);
//		register___(RFC3211WrapEngine.class);
//		register___(RFC3394WrapEngine.class);
//		register___(SEEDWrapEngine.class);

		// *** Other stuff ***
//		register___(IESEngine.class);



		// *** BEGIN block cipher modes ***
		registerBlockCipherMode("CBC", CBCBlockCipher.class);
		registerAEADBlockCipherMode("CCM", CCMBlockCipher.class);

		registerBlockCipherMode("CFB", C4jCFBBlockCipher.class);
		for (int i = 1; i <= 32; ++i)
			registerBlockCipherMode("CFB" + (i * 8), C4jCFBBlockCipher.class);

		registerBufferedBlockCipherMode("CTS", CTSBlockCipher.class);
		registerBufferedBlockCipherMode("CBC-CTS", C4jCBCCTSBlockCipher.class);

		registerAEADBlockCipherMode("EAX", EAXBlockCipher.class);
		registerAEADBlockCipherMode("GCM", GCMBlockCipher.class);
		registerBlockCipherMode("GOFB", GOFBBlockCipher.class);

		registerBlockCipherMode("OFB", C4jOFBBlockCipher.class);
		for (int i = 1; i <= 32; ++i)
			registerBlockCipherMode("OFB" + (i * 8), C4jOFBBlockCipher.class);

//		registerBlockCipherMode("OpenPGPCFB", OpenPGPCFBBlockCipher.class);
//		registerBlockCipherMode("PGPCFB", PGPCFBBlockCipher.class);
		registerBlockCipherMode("SIC", SICBlockCipher.class);

		// Test all registered BlockCipherModes - MUST BE HERE AFTER THEIR REGISTRATION
		testBlockCipherModes();
		// *** END block cipher modes ***

		// *** BEGIN block cipher paddings ***
		registerBlockCipherPadding(ISO10126d2Padding.class);
		registerBlockCipherPadding("ISO10126", ISO10126d2Padding.class);
		registerBlockCipherPadding(ISO7816d4Padding.class);
		registerBlockCipherPadding(PKCS7Padding.class);
		registerBlockCipherPadding("PKCS5", PKCS7Padding.class);
		registerBlockCipherPadding(TBCPadding.class);
		registerBlockCipherPadding(X923Padding.class);
		registerBlockCipherPadding(ZeroBytePadding.class);
		// *** END block cipher paddings ***


		// *** BEGIN asymmetric paddings ***
		registerAsymmetricBlockCipherPadding("ISO9796-1", ISO9796d1Encoding.class);
		registerAsymmetricBlockCipherPadding("OAEP", OAEPEncoding.class);
		registerAsymmetricBlockCipherPadding("OAEPWITHSHA1ANDMGF1", OAEPEncoding.class); // JCE name for compatibility.
		registerAsymmetricBlockCipherPadding("PKCS1", PKCS1Encoding.class);


		// Test all registered asymmetric paddings - MUST BE HERE AFTER THEIR REGISTRATION
		testAsymmetricBlockCipherPaddings();
		// *** END asymmetric paddings ***

		// *** BEGIN asymmetric key pair generators ***
		registerAsymmetricCipherKeyPairGeneratorFactory(new DHBasicKeyPairGeneratorFactory());
		registerAsymmetricCipherKeyPairGeneratorFactory(new DSAKeyPairGeneratorFactory());
		registerAsymmetricCipherKeyPairGeneratorFactory(new ElGamalKeyPairGeneratorFactory());
		registerAsymmetricCipherKeyPairGeneratorFactory(new GOST3410KeyPairGeneratorFactory());
		registerAsymmetricCipherKeyPairGeneratorFactory(new NaccacheSternKeyPairGeneratorFactory());
		registerAsymmetricCipherKeyPairGeneratorFactory(new RSAKeyPairGeneratorFactory());
		// *** END asymmetric key pair generators ***
	}

	private void testAsymmetricBlockCipherPaddings()
	{
		final AsymmetricBlockCipher engine = createAsymmetricBlockCipherEngine("RSA");
		if (engine == null)
			throw new IllegalStateException("No engine!");

		for (final String paddingName : paddingName2asymmetricBlockCipherPaddingClass.keySet())
			createAsymmetricBlockCipherPadding(paddingName, engine);
	}

	private void testBlockCipherModes()
	{
		final BlockCipher engine8 = createBlockCipherEngine("Blowfish".toUpperCase(Locale.ENGLISH));
		if (engine8 == null)
			throw new IllegalStateException("No 'Blowfish' engine!");

		final BlockCipher engine16 = createBlockCipherEngine("AES".toUpperCase(Locale.ENGLISH));
		if (engine16 == null)
			throw new IllegalStateException("No 'AES' engine!");

		for (final String modeName : modeName2blockCipherModeClass.keySet())
			createBlockCipherMode(modeName, engine8);

		for (final String modeName : modeName2bufferedBlockCipherModeClass.keySet())
			createBufferedBlockCipherMode(modeName, engine8);

		for (final String modeName : modeName2aeadBlockCipherModeClass.keySet())
			createAEADBlockCipherMode(modeName, engine16); // Most of these modes require a block-size of 16!
	}

	private <T> T newInstance(final Class<T> clazz)
	{
		try {
			return clazz.newInstance();
		} catch (final InstantiationException e) {
			throw new RuntimeException(e);
		} catch (final IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param algorithmName the simple encryption algorithm name (e.g. "AES" or "Twofish") and <b>not</b> the complete transformation.
	 * @return
	 */
	private BlockCipher createBlockCipherEngine(final String algorithmName)
	{
		final Class<? extends BlockCipher> engineClass = algorithmName2blockCipherEngineClass.get(algorithmName);
		if (engineClass == null)
			return null;

		return newInstance(engineClass);
	}

	private AsymmetricBlockCipher createAsymmetricBlockCipherEngine(final String algorithmName)
	{
		final Class<? extends AsymmetricBlockCipher> engineClass = algorithmName2asymmetricBlockCipherEngineClass.get(algorithmName);
		if (engineClass == null)
			return null;

		return newInstance(engineClass);
	}

	private StreamCipher createStreamCipherEngine(final String algorithmName)
	throws NoSuchAlgorithmException
	{
		final Class<? extends StreamCipher> engineClass = algorithmName2streamCipherEngineClass.get(algorithmName);
		if (engineClass == null)
			return null;

		return newInstance(engineClass);
	}

	private BlockCipher createBlockCipherMode(final String modeName, final BlockCipher engine)
	{
		final Class<? extends BlockCipher> modeClass = modeName2blockCipherModeClass.get(modeName);
		if (modeClass == null)
			return null;

		try {
			final Constructor<? extends BlockCipher> c = modeClass.getConstructor(BlockCipher.class, String.class);
			return c.newInstance(engine, modeName);
		} catch (final NoSuchMethodException x) {
			silentlyIgnore(); // We'll try it with the constructor without mode.
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}

		try {
			final Constructor<? extends BlockCipher> c = modeClass.getConstructor(BlockCipher.class);
			return c.newInstance(engine);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void silentlyIgnore() { } // this method does not need to be marked 'final', because the class is.

	private AEADBlockCipher createAEADBlockCipherMode(final String modeName, final BlockCipher engine)
	{
		final Class<? extends AEADBlockCipher> modeClass = modeName2aeadBlockCipherModeClass.get(modeName);
		if (modeClass == null)
			return null;

		try {
			final Constructor<? extends AEADBlockCipher> c = modeClass.getConstructor(BlockCipher.class);
			return c.newInstance(engine);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private BufferedBlockCipher createBufferedBlockCipherMode(final String modeName, final BlockCipher engine)
	{
		final Class<? extends BufferedBlockCipher> modeClass = modeName2bufferedBlockCipherModeClass.get(modeName);
		if (modeClass == null)
			return null;

		try {
			final Constructor<? extends BufferedBlockCipher> c = modeClass.getConstructor(BlockCipher.class);
			return c.newInstance(engine);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private BlockCipherPadding createBlockCipherPadding(final String paddingName)
	{
		final Class<? extends BlockCipherPadding> paddingClass = paddingName2blockCipherPaddingClass.get(paddingName);
		if (paddingClass == null)
			return null;

		return newInstance(paddingClass);
	}

	private Cipher createCipherForBlockCipherMode(final String transformation, final BlockCipher modeWithEngine, final String engineName, final String modeName, final String paddingName)
	throws NoSuchPaddingException
	{
		if (paddingName.isEmpty() || "NOPADDING".equals(paddingName))
			return new BufferedBlockCipherImpl(transformation, new BufferedBlockCipher(modeWithEngine));

		final BlockCipherPadding padding = createBlockCipherPadding(paddingName);
		if (padding == null)
			throw new NoSuchPaddingException("There is no block-cipher-padding class registed with the name \"" + paddingName + "\"!");

		return new BufferedBlockCipherImpl(transformation, new PaddedBufferedBlockCipher(modeWithEngine, padding));
	}

	private Cipher createCipherForBlockCipherMode(final String transformation, final AEADBlockCipher modeWithEngine, final String engineName, final String modeName, final String paddingName)
	throws NoSuchPaddingException
	{
		if (paddingName.isEmpty() || "NOPADDING".equals(paddingName))
			return new AEADBlockCipherImpl(transformation, modeWithEngine);

		throw new NoSuchPaddingException("The AEAD-mode \"" + modeName + "\" does not support the padding \"" + paddingName + "\"! Padding must be \"NoPadding\" or an empty string!");
	}

	private Cipher createCipherForBlockCipherMode(final String transformation, final BufferedBlockCipher modeWithEngine, final String engineName, final String modeName, final String paddingName)
	throws NoSuchPaddingException
	{
		if (paddingName.isEmpty() || "NOPADDING".equals(paddingName))
			return new BufferedBlockCipherImpl(transformation, modeWithEngine);

		throw new NoSuchPaddingException("The block-cipher-mode \"" + modeName + "\" does not support the padding \"" + paddingName + "\"! Padding must be \"NoPadding\" or an empty string!");
	}

	private Cipher createCipherForBlockCipherEngine(final String transformation, final BlockCipher engine, final String engineName, final String modeName, final String paddingName)
	throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		if (modeName.isEmpty() || "ECB".equals(modeName))
			return createCipherForBlockCipherMode(transformation, engine, engineName, modeName, paddingName);

		{
			final BlockCipher mode = createBlockCipherMode(modeName, engine);
			if (mode != null)
				return createCipherForBlockCipherMode(transformation, mode, engineName, modeName, paddingName);
		}

		{
			final BufferedBlockCipher mode = createBufferedBlockCipherMode(modeName, engine);
			if (mode != null)
				return createCipherForBlockCipherMode(transformation, mode, engineName, modeName, paddingName);
		}

		{
			final AEADBlockCipher mode = createAEADBlockCipherMode(modeName, engine);
			if (mode != null)
				return createCipherForBlockCipherMode(transformation, mode, engineName, modeName, paddingName);
		}

		throw new NoSuchAlgorithmException("There is no block-cipher-mode-class registered with the modeName \"" + modeName + "\"!");
	}

	private Cipher createCipherForStreamCipherMode(final String transformation, final StreamCipher modeWithEngine, final String engineName, final String modeName, final String paddingName)
	throws NoSuchPaddingException
	{
		if (paddingName.isEmpty() || "NOPADDING".equals(paddingName))
			return new StreamCipherImpl(transformation, modeWithEngine);

		throw new NoSuchPaddingException("The stream-cipher-mode \"" + modeName + "\" does not support the padding \"" + paddingName + "\"! Padding must be \"NoPadding\" or an empty string!");
	}

	private Cipher createCipherForStreamCipherEngine(final String transformation, final StreamCipher engine, final String engineName, final String modeName, final String paddingName)
	throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		if (modeName.isEmpty() || "ECB".equals(modeName))
			return createCipherForStreamCipherMode(transformation, engine, engineName, modeName, paddingName);

		throw new NoSuchAlgorithmException("The stream-cipher does not support the mode \"" + modeName + "\"! Only \"ECB\" or an empty string are allowed as mode!");
	}

	private AsymmetricBlockCipher createAsymmetricBlockCipherPadding(final String paddingName, final AsymmetricBlockCipher engine)
	{
		final Class<? extends AsymmetricBlockCipher> paddingClass = paddingName2asymmetricBlockCipherPaddingClass.get(paddingName);
		if (paddingClass == null)
			return null;

		try {
			final Constructor<? extends AsymmetricBlockCipher> c = paddingClass.getConstructor(AsymmetricBlockCipher.class);
			return c.newInstance(engine);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Cipher createCipherForAsymmetricBlockCipherMode(final String transformation, final AsymmetricBlockCipher modeWithEngine, final String engineName, final String modeName, final String paddingName)
	throws NoSuchPaddingException
	{
		AsymmetricBlockCipher padding;
		if (paddingName.isEmpty() || "NOPADDING".equals(paddingName))
			padding = modeWithEngine;
		else {
			padding = createAsymmetricBlockCipherPadding(paddingName, modeWithEngine);
			if (padding == null)
				throw new NoSuchPaddingException("There is no asymmetric-block-cipher-padding registered with name \"" + paddingName + "\"!");
		}

		return new AsymmetricBlockCipherImpl(
				transformation,
				new BufferedAsymmetricBlockCipher(padding)
		);
	}

	private Cipher createCipherForAsymmetricBlockCipherEngine(final String transformation, final AsymmetricBlockCipher engine, final String engineName, final String modeName, final String paddingName)
	throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		if (modeName.isEmpty() || "ECB".equals(modeName))
			return createCipherForAsymmetricBlockCipherMode(transformation, engine, engineName, modeName, paddingName);

		throw new NoSuchAlgorithmException("The asymmetric-block-cipher does not support the mode \"" + modeName + "\"! Only \"ECB\" or an empty string are allowed as mode!");
	}

	/**
	 * Get all supported cipher engines. A cipher engine implements a raw
	 * <a target="_blank" href="http://en.wikipedia.org/wiki/Encryption_algorithm">encryption algorithm</a>;
	 * 'raw' means without any additional transformation like block mode or padding.
	 *
	 * @param cipherEngineType the type of the cipher engine or <code>null</code> to list all.
	 * @return all supported cipher engines for the (optionally) given criteria.
	 * @see #createCipher(String)
	 */
	public Set<String> getSupportedCipherEngines(final CipherEngineType cipherEngineType)
	{
		final SortedSet<String> result = new TreeSet<String>();

		if (cipherEngineType == null || cipherEngineType == CipherEngineType.symmetricBlock)
			result.addAll(algorithmName2blockCipherEngineClass.keySet());

		if (cipherEngineType == null || cipherEngineType == CipherEngineType.symmetricStream)
			result.addAll(algorithmName2streamCipherEngineClass.keySet());

		if (cipherEngineType == null || cipherEngineType == CipherEngineType.asymmetricBlock)
			result.addAll(algorithmName2asymmetricBlockCipherEngineClass.keySet());

		return Collections.unmodifiableSortedSet(result);
	}

	/**
	 * <p>
	 * Get all supported <a target="_blank" href="http://en.wikipedia.org/wiki/Block_cipher_modes_of_operation">modes</a> for
	 * the given cipher engine (a raw
	 * <a target="_blank" href="http://en.wikipedia.org/wiki/Encryption_algorithm">encryption algorithm</a>). The
	 * <code>cipherEngine</code> can be <code>null</code> to not restrict the result by this criterion.
	 * </p>
	 * <p>
	 * See <a target="_blank" href="http://cumulus4j.org/${project.version}/documentation/supported-algorithms.html">Supported algorithms</a>
	 * for a list of supported algorithms or use {@link #getSupportedCipherEngines(CipherEngineType)} to
	 * query them.
	 * </p>
	 *
	 * @param cipherEngine the name of the encryption algorithm for which to look up supported
	 * modes or <code>null</code> to list all.
	 * @return all supported modes for the (optionally) given criteria.
	 * @see #createCipher(String)
	 */
	public Set<String> getSupportedCipherModes(String cipherEngine)
	{
		if (cipherEngine != null)
			cipherEngine = cipherEngine.toUpperCase(Locale.ENGLISH);

		final SortedSet<String> result = new TreeSet<String>();

		if (cipherEngine == null || algorithmName2blockCipherEngineClass.containsKey(cipherEngine)) {
			// Engine is a block cipher => return all modes
			result.add(""); result.add("ECB"); // both are synonymous
			result.addAll(modeName2aeadBlockCipherModeClass.keySet());
			result.addAll(modeName2blockCipherModeClass.keySet());
			result.addAll(modeName2bufferedBlockCipherModeClass.keySet());
		}

		if (cipherEngine == null || algorithmName2streamCipherEngineClass.containsKey(cipherEngine)) {
			// Engine is a stream cipher => no modes supported besides ECB (either named "ECB" or empty String).
			result.add(""); result.add("ECB"); // both are synonymous
		}

		if (cipherEngine == null || algorithmName2asymmetricBlockCipherEngineClass.containsKey(cipherEngine)) {
			// Engine is an asymmetric cipher => no modes supported besides ECB (either named "ECB" or empty String).
			result.add(""); result.add("ECB"); // both are synonymous
		}

		final Set<String> blackListedModes = blackListedCipherEngine2Modes.get(cipherEngine);
		if (blackListedModes != null)
			result.removeAll(blackListedModes);

		return Collections.unmodifiableSortedSet(result);
	}

	private final Map<String, Set<String>> blackListedCipherEngine2Modes = new HashMap<String, Set<String>>();
	{
		// Created this by trial and error. Needs to be updated, if the CipherTest fails.
		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("AES.FAST", modes);
			modes.add("GOFB");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("AES.LIGHT", modes);
			modes.add("GOFB");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("AES", modes);
			modes.add("GOFB");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("BLOWFISH", modes);
			modes.add("CCM");
			modes.add("GCM");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("CAMELLIA.LIGHT", modes);
			modes.add("GOFB");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("CAMELLIA", modes);
			modes.add("GOFB");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("CAST5", modes);
			modes.add("CCM");
			modes.add("GCM");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("CAST6", modes);
			modes.add("GOFB");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("DES", modes);
			modes.add("CCM");
			modes.add("GCM");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("DESEDE", modes);
			modes.add("CCM");
			modes.add("GCM");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("GOST28147", modes);
			modes.add("CCM");
			modes.add("GCM");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("NOEKEON", modes);
			modes.add("GOFB");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("NULL", modes);
			modes.add("CCM");
			modes.add("GCM");
			modes.add("EAX");
			modes.add("GOFB");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("RC2", modes);
			modes.add("CCM");
			modes.add("GCM");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("RC5-32", modes);
			modes.add("CCM");
			modes.add("GCM");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("RC5-64", modes);
			modes.add("GOFB");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("RC6", modes);
			modes.add("GOFB");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("RIJNDAEL", modes);
			modes.add("GOFB");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("SEED", modes);
			modes.add("GOFB");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("SERPENT", modes);
			modes.add("GOFB");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("SKIPJACK", modes);
			modes.add("CCM");
			modes.add("GCM");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("TEA", modes);
			modes.add("CCM");
			modes.add("GCM");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("TWOFISH", modes);
			modes.add("GOFB");
		}

		{
			final Set<String> modes = new HashSet<String>();
			blackListedCipherEngine2Modes.put("XTEA", modes);
			modes.add("CCM");
			modes.add("GCM");
		}
	}

	/**
	 * Get all supported paddings for the given {@link CipherEngineType}. If there is
	 * no cipher-engine-type given, all supported paddings for all engine types are returned.
	 * @param cipherEngineType the type of the cipher engine or <code>null</code> to ignore this criterion.
	 * @return all supported paddings for the (optionally) given criteria.
	 * @see #createCipher(String)
	 */
	public Set<String> getSupportedCipherPaddings(final CipherEngineType cipherEngineType)
	{
		return getSupportedCipherPaddings(cipherEngineType, null, null);
	}

	/**
	 * <p>
	 * Get all supported paddings for the given cipher engine (a raw
	 * <a target="_blank" href="http://en.wikipedia.org/wiki/Encryption_algorithm">encryption algorithm</a>) and
	 * <a target="_blank" href="http://en.wikipedia.org/wiki/Block_cipher_modes_of_operation">mode</a>. Each of the
	 * parameters can be <code>null</code> to not restrict the result by this criterion.
	 * </p>
	 * <p>
	 * See <a target="_blank" href="http://cumulus4j.org/${project.version}/documentation/supported-algorithms.html">Supported algorithms</a>
	 * for a list of supported algorithms or use {@link #getSupportedCipherEngines(CipherEngineType)}
	 * and {@link #getSupportedCipherModes(String)} to
	 * query them.
	 * </p>
	 *
	 * @param cipherEngine the cipher engine for which to get the supported paddings or <code>null</code>
	 * to list all.
	 * @param cipherMode the <a target="_blank" href="http://en.wikipedia.org/wiki/Block_cipher_modes_of_operation">mode</a>
	 * to restrict the result or <code>null</code> to list all (for the given cipher-engine).
	 * @return all supported paddings for the (optionally) given criteria.
	 * @see #createCipher(String)
	 */
	public Set<String> getSupportedCipherPaddings(final String cipherEngine, final String cipherMode)
	{
		return getSupportedCipherPaddings(null, cipherEngine, cipherMode);
	}

	private Set<String> getSupportedCipherPaddings(final CipherEngineType cipherEngineType, String cipherEngine, String cipherMode)
	{
		if (cipherEngine != null)
			cipherEngine = cipherEngine.toUpperCase(Locale.ENGLISH);

		if (cipherMode != null)
			cipherMode = cipherMode.toUpperCase(Locale.ENGLISH);

		final SortedSet<String> result = new TreeSet<String>();

		if ((cipherEngineType == null || cipherEngineType == CipherEngineType.symmetricBlock) &&
				(cipherEngine == null || algorithmName2blockCipherEngineClass.containsKey(cipherEngine)))
		{
			// Engine is a block cipher
			result.add(""); result.add("NOPADDING"); // both are synonymous

			if (cipherMode == null || modeName2blockCipherModeClass.containsKey(cipherMode))
				result.addAll(paddingName2blockCipherPaddingClass.keySet());
		}

		if ((cipherEngineType == null || cipherEngineType == CipherEngineType.symmetricStream) &&
				(cipherEngine == null || algorithmName2streamCipherEngineClass.containsKey(cipherEngine)))
		{
			// Engine is a stream cipher
			result.add(""); result.add("NOPADDING"); // both are synonymous
		}

		if ((cipherEngineType == null || cipherEngineType == CipherEngineType.asymmetricBlock) &&
				(cipherEngine == null || algorithmName2asymmetricBlockCipherEngineClass.containsKey(cipherEngine)))
		{
			// Engine is an asymmetric block cipher
			result.add(""); result.add("NOPADDING"); // both are synonymous
			result.addAll(paddingName2asymmetricBlockCipherPaddingClass.keySet());
		}

		return Collections.unmodifiableSortedSet(result);
	}

	/**
	 * <p>
	 * Get all supported cipher transformations.
	 * </p>
	 * <p>
	 * Every element of the resulting <code>Set</code> can be passed to {@link #createCipher(String)} and will
	 * return a usable {@link Cipher} instance. However, not everything that is supported makes sense! It might
	 * not even be secure in certain situations! This is just a listing of what you theoretically could pass to
	 * {@link #createCipher(String)}.
	 * </p>
	 *
	 * @param cipherEngineType the type of the cipher engine or <code>null</code> to list all.
	 * @return all supported cipher transformations for the (optionally) given criteria.
	 * @see #createCipher(String)
	 */
	public Set<String> getSupportedCipherTransformations(final CipherEngineType cipherEngineType)
	{
		final SortedSet<String> result = new TreeSet<String>();

		for (final String cipherEngine : getSupportedCipherEngines(cipherEngineType)) {
			for (final String cipherMode : getSupportedCipherModes(cipherEngine)) {
				for (final String cipherPadding : getSupportedCipherPaddings(cipherEngine, cipherMode))
					result.add(cipherEngine + '/' + cipherMode + '/' + cipherPadding);
			}
		}

		return Collections.unmodifiableSortedSet(result);
	}

	/**
	 * <p>
	 * Create a {@link Cipher} instance according to the given transformation.
	 * The transformation is a chain of algorithms containing 1 to 3 elements:
	 * </p>
	 * <ul>
	 * 	<li>encryption algorithm (required)</li>
	 *  <li>mode (optional)</li>
	 *  <li>padding (optional)</li>
	 * </ul>
	 * <p>
	 * For example:
	 * </p>
	 * <ul>
	 * <li>"AES"</li>
	 * <li>"AES/CBC/PKCS5Padding"</li>
	 * <li>"Twofish/CFB/NoPadding"</li>
	 * <li>"RSA"</li>
	 * <li>"RSA/ECB/OAEPWITHSHA1ANDMGF1PADDING"</li>
	 * <li>"RSA//OAEPWITHSHA1ANDMGF1PADDING"</li>
	 * </ul>
	 * <p>
	 * "ECB" and "NoPadding" are equivalent to an empty <code>String</code>.
	 * </p>
	 * <p>
	 * See <a target="_blank" href="http://cumulus4j.org/${project.version}/documentation/supported-algorithms.html">Supported algorithms</a>
	 * for a list of supported algorithms or use {@link #getSupportedCipherTransformations(CipherEngineType)}
	 * to query them. Additionally, you can use {@link #getSupportedCipherEngines(CipherEngineType)},
	 * {@link #getSupportedCipherModes(String)} and {@link #getSupportedCipherPaddings(String, String)}
	 * to query the individual parts of the supported transformations.
	 * </p>
	 *
	 * @param transformation the transformation. This is case-INsensitive. It must not be <code>null</code>.
	 * @return a new <code>Cipher</code> instance.
	 * @throws NoSuchAlgorithmException if there is no encryption engine or no mode registered to suit the given transformation.
	 * @throws NoSuchPaddingException if there is no padding registered to suit the given transformation.
	 * @see #getSupportedCipherTransformations(CipherEngineType)
	 * @see #getSupportedCipherEngines(CipherEngineType)
	 * @see #getSupportedCipherModes(String)
	 * @see #getSupportedCipherPaddings(CipherEngineType)
	 * @see #getSupportedCipherPaddings(String, String)
	 */
	public Cipher createCipher(final String transformation)
	throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		String[] transformationParts = splitTransformation(transformation);
		final String engineName = transformationParts[0].toUpperCase(Locale.ENGLISH);
		final String modeName = transformationParts[1].toUpperCase(Locale.ENGLISH);
		final String paddingName = transformationParts[2].toUpperCase(Locale.ENGLISH);
		transformationParts = null;

		{
			final BlockCipher engine = createBlockCipherEngine(engineName);
			if (engine != null)
				return createCipherForBlockCipherEngine(transformation, engine, engineName, modeName, paddingName);
		}

		{
			final AsymmetricBlockCipher engine = createAsymmetricBlockCipherEngine(engineName);
			if (engine != null)
				return createCipherForAsymmetricBlockCipherEngine(transformation, engine, engineName, modeName, paddingName);
		}

		{
			final StreamCipher engine = createStreamCipherEngine(engineName);
			if (engine != null)
				return createCipherForStreamCipherEngine(transformation, engine, engineName, modeName, paddingName);
		}

		throw new NoSuchAlgorithmException("There is no cipher-engine-class registered with the algorithmName \"" + engineName + "\"!");
	}

	/**
	 * Split the transformation-<code>String</code> into its parts. The transformation is what you would
	 * normally pass to {@link #createCipher(String)}, i.e. a chain of operations usually starting with
	 * an encryption algorithm and then optionally followed by a block-cipher-mode (e.g. "CBC") and a
	 * padding (e.g. "PKCS5Padding").
	 * @param transformation the transformation-<code>String</code>.
	 * @return a <code>String</code>-array with exactly 3 elements. None of these is ever <code>null</code>.
	 * If parts are missing in the transformation, the corresponding elements are an empty string.
	 * @throws IllegalArgumentException if the given transformation is <code>null</code> or contains
	 * more than 3 parts (i.e. more than 2 slashes).
	 */
	public static String[] splitTransformation(final String transformation)
	throws IllegalArgumentException
	{
		if (transformation == null)
			throw new IllegalArgumentException("transformation == null");

		final String[] result = new String[3];
		Arrays.fill(result, "");

		int lastSlashIdx = -1;
		int resultIdx = -1;
		while (true) {
			int slashIdx = transformation.indexOf('/', lastSlashIdx + 1);
			if (slashIdx < 0)
				slashIdx = transformation.length();

			if (++resultIdx > result.length - 1)
				throw new IllegalArgumentException("transformation=\"" + transformation + "\" contains more than " + (result.length - 1) + " slashes!");

			result[resultIdx] = transformation.substring(lastSlashIdx + 1, slashIdx).trim();
			lastSlashIdx = slashIdx;

			if (slashIdx == transformation.length())
				break;
		}

		return result;
	}

	/**
	 * Create a new {@link SecretKeyGenerator}.
	 *
	 * @param algorithmName the encryption algorithm for which the generated keys will be used.
	 * This is the first element of a transformation, i.e.
	 * you can pass a <code>transformation</code> to {@link #splitTransformation(String)} and use element 0 of its result.
	 * See <a target="_blank" href="http://cumulus4j.org/${project.version}/documentation/supported-algorithms.html">Supported algorithms</a>
	 * for a list of supported algorithms.
	 * @param initWithDefaults whether to initialise the secret key generator with default values.
	 * @return an instance of {@link SecretKeyGenerator}. If <code>initWithDefaults == true</code>, it can directly
	 * be used to generate keys, i.e. it is already initialised with some default values. If <code>initWithDefaults == false</code>,
	 * you still have to {@link SecretKeyGenerator#init(org.bouncycastle.crypto.KeyGenerationParameters) initialise} the
	 * key generator before you can use it.
	 * @throws NoSuchAlgorithmException
	 */
	public SecretKeyGenerator createSecretKeyGenerator(String algorithmName, final boolean initWithDefaults)
	throws NoSuchAlgorithmException
	{
		if (algorithmName == null)
			throw new IllegalArgumentException("algorithmName == null");

		algorithmName = algorithmName.toUpperCase(Locale.ENGLISH);

		if (!algorithmName2blockCipherEngineClass.containsKey(algorithmName) && !algorithmName2streamCipherEngineClass.containsKey(algorithmName))
			throw new NoSuchAlgorithmException("There is no block/stream cipher registered for the algorithmName=\"" + algorithmName + "\"!");

		final SecretKeyGeneratorImpl secretKeyGeneratorImpl = new SecretKeyGeneratorImpl();

		if (initWithDefaults)
			secretKeyGeneratorImpl.init(null);

		return secretKeyGeneratorImpl;
	}

	/**
	 * Create a key pair generator for the given <b>asymmetric</b> encryption algorithm. If <code>initWithDefaults</code>
	 * is specified with value <code>true</code>, the returned generator is ready to be used and doesn't require any
	 * further initialisation.
	 *
	 * @param algorithmName the name of the <b>asymmetric</b> encryption algorithm. This is the first element of a transformation, i.e.
	 * you can pass a <code>transformation</code> to {@link #splitTransformation(String)} and use element 0 of its result.
	 * See <a target="_blank" href="http://cumulus4j.org/${project.version}/documentation/supported-algorithms.html">Supported algorithms</a>
	 * for a list of supported algorithms.
	 * @param initWithDefaults whether to initialise the key pair generator with default values.
	 * @return an instance of {@link AsymmetricCipherKeyPairGenerator}. If <code>initWithDefaults == true</code>, it can directly
	 * be used to generate key pairs, i.e. it is already initialised with some default values. If <code>initWithDefaults == false</code>,
	 * you still have to {@link AsymmetricCipherKeyPairGenerator#init(org.bouncycastle.crypto.KeyGenerationParameters) initialise} the
	 * key pair generator before you can use it.
	 * @throws NoSuchAlgorithmException if there is no generator available for the given <code>algorithmName</code>.
	 */
	public AsymmetricCipherKeyPairGenerator createKeyPairGenerator(final String algorithmName, final boolean initWithDefaults)
	throws NoSuchAlgorithmException
	{
		if (algorithmName == null)
			throw new IllegalArgumentException("algorithmName == null");

		final AsymmetricCipherKeyPairGeneratorFactory factory = algorithmName2asymmetricCipherKeyPairGeneratorFactory.get(algorithmName);
		if (factory == null)
			throw new NoSuchAlgorithmException("There is no key-pair-generator-class registered for algorithmName \"" + algorithmName + "\"!");

		final AsymmetricCipherKeyPairGenerator generator = factory.createAsymmetricCipherKeyPairGenerator(initWithDefaults);
		return generator;
	}

	/**
	 * Decode (deserialise) a public key, that was previously encoded (serialised) by {@link #encodePublicKey(CipherParameters)}.
	 * @param publicKeyData the serialised public key.
	 * @return the public key (as previously passed to {@link #encodePublicKey(CipherParameters)}).
	 * @throws IOException if parsing the serialised public key fails.
	 * @see #encodePublicKey(CipherParameters)
	 * @see #decodePrivateKey(byte[])
	 */
	public CipherParameters decodePublicKey(final byte[] publicKeyData) throws IOException
	{
		final AsymmetricKeyParameter asymmetricKeyParameter = PublicKeyFactory.createKey(publicKeyData);
		return asymmetricKeyParameter;
	}

	/**
	 * Encode (serialise) a public key in order to store it or transport it over a network.
	 * @param publicKey the public key to be encoded; must not be <code>null</code>.
	 * @return the encoded (serialised) form of the public key. Can be passed to {@link #decodePublicKey(byte[])} to
	 * reverse this method.
	 * @see #decodePublicKey(byte[])
	 * @see #encodePrivateKey(CipherParameters)
	 */
	public byte[] encodePublicKey(final CipherParameters publicKey)
	{
		if (publicKey == null)
			throw new IllegalArgumentException("publicKey == null");

		// TODO use a class-based map or similar registry!
		try {
			if (publicKey instanceof RSAKeyParameters) {
				final RSAKeyParameters rsaPublicKey = (RSAKeyParameters) publicKey;

				final SubjectPublicKeyInfo info = new SubjectPublicKeyInfo(
						new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE),
						new RSAPublicKey(rsaPublicKey.getModulus(), rsaPublicKey.getExponent()).toASN1Primitive()
						);
				return info.getEncoded();
			}
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}

		throw new UnsupportedOperationException("publicKey.class=\"" + publicKey.getClass().getName() + "\" not yet supported!");
	}

	/**
	 * Decode (deserialise) a private key, that was previously encoded (serialised) by {@link #encodePrivateKey(CipherParameters)}.
	 * @param privateKeyData the serialised private key.
	 * @return the private key (as previously passed to {@link #encodePrivateKey(CipherParameters)}).
	 * @throws IOException if parsing the serialised private key fails.
	 * @see #encodePrivateKey(CipherParameters)
	 * @see #decodePublicKey(byte[])
	 */
	public CipherParameters decodePrivateKey(final byte[] privateKeyData) throws IOException
	{
		final AsymmetricKeyParameter asymmetricKeyParameter = PrivateKeyFactory.createKey(privateKeyData);
		return asymmetricKeyParameter;
	}

	/**
	 * <p>
	 * Encode (serialise) a private key in order to store it or transport it over a network.
	 * </p><p>
	 * <b>Important: You should keep your private key secret!</b> Thus, you might want to encrypt the result before
	 * storing it to a file or sending it somewhere!
	 * </p>
	 * @param privateKey the private key to be encoded; must not be <code>null</code>.
	 * @return the encoded (serialised) form of the private key. Can be passed to {@link #decodePrivateKey(byte[])} to
	 * reverse this method.
	 * @see #decodePrivateKey(byte[])
	 * @see #encodePublicKey(CipherParameters)
	 */
	public byte[] encodePrivateKey(final CipherParameters privateKey)
	{
		if (privateKey == null)
			throw new IllegalArgumentException("privateKey == null");

		// TODO use a class-based map or similar registry!
		try {
			if (privateKey instanceof RSAPrivateCrtKeyParameters) {
				final RSAPrivateCrtKeyParameters rsaPrivateKey = (RSAPrivateCrtKeyParameters) privateKey;

				final PrivateKeyInfo info = new PrivateKeyInfo(
						new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE),
						new RSAPrivateKey(
								rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent(), rsaPrivateKey.getExponent(),
								rsaPrivateKey.getP(), rsaPrivateKey.getQ(), rsaPrivateKey.getDP(),
								rsaPrivateKey.getDQ(), rsaPrivateKey.getQInv()).toASN1Primitive()
						);
				return info.getEncoded();
			}
		} catch (final IOException x) {
			throw new RuntimeException(x);
		}

		throw new UnsupportedOperationException("privateKey.class=\"" + privateKey.getClass().getName() + "\" not yet supported!");
	}

	private final Map<String, MACCalculatorFactory> macName2macCalculatorFactory = new HashMap<String, MACCalculatorFactory>();

	private void registerMACCalculatorFactory(final String macName, final MACCalculatorFactory factory)
	{
		if (macName != null)
			factory.setAlgorithmName(macName);

		logger.trace("registerMACCalculatorFactory: algorithmName=\"{}\" factoryClass=\"{}\"", factory.getAlgorithmName(), factory.getClass().getName());
		macName2macCalculatorFactory.put(factory.getAlgorithmName(), factory);
	}

	@SuppressWarnings("deprecation")
	private void registerDeprecatedMACCalculatorFactories()
	{
		registerMACCalculatorFactory("OLDHMACSHA384", new MACCalculatorFactoryImpl.OldSHA384());
		registerMACCalculatorFactory("OLDHMACSHA512", new MACCalculatorFactoryImpl.OldSHA512());
	}

	{
		registerMACCalculatorFactory("DES", new MACCalculatorFactoryImpl.DES());
		registerMACCalculatorFactory("DESMAC", new MACCalculatorFactoryImpl.DES());

		registerMACCalculatorFactory("DES64", new MACCalculatorFactoryImpl.DES64());
		registerMACCalculatorFactory("DES64MAC", new MACCalculatorFactoryImpl.DES64());

		registerMACCalculatorFactory("DES/CFB8", new MACCalculatorFactoryImpl.DESCFB8());
		registerMACCalculatorFactory("DESMAC/CFB8", new MACCalculatorFactoryImpl.DESCFB8());

		registerMACCalculatorFactory("DESWITHISO9797", new MACCalculatorFactoryImpl.DES9797Alg3());
		registerMACCalculatorFactory("DESWITHISO9797MAC", new MACCalculatorFactoryImpl.DES9797Alg3());

		registerMACCalculatorFactory("ISO9797ALG3", new MACCalculatorFactoryImpl.DES9797Alg3());
		registerMACCalculatorFactory("ISO9797ALG3MAC", new MACCalculatorFactoryImpl.DES9797Alg3());

		registerMACCalculatorFactory("ISO9797ALG3WITHISO7816-4PADDING", new MACCalculatorFactoryImpl.DES9797Alg3with7816d4());
		registerMACCalculatorFactory("ISO9797ALG3MACWITHISO7816-4PADDING", new MACCalculatorFactoryImpl.DES9797Alg3with7816d4());

		registerMACCalculatorFactory("RC2", new MACCalculatorFactoryImpl.RC2());
		registerMACCalculatorFactory("RC2MAC", new MACCalculatorFactoryImpl.RC2());

		registerMACCalculatorFactory("RC2/CFB8", new MACCalculatorFactoryImpl.RC2CFB8());
		registerMACCalculatorFactory("RC2MAC/CFB8", new MACCalculatorFactoryImpl.RC2CFB8());

		registerMACCalculatorFactory("GOST28147", new MACCalculatorFactoryImpl.GOST28147());
		registerMACCalculatorFactory("GOST28147MAC", new MACCalculatorFactoryImpl.GOST28147());

		registerDeprecatedMACCalculatorFactories();

		registerMACCalculatorFactory("HMACMD2", new MACCalculatorFactoryImpl.MD2());
		registerMACCalculatorFactory("HMAC-MD2", new MACCalculatorFactoryImpl.MD2());
		registerMACCalculatorFactory("HMAC/MD2", new MACCalculatorFactoryImpl.MD2());

		registerMACCalculatorFactory("HMACMD4", new MACCalculatorFactoryImpl.MD4());
		registerMACCalculatorFactory("HMAC-MD4", new MACCalculatorFactoryImpl.MD4());
		registerMACCalculatorFactory("HMAC/MD4", new MACCalculatorFactoryImpl.MD4());

		registerMACCalculatorFactory("HMACMD5", new MACCalculatorFactoryImpl.MD5());
		registerMACCalculatorFactory("HMAC-MD5", new MACCalculatorFactoryImpl.MD5());
		registerMACCalculatorFactory("HMAC/MD5", new MACCalculatorFactoryImpl.MD5());

		registerMACCalculatorFactory("HMACSHA1", new MACCalculatorFactoryImpl.SHA1());
		registerMACCalculatorFactory("HMAC-SHA1", new MACCalculatorFactoryImpl.SHA1());
		registerMACCalculatorFactory("HMAC/SHA1", new MACCalculatorFactoryImpl.SHA1());

		registerMACCalculatorFactory("HMACSHA224", new MACCalculatorFactoryImpl.SHA224());
		registerMACCalculatorFactory("HMAC-SHA224", new MACCalculatorFactoryImpl.SHA224());
		registerMACCalculatorFactory("HMAC/SHA224", new MACCalculatorFactoryImpl.SHA224());

		registerMACCalculatorFactory("HMACSHA256", new MACCalculatorFactoryImpl.SHA256());
		registerMACCalculatorFactory("HMAC-SHA256", new MACCalculatorFactoryImpl.SHA256());
		registerMACCalculatorFactory("HMAC/SHA256", new MACCalculatorFactoryImpl.SHA256());

		registerMACCalculatorFactory("HMACSHA384", new MACCalculatorFactoryImpl.SHA384());
		registerMACCalculatorFactory("HMAC-SHA384", new MACCalculatorFactoryImpl.SHA384());
		registerMACCalculatorFactory("HMAC/SHA384", new MACCalculatorFactoryImpl.SHA384());

		registerMACCalculatorFactory("HMACSHA512", new MACCalculatorFactoryImpl.SHA512());
		registerMACCalculatorFactory("HMAC-SHA512", new MACCalculatorFactoryImpl.SHA512());
		registerMACCalculatorFactory("HMAC/SHA512", new MACCalculatorFactoryImpl.SHA512());

		registerMACCalculatorFactory("HMACRIPEMD128", new MACCalculatorFactoryImpl.RIPEMD128());
		registerMACCalculatorFactory("HMAC-RIPEMD128", new MACCalculatorFactoryImpl.RIPEMD128());
		registerMACCalculatorFactory("HMAC/RIPEMD128", new MACCalculatorFactoryImpl.RIPEMD128());

		registerMACCalculatorFactory("HMACRIPEMD160", new MACCalculatorFactoryImpl.RIPEMD160());
		registerMACCalculatorFactory("HMAC-RIPEMD160", new MACCalculatorFactoryImpl.RIPEMD160());
		registerMACCalculatorFactory("HMAC/RIPEMD160", new MACCalculatorFactoryImpl.RIPEMD160());

		registerMACCalculatorFactory("HMACTIGER", new MACCalculatorFactoryImpl.Tiger());
		registerMACCalculatorFactory("HMAC-TIGER", new MACCalculatorFactoryImpl.Tiger());
		registerMACCalculatorFactory("HMAC/TIGER", new MACCalculatorFactoryImpl.Tiger());
	}

	/**
	 * <p>
	 * Create a <a target="_blank" href="http://en.wikipedia.org/wiki/Message_authentication_code">MAC</a> calculator.
	 * </p>
	 *
	 * @param algorithmName the name of the MAC algorithm. See <a target="_blank" href="http://cumulus4j.org/${project.version}/documentation/supported-algorithms.html">Supported algorithms</a>
	 * for a list of supported algorithms or use {@link #getSupportedMACAlgorithms()} to query them.
	 * @param initWithDefaults whether to
	 * {@link MACCalculator#init(org.bouncycastle.crypto.CipherParameters) initialise} the <code>MACCalculator</code> with default values
	 * so that it can be used immediately as-is.
	 * @return a new instance of {@link MACCalculator} (iff <code>initWithDefaults==true</code> ready-to-use;
	 * otherwise requiring {@link MACCalculator#init(org.bouncycastle.crypto.CipherParameters) initialisation}
	 * before it can be used).
	 * @throws NoSuchAlgorithmException if there is no {@link MACCalculatorFactory} registered to suit the given <code>algorithmName</code>.
	 * @see #getSupportedMACAlgorithms()
	 */
	public MACCalculator createMACCalculator(final String algorithmName, final boolean initWithDefaults)
	throws NoSuchAlgorithmException
	{
		if (algorithmName == null)
			throw new IllegalArgumentException("algorithmName == null");

		final MACCalculatorFactory factory = macName2macCalculatorFactory.get(algorithmName.toUpperCase(Locale.ENGLISH));
		if (factory == null)
			throw new NoSuchAlgorithmException("There is no MAC calculator registered for algorithmName=\"" + algorithmName.toUpperCase(Locale.ENGLISH) + "\"!");

		return factory.createMACCalculator(initWithDefaults);
	}

	/**
	 * Get all supported <a target="_blank" href="http://en.wikipedia.org/wiki/Message_authentication_code">MAC</a> algorithms.
	 * {@link #createMACCalculator(String, boolean)} should be able to return a {@link MACCalculator} for each of them.
	 * @return all supported MAC algorithms.
	 * @see #createMACCalculator(String, boolean)
	 */
	public Set<String> getSupportedMACAlgorithms()
	{
		return Collections.unmodifiableSet(new TreeSet<String>(macName2macCalculatorFactory.keySet()));
	}
}
