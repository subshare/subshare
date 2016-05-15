package org.subshare.local.persistence;

import static co.codewizards.cloudstore.core.util.Util.*;

import javax.jdo.annotations.Index;
import javax.jdo.annotations.Indices;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

import co.codewizards.cloudstore.local.persistence.Entity;

@PersistenceCapable
@Unique(
		name = "UK_HistoFileChunk_histoCryptoRepoFile_offset", members = {"histoCryptoRepoFile", "offset"})
@Indices({
//	@Index(name = "HistoFileChunk_normalFile", members = "normalFile"),
	@Index(name = "HistoFileChunk_histoCryptoRepoFile_offset", members = {"histoCryptoRepoFile", "offset"}),
	@Index(name = "HistoFileChunk_fileChunkPayload", members = "fileChunkPayload"),
})
@Queries({
	@Query(name = "getHistoFileChunks_fileChunkPayload", value = "SELECT WHERE this.fileChunkPayload == :fileChunkPayload"),
	@Query(name = "getHistoFileChunkCount_fileChunkPayload", value = "SELECT count(this) WHERE this.fileChunkPayload == :fileChunkPayload"),

	@Query(name = "getHistoFileChunks_histoCryptoRepoFile", value = "SELECT WHERE this.histoCryptoRepoFile == :histoCryptoRepoFile")

//	@Query(name = "getHistoFileChunks_normalFile", value = "SELECT WHERE this.normalFile == :normalFile"),
//
//	@Query(name = "getHistoFileChunks_normalFile_remoteRepositoryId",
//	value = "SELECT WHERE this.normalFile == :normalFile "
//			+ "&& this.remoteRepositoryId == :remoteRepositoryId "),
//
//	@Query(name = "getHistoFileChunk_normalFile_remoteRepositoryId_offset",
//			value = "SELECT UNIQUE WHERE this.normalFile == :normalFile "
//					+ "&& this.remoteRepositoryId == :remoteRepositoryId "
//					+ "&& this.offset == :offset")
})
public class HistoFileChunk extends Entity {

	@Persistent(nullValue = NullValue.EXCEPTION)
	private HistoCryptoRepoFile histoCryptoRepoFile;

//	private NormalFile normalFile;

	private long offset;

	private int length;

//	private String sha1;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private FileChunkPayload fileChunkPayload;

	public HistoFileChunk() {
	}

	public HistoCryptoRepoFile getHistoCryptoRepoFile() {
		return histoCryptoRepoFile;
	}
	public void setHistoCryptoRepoFile(HistoCryptoRepoFile histoCryptoRepoFile) {
		if (! equal(this.histoCryptoRepoFile, histoCryptoRepoFile))
			this.histoCryptoRepoFile = histoCryptoRepoFile;
	}

//	public NormalFile getNormalFile() {
//		return normalFile;
//	}
//	public void setNormalFile(NormalFile normalFile) {
//		this.normalFile = normalFile;
//	}

	public long getOffset() {
		return offset;
	}
	public void setOffset(long offset) {
		if (! equal(this.offset, offset))
			this.offset = offset;
	}

	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		if (! equal(this.length, length))
			this.length = length;
	}

//	/**
//	 * Gets the SHA1 of the chunk's binary data.
//	 * @return the SHA1 of the chunk's binary data. Never <code>null</code> on client-side, but always <code>null</code> on server-side.
//	 */
//	public String getSha1() {
//		return sha1;
//	}
//	public void setSha1(final String sha1) {
//		this.sha1 = sha1;
//	}

	public FileChunkPayload getFileChunkPayload() {
		return fileChunkPayload;
	}
	public void setFileChunkPayload(FileChunkPayload fileChunkPayload) {
		if (! equal(this.fileChunkPayload, fileChunkPayload))
			this.fileChunkPayload = fileChunkPayload;
	}
}
