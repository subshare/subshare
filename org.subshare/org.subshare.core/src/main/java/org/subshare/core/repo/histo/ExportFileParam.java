package org.subshare.core.repo.histo;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;
import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;

public class ExportFileParam {

	private Uid histoCryptoRepoFileId;
	private File exportDirectory;
	private boolean recursive = true;

	public ExportFileParam() {
	}

	public ExportFileParam(Uid histoCryptoRepoFileId, File exportDirectory) {
		this.histoCryptoRepoFileId = assertNotNull("histoCryptoRepoFileId", histoCryptoRepoFileId);
		this.exportDirectory = assertNotNull("exportDirectory", exportDirectory);
	}

	/**
	 * Gets the identifier of the {@code HistoCryptoRepoFile} to be exported.
	 * @return the identifier of the {@code HistoCryptoRepoFile} to be exported. Must not be <code>null</code>
	 * when invoking {@link HistoExporter#exportFile(ExportFileParam)}.
	 */
	public Uid getHistoCryptoRepoFileId() {
		return histoCryptoRepoFileId;
	}
	public void setHistoCryptoRepoFileId(Uid histoCryptoRepoFileId) {
		this.histoCryptoRepoFileId = histoCryptoRepoFileId;
	}

	/**
	 * Gets the destination directory into which the export is written.
	 * @return the destination directory into which the export is written. Must not be <code>null</code>
	 * when invoking {@link HistoExporter#exportFile(ExportFileParam)}.
	 */
	public File getExportDirectory() {
		return exportDirectory;
	}
	public void setExportDirectory(File exportDirectory) {
		this.exportDirectory = exportDirectory;
	}

	/**
	 * Indicates whether to export a directory recursively including its children.
	 * <p>
	 * If the file being exported is not a directory, this property is ignored.
	 * <p>
	 * The default value is <code>true</code>.
	 * @return <code>true</code> to export a directory with all children recursively. <code>false</code>
	 * to export only the single directory without children.
	 */
	public boolean isRecursive() {
		return recursive;
	}
	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	public ExportFileParam recursive(boolean recursive) {
		this.setRecursive(recursive);
		return this;
	}
}
