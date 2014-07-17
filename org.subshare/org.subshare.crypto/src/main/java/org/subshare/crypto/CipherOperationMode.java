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

/**
 * Operation mode of a {@link Cipher}. Used to
 * {@link Cipher#init(CipherOperationMode, org.bouncycastle.crypto.CipherParameters) initialise} a cipher.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public enum CipherOperationMode
{
	/**
	 * Operation mode is encryption (from <a target="_blank" href="http://en.wikipedia.org/wiki/Plaintext">plaintext</a> to <a target="_blank" href="http://en.wikipedia.org/wiki/Ciphertext">ciphertext</a>).
	 */
	ENCRYPT,

	/**
	 * Operation mode is decryption (from <a target="_blank" href="http://en.wikipedia.org/wiki/Ciphertext">ciphertext</a> to <a target="_blank" href="http://en.wikipedia.org/wiki/Plaintext">plaintext</a>).
	 */
	DECRYPT
}
