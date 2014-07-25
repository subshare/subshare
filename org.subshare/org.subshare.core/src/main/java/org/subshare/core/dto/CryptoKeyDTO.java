package org.subshare.core.dto;

import javax.xml.bind.annotation.XmlRootElement;

import co.codewizards.cloudstore.core.dto.Uid;

@XmlRootElement
public class CryptoKeyDTO {

	private Uid cryptoKeyId;

//	private long repoFileId;

	private CryptoKeyType cryptoKeyType;

	private CryptoKeyRole cryptoKeyRole;

	private long localRevision;

	public Uid getCryptoKeyId() {
		return cryptoKeyId;
	}
	public void setCryptoKeyId(final Uid cryptoKeyId) {
		this.cryptoKeyId = cryptoKeyId;
	}

//	public long getRepoFileId() {
//		return repoFileId;
//	}
//	public void setRepoFileId(final long repoFileId) {
//		this.repoFileId = repoFileId;
//	}

	private String path;

	/**
	 * @deprecated TODO this should be replaced by a far more efficient way of encoding: repoFileId plus those RepoFileDTOs in the CryptoKeyChangeSetDTO that are need to reconstruct the path (or maybe only an id-to-pathsegment-map).
	 */
	@Deprecated
	public String getPath() {
		return path;
	}
	public void setPath(final String path) {
		this.path = path;
	}

	public CryptoKeyType getCryptoKeyType() {
		return cryptoKeyType;
	}
	public void setCryptoKeyType(final CryptoKeyType cryptoKeyType) {
		this.cryptoKeyType = cryptoKeyType;
	}

	public CryptoKeyRole getCryptoKeyRole() {
		return cryptoKeyRole;
	}
	public void setCryptoKeyRole(final CryptoKeyRole cryptoKeyRole) {
		this.cryptoKeyRole = cryptoKeyRole;
	}

	public long getLocalRevision() {
		return localRevision;
	}
	public void setLocalRevision(final long localRevision) {
		this.localRevision = localRevision;
	}

}
