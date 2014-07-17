/**
 * <p>
 * API providing a unified way to use various cryptography algorithms.
 * </p><p>
 * For example, there is the {@link org.subshare.crypto.Cipher}
 * which provides a generic API for <a target="_blank" href="http://en.wikipedia.org/wiki/Symmetric_encryption">symmetric</a> and
 * <a target="_blank" href="http://en.wikipedia.org/wiki/Public-key_cryptography">asymmetric</a> encryption (&amp; decryption) or there is the
 * {@link org.subshare.crypto.MACCalculator} for calculating <a target="_blank" href="http://en.wikipedia.org/wiki/Message_authentication_code">message
 * authentication codes</a>.
 * </p><p>
 * This API allows for the simple configuration of the algorithms used by your application as the
 * {@link org.subshare.crypto.CryptoRegistry} binds an algorithm name (i.e. a <code>String</code>) to an implementation of one
 * of the API's interfaces.
 * </p>
 */
package org.subshare.crypto;
