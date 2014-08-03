package org.subshare.local.persistence;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;

@PersistenceCapable
@Unique(name="LastCryptoKeySyncToRemoteRepo_remoteRepository", members="remoteRepository")
@Query(name="getLastCryptoKeySyncToRemoteRepo_remoteRepository", value="SELECT UNIQUE WHERE this.remoteRepository == :remoteRepository")
public class LastCryptoKeySyncToRemoteRepo extends Entity {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private RemoteRepository remoteRepository;
	private long localRepositoryRevisionSynced = -1;
	private long localRepositoryRevisionInProgress = -1;

	public RemoteRepository getRemoteRepository() {
		return remoteRepository;
	}
	public void setRemoteRepository(final RemoteRepository remoteRepository) {
		this.remoteRepository = remoteRepository;
	}

	/**
	 * Gets the {@link LocalRepository#getRevision() LocalRepository.revision} that
	 * was the most recent, when keys were synced to the remote repository.
	 * <p>
	 * This means all local changes of {@link CryptoKey} and {@link CryptoLink} with a
	 * {@link AutoTrackLocalRevision#getLocalRevision() localRevision}
	 * greater than (&gt;) this revision are not yet sent to the remote repo.
	 * @return the {@link LocalRepository#getRevision() LocalRepository.revision} that
	 * was the most recent, when keys were synced to the remote repository.
	 */
	public long getLocalRepositoryRevisionSynced() {
		return localRepositoryRevisionSynced;
	}
	public void setLocalRepositoryRevisionSynced(final long localRepositoryRevision) {
		this.localRepositoryRevisionSynced = localRepositoryRevision;
	}

	public long getLocalRepositoryRevisionInProgress() {
		return localRepositoryRevisionInProgress;
	}
	public void setLocalRepositoryRevisionInProgress(final long localRepositoryRevisionInProgress) {
		this.localRepositoryRevisionInProgress = localRepositoryRevisionInProgress;
	}
}
