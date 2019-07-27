package org.subshare.local.persistence;

import static java.util.Objects.*;

import java.util.UUID;

import javax.jdo.Query;

import co.codewizards.cloudstore.local.persistence.Dao;

public class RepositoryOwnerDao extends Dao<RepositoryOwner, RepositoryOwnerDao> {

	public RepositoryOwner getRepositoryOwnerOrFail(final UUID serverRepositoryId) {
		final RepositoryOwner repositoryOwner = getRepositoryOwner(serverRepositoryId);
		if (repositoryOwner == null)
			throw new IllegalStateException(String.format("RepositoryOwner with serverRepositoryId=%s not found!", serverRepositoryId));

		return repositoryOwner;
	}

	public RepositoryOwner getRepositoryOwner(final UUID serverRepositoryId) {
		requireNonNull(serverRepositoryId, "serverRepositoryId");
		final Query q = pm().newNamedQuery(getEntityClass(), "getRepositoryOwner_serverRepositoryId");
		final RepositoryOwner repositoryOwner = (RepositoryOwner) q.execute(serverRepositoryId.toString());
		return repositoryOwner;
	}

	@Override
	public <P extends RepositoryOwner> P makePersistent(P entity) {
		return super.makePersistent(entity);
	}
}
