package org.subshare.local;

import static co.codewizards.cloudstore.core.util.Util.*;

import org.subshare.core.context.RepoFileContext;
import org.subshare.local.persistence.SsLocalRepository;
import org.subshare.local.persistence.SsRepoFile;
import org.subshare.local.persistence.LocalRepositoryType;

import co.codewizards.cloudstore.core.oio.File;
import co.codewizards.cloudstore.core.progress.ProgressMonitor;
import co.codewizards.cloudstore.core.repo.local.LocalRepoTransaction;
import co.codewizards.cloudstore.local.LocalRepoSync;
import co.codewizards.cloudstore.local.persistence.LocalRepository;
import co.codewizards.cloudstore.local.persistence.LocalRepositoryDao;
import co.codewizards.cloudstore.local.persistence.RepoFile;

public class SsLocalRepoSync extends LocalRepoSync {

	protected SsLocalRepoSync(final LocalRepoTransaction transaction) {
		super(transaction);
	}

	@Override
	protected RepoFile sync(final RepoFile parentRepoFile, final File file, final ProgressMonitor monitor, boolean resursiveChildren) {
		if (resursiveChildren) {
			final LocalRepository lr = transaction.getDao(LocalRepositoryDao.class).getLocalRepositoryOrFail();
			final SsLocalRepository localRepository = (SsLocalRepository) lr;
			if (localRepository.getLocalRepositoryType() == LocalRepositoryType.SERVER)
				resursiveChildren = false;
		}
		return super.sync(parentRepoFile, file, monitor, resursiveChildren);
	}

	@Override
	protected RepoFile _createRepoFile(final RepoFile parentRepoFile, final File file, final ProgressMonitor monitor) {
		final RepoFile repoFile = super._createRepoFile(parentRepoFile, file, monitor);
		applyRepoFileContextIfExists(repoFile);
		return repoFile;
	}

	@Override
	public void updateRepoFile(final RepoFile repoFile, final File file, final ProgressMonitor monitor) {
		super.updateRepoFile(repoFile, file, monitor);
		applyRepoFileContextIfExists(repoFile);
	}

	protected void applyRepoFileContextIfExists(final RepoFile repoFile) {
		final RepoFileContext repoFileContext = RepoFileContext.getContext();
		if (repoFileContext != null) {
			final SsRepoFile ccRepoFile = (SsRepoFile) repoFile;
			final RepoFile parentRepoFile = repoFile.getParent();
			final String parentName = parentRepoFile == null ? null : parentRepoFile.getName();

			if (!equal(parentName, repoFileContext.getSsRepoFileDto().getParentName()))
				throw new IllegalStateException(String.format("parentName != ssRepoFileDto.parentName :: '%s' != '%s'",
						parentName, repoFileContext.getSsRepoFileDto().getParentName()));

			ccRepoFile.setSignature(repoFileContext.getSsRepoFileDto().getSignature());
		}
	}

}
