package org.subshare.core.repo.local;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import co.codewizards.cloudstore.core.dto.Uid;

@SuppressWarnings("serial")
public class CollisionPrivateFilter implements Serializable, Cloneable {

	private Set<Uid> collisionIds;

	private Uid cryptoRepoFileId;

	private Uid histoCryptoRepoFileId;

	private String localPath;

	private Boolean resolved;

	private boolean includeChildrenRecursively;

	public Set<Uid> getCollisionIds() {
		return collisionIds;
	}
	public void setCollisionIds(Set<Uid> collisionIds) {
		this.collisionIds = collisionIds;
	}

	public Uid getCryptoRepoFileId() {
		return cryptoRepoFileId;
	}
	public void setCryptoRepoFileId(Uid cryptoRepoFileId) {
		this.cryptoRepoFileId = cryptoRepoFileId;
	}

	public Uid getHistoCryptoRepoFileId() {
		return histoCryptoRepoFileId;
	}
	public void setHistoCryptoRepoFileId(Uid histoCryptoRepoFileId) {
		this.histoCryptoRepoFileId = histoCryptoRepoFileId;
	}

	public String getLocalPath() {
		return localPath;
	}
	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	public Boolean getResolved() {
		return resolved;
	}
	public void setResolved(Boolean resolved) {
		this.resolved = resolved;
	}

	/**
	 * Whether to include {@code Collision}s associated with sub-directories or files within the specified
	 * directory or any of its sub-directories.
	 * <p>
	 * Can be used in combination with {@link #getLocalPath() localPath} or {@link #getCryptoRepoFileId() cryptoRepoFileId};
	 * @return
	 */
	public boolean isIncludeChildrenRecursively() {
		return includeChildrenRecursively;
	}
	public void setIncludeChildrenRecursively(boolean includeChildrenRecursively) {
		this.includeChildrenRecursively = includeChildrenRecursively;
	}

	@Override
	public CollisionPrivateFilter clone() {
		final CollisionPrivateFilter clone;
		try {
			clone = (CollisionPrivateFilter) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // should really never happen!
		}

		if (this.collisionIds != null)
			clone.collisionIds = new HashSet<>(this.collisionIds);

		return clone;
	}
}
