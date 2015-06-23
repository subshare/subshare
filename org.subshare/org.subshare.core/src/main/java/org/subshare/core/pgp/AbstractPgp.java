package org.subshare.core.pgp;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import co.codewizards.cloudstore.core.oio.File;

public abstract class AbstractPgp implements Pgp {

	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	@Override
	public PgpEncoder createEncoder(final InputStream in, final OutputStream out) {
		final PgpEncoder encoder = _createEncoder();
		encoder.setInputStream(in);
		encoder.setOutputStream(out);
		return encoder;
	}

	protected abstract PgpEncoder _createEncoder();

	@Override
	public PgpDecoder createDecoder(final InputStream in, final OutputStream out) {
		final PgpDecoder decoder = _createDecoder();
		decoder.setInputStream(in);
		decoder.setOutputStream(out);
		return decoder;
	}

	protected abstract PgpDecoder _createDecoder();

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	@Override
	public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
	}
	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}
	@Override
	public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
	}

	protected void firePropertyChange(Property property, Object oldValue, Object newValue) {
		propertyChangeSupport.firePropertyChange(property.name(), oldValue, newValue);
	}

	@Override
	public void exportPublicKeysWithPrivateKeys(final Set<PgpKey> pgpKeys, final File file) {
		assertNotNull("pgpKeys", pgpKeys);
		try {
			try (OutputStream out = assertNotNull("file", file).createOutputStream();) {
				exportPublicKeysWithPrivateKeys(pgpKeys, out);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void exportPublicKeys(final Set<PgpKey> pgpKeys, final File file) {
		assertNotNull("pgpKeys", pgpKeys);
		try {
			try (OutputStream out = assertNotNull("file", file).createOutputStream();) {
				exportPublicKeys(pgpKeys, out);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ImportKeysResult importKeys(final File file) {
		try {
			try (InputStream in = assertNotNull("file", file).createInputStream();) {
				return importKeys(in);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
