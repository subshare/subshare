package org.subshare.core.repo.histo;

import java.io.IOException;

public interface HistoExporter extends AutoCloseable {

	@Override
	public void close();

	void exportFile(ExportFileParam exportFileParam) throws IOException;

}
