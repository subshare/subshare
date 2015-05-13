package org.subshare.local.persistence;

import java.util.UUID;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.subshare.core.repo.CreateRepositoryContext;

import co.codewizards.cloudstore.local.persistence.LocalRepository;

@PersistenceCapable
@Inheritance(strategy=InheritanceStrategy.SUPERCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.VALUE_MAP, value="SsLocalRepository")
public class SsLocalRepository extends LocalRepository {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Column(jdbcType="INTEGER")
	private LocalRepositoryType localRepositoryType = LocalRepositoryType.UNINITIALISED;

	private boolean assertedAllRepoFilesAreSigned;

	public LocalRepositoryType getLocalRepositoryType() {
		return localRepositoryType;
	}
	public void setLocalRepositoryType(final LocalRepositoryType localRepositoryType) {
		this.localRepositoryType = localRepositoryType;
	}

	public boolean isAssertedAllRepoFilesAreSigned() {
		return assertedAllRepoFilesAreSigned;
	}
	public void setAssertedAllRepoFilesAreSigned(final boolean assertedAllRepoFilesAreSigned) {
		this.assertedAllRepoFilesAreSigned = assertedAllRepoFilesAreSigned;
	}

	@Override
	protected UUID createRepositoryId() {
		final UUID repositoryId = CreateRepositoryContext.repositoryIdThreadLocal.get();
		if (repositoryId != null)
			return repositoryId;

		return super.createRepositoryId();
	}
}
