package org.cumulus4j.crypto.test;

import org.subshare.crypto.CipherEngineType;
import org.subshare.crypto.CryptoRegistry;
import org.junit.Test;

/**
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public class ShowSupportedAlgosForDocumentation
{
	private static CryptoRegistry cryptoRegistry = CryptoRegistry.sharedInstance();

	@Test
	public void showSupportedCipherEngines() {
		for (CipherEngineType cipherEngineType : CipherEngineType.values()) {
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			System.out.println("*** CipherEngine/" + cipherEngineType + " ***");
			for (String cipherEngine : cryptoRegistry.getSupportedCipherEngines(cipherEngineType))
				System.out.println(cipherEngine);

			System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		}
	}

	@Test
	public void showSupportedBlockCipherModes() {
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			System.out.println("*** BlockCipherMode ***");
			for (String mode : cryptoRegistry.getSupportedCipherModes(null))
				System.out.println(mode);

			System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
	}

	@Test
	public void showSupportedPaddings() {
		for (CipherEngineType cipherEngineType : CipherEngineType.values()) {
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
			System.out.println("*** Padding/" + cipherEngineType + " ***");
			for (String padding : cryptoRegistry.getSupportedCipherPaddings(cipherEngineType))
				System.out.println(padding);

			System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		}
	}

	@Test
	public void showSupportedMACs() {
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		System.out.println("*** MAC ***");
		for (String mac : cryptoRegistry.getSupportedMACAlgorithms())
			System.out.println(mac);

		System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
	}

}
