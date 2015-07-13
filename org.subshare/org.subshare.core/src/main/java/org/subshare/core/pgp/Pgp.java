package org.subshare.core.pgp;

import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Set;

import co.codewizards.cloudstore.core.bean.PropertyBase;
import co.codewizards.cloudstore.core.oio.File;

public interface Pgp {

	interface Property extends PropertyBase { }

	enum PropertyEnum implements Property {
		localRevision
	}

	int getPriority();

	boolean isSupported();

	Collection<PgpKey> getMasterKeys();

	PgpKey getPgpKey(PgpKeyId pgpKeyId);

	PgpKey createPgpKey(CreatePgpKeyParam createPgpKeyParam);

	PgpEncoder createEncoder(InputStream in, OutputStream out);

	PgpDecoder createDecoder(InputStream in, OutputStream out);

	/**
	 * Gets the certifications for the authenticity of the given {@code pgpKey}.
	 * @param pgpKey the key for which to look up the certifications. Must not be <code>null</code>.
	 * @return the certifications proving the authenticity of the given {@code pgpKey}. Never <code>null</code>; and usually
	 * never empty, either, because a PGP key should at least be self-signed.
	 */
	Collection<PgpSignature> getCertifications(PgpKey pgpKey);

	Collection<PgpKey> getMasterKeysWithSecretKey();

	boolean isTrusted(PgpKey pgpKey);

	PgpKeyTrustLevel getKeyTrustLevel(PgpKey pgpKey);

	void exportPublicKeys(Set<PgpKey> pgpKeys, File file);

	void exportPublicKeysWithSecretKeys(Set<PgpKey> pgpKeys, File file);

	void exportPublicKeys(Set<PgpKey> pgpKeys, OutputStream out);

	byte[] exportPublicKeys(Set<PgpKey> pgpKeys);

	/**
	 * Export the keys identified by {@code pgpKeys} to the given stream.
	 * <p>
	 * In contrast to {@link #exportPublicKeys(Set, OutputStream)}, this method also includes the secret keys.
	 * Please note the difference between <i>secret</i> and <i>private</i>: The <i>private</i> key is the unprotected,
	 * decrypted key itself. The <i>secret</i> key, however, is the passphrase-protected form of the <i>private</i>
	 * key.
	 * @param pgpKeys the keys to be exported. Must not be <code>null</code>.
	 * @param out the stream to write to. Must not be <code>null</code>.
	 */
	void exportPublicKeysWithSecretKeys(Set<PgpKey> pgpKeys, OutputStream out);

	byte[] exportPublicKeysWithSecretKeys(Set<PgpKey> pgpKeys);

	ImportKeysResult importKeys(InputStream in);

	ImportKeysResult importKeys(File file);

	ImportKeysResult importKeys(byte[] data);

	/**
	 * Gets the <i>global</i> local-revision.
	 * <p>
	 * Whenever any key is added or changed (e.g. a new signature added).
	 * @return the <i>global</i> local-revision.
	 */
	long getLocalRevision();

	/**
	 * Gets the local-revision of the given {@code pgpKey}.
	 * @param pgpKey
	 * @return
	 */
	long getLocalRevision(PgpKey pgpKey);

	void addPropertyChangeListener(PropertyChangeListener listener);
	void addPropertyChangeListener(Property property, PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);
	void removePropertyChangeListener(Property property, PropertyChangeListener listener);

	/**
	 * Tests whether the given {@code pgpKey}'s secret key can be decrypted using the given {@code passphrase}.
	 * <p>
	 * This method tries to obtain the <i>private</i> key by decrypting the <i>secret</i> key of the given
	 * {@code pgpKey}. If this succeeds using the given {@code passphrase}, it returns <code>true</code>,
	 * otherwise it returns <code>false</code>.
	 *
	 * @param pgpKey the key whose secret part is to be decrypted. Must not be <code>null</code>.
	 * @param passphrase the passphrase used to decrypt. Must not be <code>null</code>, but may be empty (if the
	 * private key is not protected).
	 * @return <code>true</code>, if the given passphrase matches the secret key; i.e. the private key can be obtained
	 * from it. <code>false</code> otherwise.
	 * @throws IllegalArgumentException if one of the parameters is <code>null</code> or the given {@code pgpKey}
	 * contains solely the public key - no secret key.
	 */
	boolean testPassphrase(PgpKey pgpKey, char[] passphrase) throws IllegalArgumentException;
}
