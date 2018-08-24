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
@Unique(name="LastCryptoKeySyncFromRemoteRepo_remoteRepository", members="remoteRepository")
@Query(name="getLastCryptoKeySyncFromRemoteRepo_remoteRepository", value="SELECT UNIQUE WHERE this.remoteRepository == :remoteRepository")
public class LastCryptoKeySyncFromRemoteRepo extends Entity {

	private static final Logger logger = LoggerFactory.getLogger(LastCryptoKeySyncFromRemoteRepo.class);

	@Persistent(nullValue=NullValue.EXCEPTION)
	private RemoteRepository remoteRepository;
	private long remoteRepositoryRevisionSynced = Long.MIN_VALUE;

	public RemoteRepository getRemoteRepository() {
		return remoteRepository;
	}
	public void setRemoteRepository(final RemoteRepository remoteRepository) {
		if (! equal(this.remoteRepository, remoteRepository))
			this.remoteRepository = remoteRepository;
	}

	/**
	 * Gets the {@link LocalRepository#getRevision() LocalRepository.revision}
	 * of the remote repository that was the most recent, when keys were synced
	 * to the local repository.
	 * <p>
	 * This means all remote changes of {@link CryptoKey} and {@link CryptoLink} with a
	 * {@link AutoTrackLocalRevision#getLocalRevision() localRevision}
	 * greater than (&gt;) this revision are not yet sent to the local repo.
	 * @return the {@link LocalRepository#getRevision() LocalRepository.revision}
	 * of the remote repository that was the most recent, when keys were synced to the
	 * local repository.
	 */
	public long getRemoteRepositoryRevisionSynced() {
		return remoteRepositoryRevisionSynced;
	}
	public void setRemoteRepositoryRevisionSynced(final long revision) {
		if (! equal(this.remoteRepositoryRevisionSynced, revision)) {
			logger.info("setRemoteRepositoryRevisionSynced: remoteRepositoryId={}, this.remoteRepositoryRevisionSynced={}, revision={}",
					(remoteRepository == null ? null : remoteRepository.getRepositoryId()),
					this.remoteRepositoryRevisionSynced, revision);
			this.remoteRepositoryRevisionSynced = revision;
		}
	}
}
