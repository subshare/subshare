package org.subshare.core.repo.local;

import java.io.Serializable;

import org.subshare.core.dto.PlainHistoCryptoRepoFileDto;

import co.codewizards.cloudstore.core.dto.Uid;

@SuppressWarnings("serial")
public class PlainHistoCryptoRepoFileFilter implements Serializable {

	private Uid histoFrameId;
	private boolean fillParents = true;
	private String localPath;

	public PlainHistoCryptoRepoFileFilter() {
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
}
