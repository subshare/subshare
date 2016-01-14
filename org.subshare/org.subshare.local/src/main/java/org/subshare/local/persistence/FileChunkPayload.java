package org.subshare.local.persistence;

import javax.jdo.JDOHelper;
import javax.jdo.annotations.Column;
import javax.jdo.annotations.Index;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.listener.StoreCallback;

import co.codewizards.cloudstore.local.persistence.Entity;
import co.codewizards.cloudstore.local.persistence.FileChunk;

@PersistenceCapable
@Index(name = "FileChunkPayload_fileChunk_tempFileChunk", members = { "fileChunk", "tempFileChunk" })
//@Unique(name = "UK_FileChunkPayload_fileChunk_tempFileChunk", members = { "fileChunk", "tempFileChunk" }) // cannot use, anymore, because multiple can be null!
@Queries({
	@Query(name = "getFileChunkPayload_fileChunk",
			value = "SELECT UNIQUE WHERE this.fileChunk == :fileChunk && this.tempFileChunk == null"),

	@Query(name = "getFileChunkPayload_tempFileChunk",
			value = "SELECT UNIQUE WHERE this.fileChunk == null && this.tempFileChunk == :tempFileChunk"),

	@Query(name = "getFileChunkPayloadOfFileChunk_normalFile_offset",
			value = "SELECT UNIQUE WHERE this.fileChunk.normalFile == :normalFile && this.tempFileChunk == null && this.fileChunk.offset == :offset"),

	@Query(name = "getFileChunkPayloadOfHistoFileChunk_histoCryptoRepoFile_offset",
			value = "SELECT UNIQUE WHERE this == histoFileChunk.fileChunkPayload"
					+ " && histoFileChunk.histoCryptoRepoFile == :histoCryptoRepoFile"
					+ " && this.tempFileChunk == null && histoFileChunk.offset == :offset"
					+ " VARIABLES org.subshare.local.persistence.HistoFileChunk histoFileChunk")
})
public class FileChunkPayload extends Entity implements StoreCallback {

	private FileChunk fileChunk;

	private TempFileChunk tempFileChunk;

	@Persistent(nullValue = NullValue.EXCEPTION)
	@Column(jdbcType="BLOB")
	private byte[] fileData;

	public FileChunkPayload() { }

	public FileChunk getFileChunk() {
		return fileChunk;
	}
	public void setFileChunk(FileChunk fileChunk) {
		this.fileChunk = fileChunk;
	}

	public TempFileChunk getTempFileChunk() {
		return tempFileChunk;
	}
	public void setTempFileChunk(TempFileChunk tempFileChunk) {
		this.tempFileChunk = tempFileChunk;
	}

	public byte[] getFileData() {
		return fileData;
	}
	public void setFileData(byte[] content) {
		this.fileData = content;
	}

	@Override
	public void jdoPreStore() {
		// Some consistency checks. These are optional - they are really just checks.
		if (fileChunk != null && tempFileChunk != null)
			throw new IllegalStateException("fileChunk != null && tempFileChunk != null :: Only one of them can be non-null!");

		if (fileChunk == null && tempFileChunk == null) {
			final long histoFileChunkCount = new HistoFileChunkDao().persistenceManager(JDOHelper.getPersistenceManager(this)).getHistoFileChunkCount(this);
			if (histoFileChunkCount == 0)
				throw new IllegalStateException("fileChunk == null && tempFileChunk == null && histoFileChunkCount == 0");
		}
	}
}
