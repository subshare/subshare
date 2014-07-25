package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Unique;

import org.subshare.core.dto.CryptoKeyRole;

import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.local.persistence.AutoTrackLocalRevision;
import co.codewizards.cloudstore.local.persistence.Entity;
import co.codewizards.cloudstore.local.persistence.RepoFile;

/**
 * Container holding the encrypted meta-data for a file (or directory).
 * <p>
 * There is a 1-1-relation to a {@link RepoFile} (via the {@link #getRepoFile() repoFile} property).
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@PersistenceCapable
@Unique(name="CryptoRepoFile_repoFile", members="repoFile")
public class CryptoRepoFile extends Entity implements AutoTrackLocalRevision {

	@Persistent(nullValue=NullValue.EXCEPTION)
	private RepoFile repoFile;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private CryptoKey cryptoKey;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] repoFileDTOData;

	/**
	 * Gets the {@link RepoFile} on the SubShare-server-side.
	 * <p>
	 * The file referenced here is encrypted and has no useful meta-data, anymore:
	 * <ul>
	 * <li>Its {@link RepoFile#getName() name} is a hash code (unique per server-repository and real name).
	 * <li>Its {@link RepoFile#getLastModified() lastModified} is always 0 (1970-01-01 00:00:00 UTC).
	 * </ul>
	 * <p>
	 * The real meta-data is encoded as DTO and then encrypted on the client-side.
	 * @return the {@link RepoFile} on the SubShare-server-side. Never <code>null</code> in persistent data
	 * (but maybe <code>null</code> temporarily in memory).
	 */
	public RepoFile getRepoFile() {
		return repoFile;
	}
	public void setRepoFile(final RepoFile repoFile) {
		this.repoFile = repoFile;
	}

	/**
	 * Gets the key used to encrypt {@link #getRepoFileDTOData() repoFileDTOData} as well as the
	 * actual content of the file (if it is a normal file - no directory).
	 * @return the key used to encrypt {@link #getRepoFileDTOData() repoFileDTOData} and - if applicable -
	 * the file contents.
	 */
	public CryptoKey getCryptoKey() {
		return cryptoKey;
	}
	public void setCryptoKey(final CryptoKey cryptoKey) {
		if (cryptoKey != null) {
			final CryptoKeyRole cryptoKeyRole = assertNotNull("cryptoKey.cryptoKeyRole", cryptoKey.getCryptoKeyRole());
			if (CryptoKeyRole.dataKey != cryptoKeyRole)
				throw new IllegalArgumentException("cryptoKey.cryptoKeyRole != dataKey");
		}
		this.cryptoKey = cryptoKey;
	}

	/**
	 * Gets the encrypted real meta-data (an instance of a sub-class of {@link RepoFileDTO}).
	 * <p>
	 * This meta-data is encrypted on the client-side using the referenced {@link #getCryptoKey() cryptoKey}.
	 * @return the encrypted real meta-data (an instance of a sub-class of {@link RepoFileDTO}). Never <code>null</code> in persistent data
	 * (but maybe <code>null</code> temporarily in memory).
	 */
	public byte[] getRepoFileDTOData() {
		return repoFileDTOData;
	}
	public void setRepoFileDTOData(final byte[] repoFileDTOData) {
		this.repoFileDTOData = repoFileDTOData;
	}

	@Override
	public long getLocalRevision() {
		return assertNotNull("repoFile", repoFile).getLocalRevision();
	}
	@Override
	public void setLocalRevision(final long localRevision) {
		assertNotNull("repoFile", repoFile).setLocalRevision(localRevision);
	}

}
