package org.subshare.core.repo.histo;

import java.io.IOException;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;

public interface HistoExporter extends AutoCloseable {

	@Override
	public void close();

	void exportFile(Uid histoCryptoRepoFileId, File exportDirectory) throws IOException;

}
