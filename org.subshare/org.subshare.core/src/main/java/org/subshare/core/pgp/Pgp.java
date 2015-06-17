package org.subshare.core.pgp;

import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Set;

import co.codewizards.cloudstore.core.bean.PropertyBase;

public interface Pgp {

	interface Property extends PropertyBase { }

	enum PropertyEnum implements Property {
		localRevision
	}

	int getPriority();

	boolean isSupported();

	Collection<PgpKey> getMasterKeys();

	PgpKey getPgpKey(PgpKeyId pgpKeyId);

	PgpEncoder createEncoder(InputStream in, OutputStream out);

	PgpDecoder createDecoder(InputStream in, OutputStream out);

	Collection<PgpSignature> getUserIdSignatures(PgpKey pgpKey);

	Collection<PgpKey> getMasterKeysWithPrivateKey();

	boolean isTrusted(PgpKey pgpKey);

	PgpKeyTrustLevel getKeyTrustLevel(PgpKey pgpKey);

	void exportPublicKeys(Set<PgpKey> pgpKeys, OutputStream out);

	void exportPublicKeysWithPrivateKeys(Set<PgpKey> pgpKeys, OutputStream out);

	void importKeys(InputStream in);

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

	void testPassphrase(PgpKey pgpKey, char[] passphrase) throws IllegalArgumentException, SecurityException;
}
