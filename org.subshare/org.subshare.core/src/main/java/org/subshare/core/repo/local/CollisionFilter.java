package org.subshare.core.repo.local;

import java.io.Serializable;

import co.codewizards.cloudstore.core.dto.Uid;

@SuppressWarnings("serial")
public class CollisionFilter implements Serializable {

	private Uid cryptoRepoFileId;

	private Uid histoCryptoRepoFileId;

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

}
