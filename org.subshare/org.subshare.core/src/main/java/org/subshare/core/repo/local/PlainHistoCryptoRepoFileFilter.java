package org.subshare.core.repo.local;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;

import co.codewizards.cloudstore.core.Uid;

@SuppressWarnings("serial")
public class PlainHistoCryptoRepoFileFilter implements Serializable, Cloneable {

	private Set<Uid> histoCryptoRepoFileIds;
	private Uid histoFrameId;
	private boolean fillParents = true;
	private boolean withFileChunkDtos;
	private String localPath;
	private Set<Uid> collisionIds;

	public PlainHistoCryptoRepoFileFilter() {
	}

	public Set<Uid> getHistoCryptoRepoFileIds() {
		return histoCryptoRepoFileIds;
	}
	public void setHistoCryptoRepoFileIds(Set<Uid> histoCryptoRepoFileIds) {
		this.histoCryptoRepoFileIds = histoCryptoRepoFileIds;
	}

	public Uid getHistoFrameId() {
		return histoFrameId;
	}
	public void setHistoFrameId(Uid histoFrameId) {
		this.histoFrameId = histoFrameId;
	}

	/**
	 * Indicates whether parents should be resolved and filled into the result.
	 * <p>
	 * The {@code HistoFrame} might only contain a file without any of its parent directories.
	 * This makes building a tree impossible. Setting this flag <code>true</code> causes
	 * the parents to be extrapolated (up to the root).
	 * <p>
	 * Please note, that
	 * {@link PlainHistoCryptoRepoFileDto#getHistoCryptoRepoFileDto() PlainHistoCryptoRepoFileDto.histoCryptoRepoFileDto}
	 * is <code>null</code> for extrapolated parents.
	 * @return whether parents should be resolved and filled into the result.
	 */
	public boolean isFillParents() {
		return fillParents;
	}
	public void setFillParents(boolean fillToRoot) {
		this.fillParents = fillToRoot;
	}

	public String getLocalPath() {
		return localPath;
	}
	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	public Set<Uid> getCollisionIds() {
		return collisionIds;
	}
	public void setCollisionIds(Set<Uid> collisionIds) {
		this.collisionIds = collisionIds;
	}

	public boolean isWithFileChunkDtos() {
		return withFileChunkDtos;
	}
	public void setWithFileChunkDtos(boolean withFileChunkDtos) {
		this.withFileChunkDtos = withFileChunkDtos;
	}

	@Override
	public PlainHistoCryptoRepoFileFilter clone() {
		final PlainHistoCryptoRepoFileFilter clone;
		try {
			clone = (PlainHistoCryptoRepoFileFilter) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // should really never happen!
		}

		if (this.histoCryptoRepoFileIds != null)
			clone.histoCryptoRepoFileIds = new HashSet<>(this.histoCryptoRepoFileIds);

		if (this.collisionIds != null)
			clone.collisionIds = new HashSet<>(this.collisionIds);

		return clone;
	}
}
