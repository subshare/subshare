package org.subshare.local.persistence;

import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Unique;

import co.codewizards.cloudstore.core.dto.RepoFileDTO;
import co.codewizards.cloudstore.local.persistence.Entity;
import co.codewizards.cloudstore.local.persistence.RepoFile;

/**
 * Container holding the encrypted meta-data for a file (or directory).
 * <p>
 * There is a 1-1-relation to a {@link RepoFile} (via the {@link #getRepoFile() repoFile} property).
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
@PersistenceCapable
public class CryptoRepoFile extends Entity {

	@Persistent(nullValue=NullValue.EXCEPTION)
	@Unique
	private RepoFile repoFile;

	@Persistent(nullValue=NullValue.EXCEPTION)
	private byte[] encryptedRepoFileDTO;

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
	 * Gets the encrypted real meta-data (an instance of a sub-class of {@link RepoFileDTO}).
	 * <p>
	 * This meta-data is encrypted on the client-side.
	 * @return the encrypted real meta-data (an instance of a sub-class of {@link RepoFileDTO}). Never <code>null</code> in persistent data
	 * (but maybe <code>null</code> temporarily in memory).
	 */
	public byte[] getEncryptedRepoFileDTO() {
		return encryptedRepoFileDTO;
	}
	public void setEncryptedRepoFileDTO(final byte[] encryptedRepoFileDTO) {
		this.encryptedRepoFileDTO = encryptedRepoFileDTO;
	}

}
