package org.subshare.local.dbrepo.persistence;

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
import co.codewizards.cloudstore.local.persistence.RepoFile;

@PersistenceCapable
@Indices({
	@Index(name = "TempFileChunk_repoFile", members = "repoFile"),

	@Index(name = "TempFileChunk_repoFile_remoteRepositoryId_offset",
			members = {"repoFile", "remoteRepositoryId", "offset"}),
})
@Unique(
		name = "UK_TempFileChunk_repoFile_remoteRepositoryId_offset",
		members = {"repoFile", "remoteRepositoryId", "offset"})
@Queries({
	@Query(name = "getTempFileChunks_repoFile", value = "SELECT WHERE this.repoFile == :repoFile"),

	@Query(name = "getTempFileChunks_repoFile_remoteRepositoryId",
	value = "SELECT WHERE this.repoFile == :repoFile "
			+ "&& this.remoteRepositoryId == :remoteRepositoryId "),

	@Query(name = "getTempFileChunk_repoFile_remoteRepositoryId_offset",
			value = "SELECT UNIQUE WHERE this.repoFile == :repoFile "
					+ "&& this.remoteRepositoryId == :remoteRepositoryId "
					+ "&& this.offset == :offset")
})
public class TempFileChunk extends Entity {

	@Persistent(nullValue = NullValue.EXCEPTION)
	private RepoFile repoFile;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private String remoteRepositoryId;

	private long offset;

//	@Persistent(mappedBy = "fileChunk", dependent = "true")
//	private FileChunkPayload fileChunkPayload;

	public TempFileChunk() {
	}

	public RepoFile getRepoFile() {
		return repoFile;
	}
	public void setRepoFile(RepoFile repoFile) {
		this.repoFile = repoFile;
	}

	public UUID getRemoteRepositoryId() {
		return remoteRepositoryId == null ? null : UUID.fromString(remoteRepositoryId);
	}
	public void setRemoteRepositoryId(UUID remoteRepositoryId) {
		this.remoteRepositoryId = remoteRepositoryId == null ? null : remoteRepositoryId.toString();
	}

	public long getOffset() {
		return offset;
	}
	public void setOffset(long offset) {
		this.offset = offset;
	}

//	public FileChunkPayload getFileChunkPayload() {
//		return fileChunkPayload;
//	}
//	public void setFileChunkPayload(FileChunkPayload fileChunkPayload) {
//		this.fileChunkPayload = fileChunkPayload;
//	}
}
