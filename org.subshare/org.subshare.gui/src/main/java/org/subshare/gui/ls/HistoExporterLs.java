package org.subshare.gui.ls;

import org.subshare.core.repo.histo.HistoExporter;
import org.subshare.core.repo.histo.HistoExporterImpl;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.ls.client.LocalServerClient;

public class HistoExporterLs {

	private HistoExporterLs() {
	}

	public static HistoExporter createHistoExporter(final File localRoot) {
		final HistoExporter result = LocalServerClient.getInstance().invokeStatic(
				HistoExporterImpl.class, "createHistoExporter", localRoot);

		return result;
	}
}
