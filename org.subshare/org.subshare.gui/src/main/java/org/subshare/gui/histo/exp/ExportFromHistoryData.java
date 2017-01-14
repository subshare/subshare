package org.subshare.gui.histo.exp;

import static co.codewizards.cloudstore.core.util.AssertUtil.*;

import java.util.HashSet;
import java.util.Set;

import org.subshare.core.repo.LocalRepo;

import co.codewizards.cloudstore.core.Uid;
import co.codewizards.cloudstore.core.oio.File;

public class ExportFromHistoryData {
	private final LocalRepo localRepo;

	private File exportDirectory;

	private final Set<Uid> histoCryptoRepoFileIds = new HashSet<>();

	public ExportFromHistoryData(final LocalRepo localRepo) {
		this.localRepo = assertNotNull(localRepo, "localRepo");
	}

	public LocalRepo getLocalRepo() {
		return localRepo;
	}

	public Set<Uid> getHistoCryptoRepoFileIds() {
		return histoCryptoRepoFileIds;
	}

	public File getExportDirectory() {
		return exportDirectory;
	}
	public void setExportDirectory(File exportDirectory) {
		this.exportDirectory = exportDirectory;
	}
}
