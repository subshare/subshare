package org.subshare.local.persistence;

import java.util.UUID;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.Indices;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

import co.codewizards.cloudstore.local.persistence.Entity;
import co.codewizards.cloudstore.local.persistence.NormalFile;

@PersistenceCapable
@Indices({
	@Index(name = "TempFileChunk_normalFile", members = "normalFile"),

	@Index(name = "TempFileChunk_normalFile_remoteRepositoryId_offset",
			members = {"normalFile", "remoteRepositoryId", "offset"}),
})
@Unique(
		name = "UK_TempFileChunk_normalFile_remoteRepositoryId_offset",
		members = {"normalFile", "remoteRepositoryId", "offset"})
@Queries({
	@Query(name = "getTempFileChunks_normalFile", value = "SELECT WHERE this.normalFile == :normalFile"),

	@Query(name = "getTempFileChunks_normalFile_remoteRepositoryId",
	value = "SELECT WHERE this.normalFile == :normalFile "
			+ "&& this.remoteRepositoryId == :remoteRepositoryId "),

	@Query(name = "getTempFileChunk_normalFile_remoteRepositoryId_offset",
			value = "SELECT UNIQUE WHERE this.normalFile == :normalFile "
					+ "&& this.remoteRepositoryId == :remoteRepositoryId "
					+ "&& this.offset == :offset")
})
public class TempFileChunk extends Entity {

	public static enum Role {
		/**
		 * This chunk is stored locally. The local repository receives the data.
		 */
		RECEIVING,
		/**
		 * This chunk is stored remotely (only managed here). The local repository sends the data.
		 */
		SENDING
	}

	@Persistent(nullValue = NullValue.EXCEPTION)
	private NormalFile normalFile;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private String remoteRepositoryId;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private Role role; // not needed in unique constraint, because we should *never* download and upload simultaneously.

	private long offset;

	private int length;

	private String sha1;

	public TempFileChunk() {
	}

	public NormalFile getNormalFile() {
		return normalFile;
	}
	public void setNormalFile(NormalFile normalFile) {
		this.normalFile = normalFile;
	}

	public UUID getRemoteRepositoryId() {
		return remoteRepositoryId == null ? null : UUID.fromString(remoteRepositoryId);
	}
	public void setRemoteRepositoryId(UUID remoteRepositoryId) {
		this.remoteRepositoryId = remoteRepositoryId == null ? null : remoteRepositoryId.toString();
	}

	public Role getRole() {
		return role;
	}
	public void setRole(Role role) {
		this.role = role;
	}

	public long getOffset() {
		return offset;
	}
	public void setOffset(long offset) {
		this.offset = offset;
	}

	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}

	/**
	 * Gets the SHA1 of the chunk's binary data.
	 * @return the SHA1 of the chunk's binary data. Never <code>null</code> on client-side, but always <code>null</code> on server-side.
	 */
	public String getSha1() {
		return sha1;
	}
	public void setSha1(final String sha1) {
		this.sha1 = sha1;
	}
}
