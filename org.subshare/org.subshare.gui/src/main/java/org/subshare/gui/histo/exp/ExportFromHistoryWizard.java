package org.subshare.gui.histo.exp;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import org.subshare.core.repo.LocalRepo;
import org.subshare.core.repo.histo.HistoExporter;
import org.subshare.gui.histo.exp.destination.ExportFromHistoryDestinationWizardPage;
import org.subshare.gui.ls.HistoExporterLs;
import org.subshare.gui.wizard.Wizard;

import co.codewizards.cloudstore.core.dto.Uid;
import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;

public class ExportFromHistoryWizard extends Wizard {

	private final ExportFromHistoryData exportFromHistoryData;

	public ExportFromHistoryWizard(final ExportFromHistoryData exportFromHistoryData) {
		this.exportFromHistoryData = assertNotNull("exportFromHistoryData", exportFromHistoryData);
		setFirstPage(new ExportFromHistoryDestinationWizardPage(exportFromHistoryData));
	}

	@Override
	public String getTitle() {
		return Messages.getString("ExportFromHistoryWizard.title"); //$NON-NLS-1$
	}

	@Override
	protected void finish(final ProgressMonitor monitor) throws Exception {
		final File exportDirectory = assertNotNull("exportFromHistoryData.exportDirectory", exportFromHistoryData.getExportDirectory());

		final LocalRepo localRepo = exportFromHistoryData.getLocalRepo();
		try (final HistoExporter histoExporter = HistoExporterLs.createHistoExporter(localRepo.getLocalRoot())) {
			for (final Uid histoCryptoRepoFileId : exportFromHistoryData.getHistoCryptoRepoFileIds()) {
				histoExporter.exportFile(histoCryptoRepoFileId, exportDirectory);
			}
		}
	}
}
