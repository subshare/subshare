package org.subshare.core.sign;

import java.io.InputStream;

import org.subshare.core.user.UserRepoKey;

/**
 * A {@code PgpSignable} can be signed and its data is thus protected.
 * <p>
 * In contrast to a {@link Signable}, a {@code PgpSignable} is directly signed with an OpenPGP key - <i>not</i>
 * with a {@link UserRepoKey}. An external attacker can thus see the identity
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface PgpSignable {

	String getSignedDataType();

	/**
	 * Gets the current version of the signed data, used when signing right now.
	 * <p>
	 * If a new field is added to an object implementing {@code Signable} later and this new field's data is
	 * included in the signature, the value returned by this method must be incremented to avoid breaking all
	 * old signatures (of all previously signed data).
	 * <p>
	 * {@link #getSignedData(int)} then must exclude the newly added field for all version numbers lower than
	 * the just incremented value.
	 * <p>
	 * It is recommended to start with the value 0.
	 * @return the current version of newly signed data.
	 */
	int getSignedDataVersion();

	/**
	 * Gets a binary representation of this object's data being signed.
	 * <p>
	 * Multiple invocations of this method must return the very same bytes, if the object's data was not
	 * modified.
	 * @param signedDataVersion version of the signed data. If a new field was added later, it must not be
	 * contained in the returned {@code InputStream}, if {@code signedDataVersion} is less than the version
	 * introducing the new field.
	 * @return a stream with the object's data to be signed. Never <code>null</code>.
	 * @see #getSignedDataVersion()
	 */
	InputStream getSignedData(int signedDataVersion);

	/**
	 * Gets the detached PGP signature.
	 * @return the detached PGP signature.
	 */
	byte[] getPgpSignatureData();

	void setPgpSignatureData(byte[] pgpSignatureData);

}
