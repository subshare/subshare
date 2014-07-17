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
 * Type of a cipher engine (a raw <a target="_blank" href="http://en.wikipedia.org/wiki/Encryption_algorithm">encryption algorithm</a>).
 * @author Marco หงุ่ยตระกูล-Schulze - marco at nightlabs dot de
 */
public enum CipherEngineType
{
	/**
	 * Indicates a <a target="_blank" href="http://en.wikipedia.org/wiki/Symmetric_key_algorithm">symmetric</a> <a target="_blank" href="http://en.wikipedia.org/wiki/Block_cipher">block cipher</a>.
	 */
	symmetricBlock,

	/**
	 * Indicates an asymmetric block cipher (aka <a target="_blank" href="http://en.wikipedia.org/wiki/Public_key_cryptography">public key cryptography</a>).
	 */
	asymmetricBlock,

	/**
	 * Indicates a <a target="_blank" href="http://en.wikipedia.org/wiki/Symmetric_key_algorithm">symmetric</a> <a target="_blank" href="http://en.wikipedia.org/wiki/Stream_cipher">stream cipher</a>.
	 */
	symmetricStream
}
