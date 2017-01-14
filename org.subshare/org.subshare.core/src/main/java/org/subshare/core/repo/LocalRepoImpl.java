package org.subshare.core.repo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.UUID;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.util.IOUtil;

public class LocalRepoImpl implements LocalRepo {
	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	private final UUID repositoryId;
	private String name;
	private File localRoot;

	public LocalRepoImpl(final UUID repositoryId) {
		this.repositoryId = assertNotNull(repositoryId, "repositoryId)");
	}

	@Override
	public UUID getRepositoryId() {
		return repositoryId;
	}

	@Override
	public String getName() {
		return name;
	}
	@Override
	public void setName(final String name) {
		final String old = this.name;
		this.name = name;
		firePropertyChange(PropertyEnum.name, old, name);
	}

	@Override
	public File getLocalRoot() {
		return localRoot;
	}
	@Override
	public void setLocalRoot(final File localRoot) {
		final File old = this.localRoot;
		this.localRoot = localRoot;
		firePropertyChange(PropertyEnum.localRoot, old, localRoot);
	}

	@Override
	public String getLocalPath(final File file) {
		assertNotNull(file, "file");
		assertNotNull(localRoot, "localRoot");

		if (file.equals(localRoot))
			return "";

		final String relativePath;
		try {
			relativePath = IOUtil.getRelativePath(localRoot, file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if (relativePath.startsWith("..") || relativePath.startsWith("/"))
			throw new IllegalArgumentException(String.format("file '%s' is not located inside repository's root '%s'!", file.getPath(), localRoot.getPath()));

		return '/' + relativePath.replace(java.io.File.separatorChar, '/');
	}

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
	public LocalRepo clone() {
		final LocalRepoImpl clone;
		try {
			clone = (LocalRepoImpl) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		clone.propertyChangeSupport = new PropertyChangeSupport(clone);
		return clone;
	}
}
