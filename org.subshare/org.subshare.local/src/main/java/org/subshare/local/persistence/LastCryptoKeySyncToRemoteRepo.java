package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.RemoteRepository;

@PersistenceCapable
@Unique(name="LastCryptoKeySyncToRemoteRepo_remoteRepository", members="remoteRepository")
@Query(name="getLastCryptoKeySyncToRemoteRepo_remoteRepository", value="SELECT UNIQUE WHERE this.remoteRepository == :remoteRepository")
public class LastCryptoKeySyncToRemoteRepo extends Entity {

	private static final Logger logger = LoggerFactory.getLogger(LastCryptoKeySyncToRemoteRepo.class);

	@Persistent(nullValue=NullValue.EXCEPTION)
	private RemoteRepository remoteRepository;
	private long localRepositoryRevisionSynced = -1;
	private long localRepositoryRevisionInProgress = -1;
	private boolean resyncMode;

	public RemoteRepository getRemoteRepository() {
		return remoteRepository;
	}
	public void setRemoteRepository(final RemoteRepository remoteRepository) {
		if (! equal(this.remoteRepository, remoteRepository))
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
	public void setLocalRepositoryRevisionSynced(final long revision) {
		if (! equal(this.localRepositoryRevisionSynced, revision)) {
			logger.info("setLocalRepositoryRevisionSynced: remoteRepositoryId={}, this.localRepositoryRevisionSynced={}, revision={}",
					(remoteRepository == null ? null : remoteRepository.getRepositoryId()),
					this.localRepositoryRevisionSynced, revision);
			this.localRepositoryRevisionSynced = revision;
		}
	}

	public long getLocalRepositoryRevisionInProgress() {
		return localRepositoryRevisionInProgress;
	}
	public void setLocalRepositoryRevisionInProgress(final long revision) {
		if (! equal(this.localRepositoryRevisionInProgress, revision)) {
			logger.info("setLocalRepositoryRevisionInProgress: remoteRepositoryId={}, this.localRepositoryRevisionInProgress={}, revision={}",
					(remoteRepository == null ? null : remoteRepository.getRepositoryId()),
					this.localRepositoryRevisionInProgress, revision);
			this.localRepositoryRevisionInProgress = revision;
		}
	}

	public boolean isResyncMode() {
		return resyncMode;
	}
	public void setResyncMode(boolean resyncMode) {
		this.resyncMode = resyncMode;
	}
}
