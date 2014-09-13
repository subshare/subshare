package org.subshare.local.persistence;

import java.util.Iterator;

import co.codewizards.cloudstore.local.persistence.Dao;

public class RepositoryOwnerDao extends Dao<RepositoryOwner, RepositoryOwnerDao> {

	public RepositoryOwner getRepositoryOwnerOrFail() {
		final RepositoryOwner repositoryOwner = getRepositoryOwner();
		if (repositoryOwner == null)
			throw new IllegalStateException("RepositoryOwner entity not found in database.");

		return repositoryOwner;
	}

	public RepositoryOwner getRepositoryOwner() {
		final Iterator<RepositoryOwner> iterator = pm().getExtent(RepositoryOwner.class).iterator();
		if (!iterator.hasNext())
			return null;

		final RepositoryOwner repositoryOwner = iterator.next();
		if (iterator.hasNext()) {
			throw new IllegalStateException("Multiple RepositoryOwner entities in database.");
		}
		return repositoryOwner;
	}

}
